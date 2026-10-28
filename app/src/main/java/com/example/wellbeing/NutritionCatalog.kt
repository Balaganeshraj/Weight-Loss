package com.example.wellbeing

import android.content.Context
import org.json.JSONObject

data class ReferenceFood(val id: String, val key: String, val sourceDescription: String, val kcal: Double, val protein: Double, val carbohydrate: Double, val fat: Double, val fiber: Double, val version: String) {
    val nameResource get() = when(key) {
        "rice" -> R.string.food_rice; "lentils" -> R.string.food_lentils; "banana" -> R.string.food_banana
        "egg" -> R.string.food_egg; "oats" -> R.string.food_oats; "apple" -> R.string.food_apple
        "blueberries" -> R.string.food_blueberries; else -> R.string.food_orange
    }
    fun macros(grams: Double) = Macros(protein * grams / 100, carbohydrate * grams / 100, fat * grams / 100, fiber * grams / 100)
}
data class Macros(val protein: Double, val carbohydrate: Double, val fat: Double, val fiber: Double)
object NutritionCatalog {
    @Volatile private var cache: List<ReferenceFood>? = null
    @Synchronized fun load(context: Context): List<ReferenceFood> {
        cache?.let { return it }
        val json = JSONObject(context.assets.open("nutrition/catalog.json").bufferedReader().use { it.readText() })
        val list=json.getJSONArray("foods")
        return (0 until list.length()).map { i -> val f=list.getJSONObject(i)
            ReferenceFood(f.getString("id"), f.getString("key"), f.getString("sourceDescription"), f.getDouble("kcal"), f.getDouble("proteinG"), f.getDouble("carbohydrateG"), f.getDouble("fatG"), f.getDouble("fiberG"),json.getString("version"))
        }.also { cache = it }
    }
}
