package com.example.wellbeing

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.content.edit
import java.util.Locale

object Language {
    private const val PREFS = "app_language"
    fun tag(context: Context): String {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val system = context.getSystemService(android.app.LocaleManager::class.java).applicationLocales
            if (!system.isEmpty && system[0].language in setOf("en", "si", "ta")) return system[0].language
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tag", "en") ?: "en"
    }
    fun apply(context: Context) = AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag(context)))
    fun set(context: Context, tag: String) {
        require(tag in setOf("en", "si", "ta"))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString("tag", tag) }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
    fun context(context: Context): Context = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag(context))) })
}
