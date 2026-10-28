package com.example.wellbeing

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.time.*

class HealthData(private val context: Context, private val repository: JournalRepository) {
    companion object {
        val STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        val SLEEP = HealthPermission.getReadPermission(SleepSessionRecord::class)
    }
    val syncing = MutableStateFlow(false)
    val result = MutableStateFlow("")
    private val mutex = Mutex()
    fun availability(): Int = HealthConnectClient.getSdkStatus(context)
    suspend fun sync() {
        if (!mutex.tryLock()) return
        syncing.value = true
        result.value = ""
        try {
            withTimeout(30_000) {
                if (availability() != HealthConnectClient.SDK_AVAILABLE) { result.value = "setup"; return@withTimeout }
                val client = HealthConnectClient.getOrCreate(context)
                val day = LocalDate.now()
                val zone = ZoneId.systemDefault()
                val start = day.atStartOfDay(zone).toInstant()
                val now = Instant.now()
                val id = "health-$day"
                val previous = repository.entries.first().find { it.id == id }
                val data = previous?.metadata() ?: JSONObject()
                val outcomes = mutableListOf<String>()
                suspend fun permitted(scope: String) = scope in client.permissionController.getGrantedPermissions()
                if (permitted(STEPS)) {
                    try {
                        val count = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, now)))[StepsRecord.COUNT_TOTAL]
                        check(permitted(STEPS))
                        if (count != null) { data.put("steps", count).put("stepsAt", now.toEpochMilli()); outcomes += "steps:updated" } else outcomes += "steps:no_new"
                    } catch (e: CancellationException) { throw e } catch (_: Exception) { outcomes += "steps:failed" }
                } else outcomes += "steps:denied"
                if (permitted(SLEEP)) {
                    try {
                        val intervals = mutableListOf<Pair<Instant, Instant>>()
                        var token: String? = null
                        var pages = 0
                        do {
                            check(permitted(SLEEP))
                            val response = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(day.minusDays(1).atStartOfDay(zone).toInstant(), now), pageSize = 1000, pageToken = token))
                            intervals += response.records.filter { it.endTime.atZone(zone).toLocalDate() == day }.map { it.startTime to it.endTime }
                            token = response.pageToken
                            pages++
                            check(pages <= 100)
                        } while (token != null)
                        check(permitted(SLEEP))
                        if (intervals.isNotEmpty()) { data.put("sleepMinutes", HealthIntervals.minutes(intervals)).put("sleepAt", now.toEpochMilli()); outcomes += "sleep:updated" } else outcomes += "sleep:no_new"
                    } catch (e: CancellationException) { throw e } catch (_: Exception) { outcomes += "sleep:failed" }
                } else outcomes += "sleep:denied"
                // Final scope check protects a revocation racing with either read.
                if (!permitted(STEPS)) { data.remove("steps"); data.remove("stepsAt"); outcomes.removeAll { it.startsWith("steps:") }; outcomes += "steps:denied" }
                if (!permitted(SLEEP)) { data.remove("sleepMinutes"); data.remove("sleepAt"); outcomes.removeAll { it.startsWith("sleep:") }; outcomes += "sleep:denied" }
                data.put("status", outcomes.joinToString("|"))
                if (outcomes.any { it.endsWith(":updated") } || previous != null) repository.save(Entry(id = id, kind = Kind.DEVICE_SNAPSHOT, at = start.toEpochMilli(), title = "Health Connect", source = "Health Connect", extra = data.toString()))
                result.value = outcomes.joinToString("|")
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) { result.value = "timeout" }
        catch (e: CancellationException) { result.value = "cancelled"; throw e }
        catch (_: Exception) { result.value = "failed" }
        finally { syncing.value = false; mutex.unlock() }
    }
}
