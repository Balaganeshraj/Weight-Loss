package com.example.wellbeing

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class VitalDomainTest {
    @Test fun recurrenceNeverAppearsBeforeStartAndWeekdaysSkipWeekend() {
        val start = LocalDate.of(2026, 10, 30)
        assertFalse(PlanRules.occurs(start, start.minusDays(1), "daily"))
        assertFalse(PlanRules.occurs(start, start.plusDays(1), "weekdays"))
        assertTrue(PlanRules.occurs(start, start.plusDays(3), "weekdays"))
        assertTrue(PlanRules.occurs(start, start.plusDays(7), "weekly"))
        assertFalse(PlanRules.occurs(start, start.plusDays(1), "once"))
    }
    @Test fun adjacentTasksDoNotOverlapButContainedTasksDo() {
        val start = Instant.parse("2026-10-28T03:30:00Z")
        assertFalse(PlanRules.overlaps(start, 30, start.plusSeconds(1800), 30))
        assertTrue(PlanRules.overlaps(start, 30, start.plusSeconds(60), 5))
    }
    @Test fun crossMidnightSleepUsesActualElapsedTime() {
        val zone = ZoneId.of("Asia/Colombo")
        assertEquals(480, PlanRules.sleepMinutes(ZonedDateTime.of(2026,10,27,22,0,0,0,zone), ZonedDateTime.of(2026,10,28,6,0,0,0,zone)))
    }
    @Test fun daylightSavingSleepUsesInstantsRatherThanWallClockHours() {
        val zone = ZoneId.of("America/New_York")
        assertEquals(540, PlanRules.sleepMinutes(ZonedDateTime.of(2026,10,31,22,0,0,0,zone), ZonedDateTime.of(2026,11,1,6,0,0,0,zone)))
    }
    @Test(expected=IllegalArgumentException::class) fun reverseSleepRejected() {
        val now = ZonedDateTime.now(); PlanRules.sleepMinutes(now, now.minusHours(1))
    }
    @Test(expected=IllegalArgumentException::class) fun MultiDaySleepRejected() {
        val now = ZonedDateTime.now(); PlanRules.sleepMinutes(now, now.plusHours(25))
    }
    @Test fun overlappingDeviceSleepSourcesAreUnionedNotAdded() {
        val start = Instant.parse("2026-10-27T18:00:00Z")
        assertEquals(120, HealthIntervals.minutes(listOf(start to start.plusSeconds(3600), start.plusSeconds(1800) to start.plusSeconds(7200))))
        assertEquals(120, HealthIntervals.minutes(listOf(start to start.plusSeconds(7200), start to start.plusSeconds(7200))))
    }
    @Test fun disconnectedSleepSessionsRemainSeparate() {
        val start = Instant.parse("2026-10-27T18:00:00Z")
        assertEquals(90, HealthIntervals.minutes(listOf(start to start.plusSeconds(3600), start.plusSeconds(7200) to start.plusSeconds(9000))))
        assertEquals(0, HealthIntervals.minutes(emptyList()))
    }
    @Test fun newRecordTypesValidateBeforeEncryptedPersistence() {
        assertFalse(WellnessMath.valid(Entry(kind=Kind.TASK,title="",amount=30.0)))
        assertFalse(WellnessMath.valid(Entry(kind=Kind.SLEEP,amount=1500.0)))
        assertTrue(WellnessMath.valid(Entry(kind=Kind.TASK,title="Plan",amount=30.0)))
    }
}
