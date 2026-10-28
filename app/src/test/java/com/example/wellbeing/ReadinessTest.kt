package com.example.wellbeing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReadinessTest {
    @Test fun dailyReminderUsesLocalTimezone() {
        val now = ZonedDateTime.of(2026,10,27,9,30,0,0,ZoneId.of("Asia/Colombo"))
        assertEquals(30, Duration.between(now,ReminderSchedule.next(now,10,0)).toMinutes())
        assertEquals(now.toLocalDate().plusDays(1),ReminderSchedule.next(now.plusHours(1),10,0).toLocalDate())
    }
    @Test fun quietHoursBoundaries() {
        val day = ZonedDateTime.of(2026,10,27,7,59,0,0,ZoneId.of("Asia/Colombo"))
        assertTrue(ReminderSchedule.withinQuietHours(day))
        assertFalse(ReminderSchedule.withinQuietHours(day.plusMinutes(1)))
        assertFalse(ReminderSchedule.withinQuietHours(day.withHour(20)))
        assertTrue(ReminderSchedule.withinQuietHours(day.withHour(21)))
    }
    @Test fun dailyReminderSurvivesDstOffsetChange() {
        val before=ZonedDateTime.of(2026,10,31,12,0,0,0,ZoneId.of("America/New_York"))
        val next=ReminderSchedule.next(before,10,0)
        assertEquals(10,next.hour)
        assertEquals(23,Duration.between(before,next).toHours())
    }
    @Test(expected=IllegalArgumentException::class) fun rejectReminderInQuietHours() { ReminderSchedule.next(ZonedDateTime.now(),23,0) }
    private class Queue:SyncQueue {
        val changes=mutableListOf(PendingChange("one","record","UPSERT",1,"encrypted-domain-payload"),PendingChange("two","other","DELETE",2,"tombstone"))
        override suspend fun pending(limit:Int)=changes.take(limit)
        override suspend fun acknowledge(mutationIds:Set<String>){changes.removeAll{it.mutationId in mutationIds}}
    }
    @Test fun syncRequiresConsentAndLeavesQueueUntouched()=runTest {
        val queue=Queue();var calls=0
        val gateway=object:CloudJournalGateway{override suspend fun push(changes:List<PendingChange>):PushResult{calls++;return PushResult(emptySet())}}
        assertTrue(runCatching{SyncCoordinator(queue,gateway).pushPending(false)}.isFailure)
        assertEquals(0,calls);assertEquals(2,queue.changes.size)
    }
    @Test fun conflictRemainsQueuedAndOnlyExplicitAckIsRemoved()=runTest {
        val queue=Queue()
        val gateway=object:CloudJournalGateway{override suspend fun push(changes:List<PendingChange>)=PushResult(setOf("one"),setOf("two"))}
        SyncCoordinator(queue,gateway).pushPending(true)
        assertEquals(listOf("two"),queue.changes.map{it.mutationId})
    }
    @Test fun unknownAcknowledgementRejectedWithoutDeletingData()=runTest {
        val queue=Queue()
        val gateway=object:CloudJournalGateway{override suspend fun push(changes:List<PendingChange>)=PushResult(setOf("foreign"))}
        assertTrue(runCatching{SyncCoordinator(queue,gateway).pushPending(true)}.isFailure)
        assertEquals(2,queue.changes.size)
    }
    @Test fun offlineFailurePreservesStableMutationIdsForRetry()=runTest {
        val queue=Queue();val original=queue.changes.toList()
        val gateway=object:CloudJournalGateway{override suspend fun push(changes:List<PendingChange>):PushResult=throw java.io.IOException("offline")}
        assertTrue(runCatching{SyncCoordinator(queue,gateway).pushPending(true)}.isFailure)
        assertEquals(original,queue.changes)
    }
}
