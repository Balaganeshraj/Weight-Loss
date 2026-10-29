package com.example.wellbeing

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.util.concurrent.TimeUnit

/** Sensitive titles and instructions never enter WorkManager data or notifications. */
object EntryReminders {
    internal val mutex = Mutex()
    const val TAG = "vitalday.entry.reminders"
    private fun name(id: String) = "$TAG.$id"
    fun next(entry: Entry, now: ZonedDateTime): ZonedDateTime? {
        val meta = entry.metadata()
        if (!meta.optBoolean("notify")) return null
        if (entry.kind == Kind.MEDICATION) {
            val time = LocalTime.parse(meta.getString("time"))
            val today = now.toLocalDate().atTime(time).atZone(now.zone)
            return if (today > now) today else today.plusDays(1)
        }
        if (entry.kind !in setOf(Kind.TASK, Kind.MEAL_PLAN)) return null
        val original = Instant.ofEpochMilli(entry.at).atZone(now.zone)
        for (offset in -1L..370L) {
            val day = now.toLocalDate().plusDays(offset)
            if (!entry.taskOccurs(day) || entry.taskDone(day)) continue
            var target = day.atTime(original.toLocalTime()).atZone(now.zone)
            // Quiet hours apply to planner prompts, never silently change a medicine time.
            if (target.hour < 8) target = target.withHour(8).withMinute(0)
            if (target.hour >= 21) target = day.plusDays(1).atTime(8, 0).atZone(now.zone)
            if (target > now) return target
        }
        return null
    }
    fun schedule(context: Context, entry: Entry, append: Boolean = false) {
        if (entry.kind !in setOf(Kind.TASK, Kind.MEAL_PLAN, Kind.MEDICATION)) return
        val target = next(entry, ZonedDateTime.now())
        if (target == null) { cancel(context, entry.id); return }
        val request = OneTimeWorkRequestBuilder<EntryReminderWorker>().setInputData(workDataOf("id" to entry.id, "target" to target.toInstant().toEpochMilli()))
            .setInitialDelay((target.toInstant().toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).addTag(TAG).build()
        WorkManager.getInstance(context).enqueueUniqueWork(name(entry.id), if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request)
    }
    fun cancel(context: Context, id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(name(id))
        NotificationManagerCompat.from(context).cancel(id, 1)
    }
    fun clear(context: Context) { WorkManager.getInstance(context).cancelAllWorkByTag(TAG); NotificationManagerCompat.from(context).cancelAll() }
    suspend fun reschedule(context: Context) {
        val repo = LocalRepository(context)
        try { repo.entries.first().forEach { schedule(context, it) } } finally { repo.close() }
    }
}
class EntryReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = EntryReminders.mutex.withLock {
        val id = inputData.getString("id") ?: return Result.failure()
        val repo = LocalRepository(applicationContext)
        try {
            val entry = repo.entries.first().find { it.id == id } ?: return Result.success()
            if (!entry.metadata().optBoolean("notify")) return Result.success()
            val medication = entry.kind == Kind.MEDICATION
            val target = Instant.ofEpochMilli(inputData.getLong("target", 0)).atZone(ZoneId.systemDefault())
            val now = ZonedDateTime.now()
            val doseRecorded = medication && repo.entries.first().any { it.id == "dose-$id-${now.toLocalDate()}" }
            val occurrence = if (!medication && entry.taskTime().hour >= 21) target.toLocalDate().minusDays(1) else target.toLocalDate()
            val eligible = now.toLocalDate() == target.toLocalDate() && !now.isBefore(target) && !doseRecorded && (medication || (!entry.taskDone(occurrence) && !ReminderSchedule.withinQuietHours(now)))
            val controller = ReminderController(applicationContext)
            if (eligible && controller.canNotify()) {
                val localized = Language.context(applicationContext)
                val channel = if (medication) "vitalday.medication" else "vitalday.tasks"
                val manager = applicationContext.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(channel, localized.getString(if (medication) R.string.v_medication else R.string.v_task), NotificationManager.IMPORTANCE_DEFAULT).apply { lockscreenVisibility = Notification.VISIBILITY_PRIVATE })
                val intent = Intent(applicationContext, MainActivity::class.java).putExtra("destination", if (medication) "medication" else "plan")
                val open = PendingIntent.getActivity(applicationContext, if (medication) 4101 else 4102, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notification = NotificationCompat.Builder(applicationContext, channel).setSmallIcon(R.drawable.ic_notification).setContentTitle("VELNUMI")
                    .setContentText(localized.getString(R.string.v_reminder_private)).setContentIntent(open).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).build()
                try { NotificationManagerCompat.from(applicationContext).notify(id, 1, notification) } catch (_: SecurityException) { /* A revocation may race with the check. */ }
            }
            EntryReminders.schedule(applicationContext, entry, append = true)
            return Result.success()
        } catch (_: java.io.IOException) { return Result.retry() } finally { repo.close() }
    }
}
