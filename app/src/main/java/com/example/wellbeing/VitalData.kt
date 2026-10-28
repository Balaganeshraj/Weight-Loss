package com.example.wellbeing

import org.json.JSONObject
import java.time.*

fun Entry.metadata(): JSONObject = if (extra.isBlank()) JSONObject() else JSONObject(extra)
fun Entry.taskOccurs(day: LocalDate): Boolean = kind in setOf(Kind.TASK, Kind.MEAL_PLAN) && PlanRules.occurs(
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate(), day, metadata().optString("recurrence", "once"))
fun Entry.taskDone(day: LocalDate): Boolean = day.toString() in metadata().optString("completed").split('|')
fun Entry.taskTime(): LocalTime = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalTime()
