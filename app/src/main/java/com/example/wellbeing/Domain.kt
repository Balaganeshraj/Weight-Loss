package com.example.wellbeing

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.pow

enum class Kind { FOOD, WATER, ACTIVITY, WEIGHT, MOOD, PROFILE, TIMER, TASK, SLEEP, MEDICATION, MEDICATION_EVENT, FOCUS_SESSION, MEAL_PLAN, SHOPPING, REPORT, DEVICE_SNAPSHOT }
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val kind: Kind,
    val at: Long = System.currentTimeMillis(),
    val title: String = "",
    val amount: Double = 0.0,
    val energy: Double = 0.0,
    val source: String = "User entered",
    val sourceVersion: String = "1",
    val note: String = "",
    val extra: String = ""
)
data class Profile(val age: Int = 0, val heightCm: Double? = null, val fluidRestriction: Boolean = false, val bmiExcluded: Boolean = false, val allergens: Set<String> = emptySet())
object WellnessMath {
    fun bmi(weightKg: Double, heightCm: Double): Double {
        require(weightKg.isFinite() && weightKg in 20.0..500.0)
        require(heightCm.isFinite() && heightCm in 100.0..250.0)
        return weightKg / (heightCm / 100.0).pow(2)
    }
    fun kcal(kj: Double): Double { require(kj.isFinite() && kj >= 0); return kj / 4.184 }
    fun portion(kcalPer100g: Double, grams: Double): Double {
        require(kcalPer100g.isFinite() && kcalPer100g in 0.0..1000.0)
        require(grams.isFinite() && grams in 1.0..3000.0)
        return kcalPer100g * grams / 100.0
    }
    fun onDate(entry: Entry, date: LocalDate, zone: ZoneId): Boolean = Instant.ofEpochMilli(entry.at).atZone(zone).toLocalDate() == date
    fun valid(entry: Entry): Boolean = entry.amount.isFinite() && entry.energy.isFinite() && when (entry.kind) {
        Kind.FOOD -> entry.title.isNotBlank() && entry.amount in 1.0..3000.0 && entry.energy in 0.0..30000.0
        Kind.WATER -> entry.amount in 1.0..3000.0
        Kind.ACTIVITY -> entry.amount in 0.016..1440.0
        Kind.WEIGHT -> entry.amount in 20.0..500.0
        Kind.MOOD -> entry.amount in 1.0..5.0
        Kind.TASK, Kind.MEAL_PLAN -> entry.title.isNotBlank() && entry.title.length <= 120 && entry.amount in 1.0..1440.0
        Kind.SLEEP -> entry.amount in 1.0..1440.0
        Kind.MEDICATION, Kind.SHOPPING, Kind.REPORT -> entry.title.isNotBlank() && entry.title.length <= 120
        Kind.FOCUS_SESSION -> entry.amount in 0.016..1440.0
        else -> true
    }
}
data class TimerState(val accumulatedMs: Long = 0, val startedElapsed: Long? = null, val boot: Int = -1) {
    fun elapsed(now: Long, currentBoot: Int): Long = accumulatedMs + if (startedElapsed != null && boot == currentBoot) (now - startedElapsed).coerceAtLeast(0) else 0
    fun pause(now: Long, currentBoot: Int) = TimerState(elapsed(now, currentBoot))
}
data class Recipe(val name: String, val description: String, val minutes: Int, val cuisine: String, val allergens: Set<String>, val emoji: String, val ingredients: List<String>, val steps: List<String>)
val recipes = listOf(
    Recipe("A little bowl of comfort", "Simple dhal & rice", 25, "Sri Lankan", emptySet(), "🥣", listOf("Red lentils", "Rice", "Tomato", "Turmeric", "Water"), listOf("Rinse rice and lentils separately.", "Cook rice according to the package.", "Simmer lentils with chopped tomato, turmeric and water until tender. Serve with rice.")),
    Recipe("Your colourful lunch", "Chickpea & cucumber salad", 15, "South Indian", emptySet(), "🥗", listOf("Cooked chickpeas", "Cucumber", "Tomato", "Lime", "Coriander"), listOf("Wash and chop vegetables.", "Mix with cooked chickpeas and herbs.", "Add lime to taste and serve fresh.")),
    Recipe("A slower kind of morning", "Banana & peanut oats", 10, "International", setOf("Peanuts", "Gluten"), "🍌", listOf("Oats", "Banana", "Peanuts", "Water"), listOf("Cook oats in water according to the package.", "Slice banana and add to the bowl.", "Sprinkle with peanuts. Check ingredient labels for allergens."))
)
