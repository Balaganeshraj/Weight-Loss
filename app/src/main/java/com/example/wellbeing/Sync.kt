package com.example.wellbeing

import kotlinx.coroutines.flow.Flow

/** Local domain boundary. UI never depends on Room or a network client. */
interface JournalRepository {
    val entries: Flow<List<Entry>>
    suspend fun save(entry: Entry)
    suspend fun delete(id: String)
    suspend fun finishSession(entry: Entry)
    suspend fun clear()
}

data class PendingChange(val mutationId: String, val recordId: String, val operation: String, val localRevision: Long, val payload: String)
interface SyncQueue {
    suspend fun pending(limit: Int = 100): List<PendingChange>
    suspend fun acknowledge(mutationIds: Set<String>)
}
/** Provider must authenticate outside these payloads and enforce owner scope on the server. */
interface CloudJournalGateway {
    suspend fun push(changes: List<PendingChange>): PushResult
}
data class PushResult(val acknowledgedIds: Set<String>, val conflicts: Set<String> = emptySet())

/** No provider is connected in this release. No Internet permission or automatic uploads. */
class SyncCoordinator(private val queue: SyncQueue, private val gateway: CloudJournalGateway) {
    suspend fun pushPending(explicitConsent: Boolean): PushResult {
        check(explicitConsent) { "Cloud synchronization requires explicit consent and a configured authenticated provider" }
        val batch = queue.pending(100)
        if (batch.isEmpty()) return PushResult(emptySet())
        val result = gateway.push(batch)
        val known = batch.map { it.mutationId }.toSet()
        require(known.containsAll(result.acknowledgedIds + result.conflicts))
        require(result.acknowledgedIds.intersect(result.conflicts).isEmpty())
        queue.acknowledge(result.acknowledgedIds)
        // Conflicts and failures remain queued; never silently overwrite local corrections.
        return result
    }
}
