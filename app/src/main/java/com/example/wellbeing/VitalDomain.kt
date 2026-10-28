package com.example.wellbeing

import java.time.*

/** Calendar recurrences use local dates; durations use instants across DST. */
object PlanRules {
    fun occurs(start: LocalDate, day: LocalDate, recurrence: String): Boolean = day >= start && when (recurrence) {
        "daily" -> true
        "weekdays" -> day.dayOfWeek.value <= 5
        "weekly" -> day.dayOfWeek == start.dayOfWeek
        else -> day == start
    }
    fun overlaps(start: Instant, minutes: Long, other: Instant, otherMinutes: Long): Boolean {
        require(minutes > 0 && otherMinutes > 0)
        return start < other.plusSeconds(otherMinutes * 60) && other < start.plusSeconds(minutes * 60)
    }
    fun sleepMinutes(bed: ZonedDateTime, wake: ZonedDateTime): Long {
        val minutes = Duration.between(bed, wake).toMinutes()
        require(minutes in 1..1440)
        return minutes
    }
}

object HealthIntervals {
    /** Union of sessions avoids double-counting overlapping sleep sources. */
    fun minutes(intervals: List<Pair<Instant, Instant>>): Long {
        val ordered = intervals.filter { it.second > it.first }.sortedBy { it.first }
        if (ordered.isEmpty()) return 0
        var start = ordered.first().first
        var end = ordered.first().second
        var seconds = 0L
        for ((a, b) in ordered.drop(1)) {
            if (a <= end) { if (b > end) end = b }
            else { seconds += Duration.between(start, end).seconds; start = a; end = b }
        }
        return (seconds + Duration.between(start, end).seconds) / 60
    }
}
