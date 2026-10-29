package com.example.wellbeing

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrandFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun brandedHomeAndSleepRetainCareAccess() {
        compose.activityRule.scenario.onActivity {
            ViewModelProvider(it)[WellbeingViewModel::class.java].apply {
                saveProfile(Profile(age = 30)); setting("system_theme", false); setDark(false)
            }
        }
        compose.waitUntil(60000) { compose.onAllNodesWithText("VELNUMI").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nutrition", substring = false).assertExists()
        compose.onAllNodesWithText("Your Life. In Balance.").onFirst().assertExists()
        capture("velnumi-today.png")
        compose.onAllNodesWithText("Sleep", substring = false).onLast().performClick()
        compose.onNodeWithText("Care, medication and reports").performScrollTo().performClick()
        compose.onNodeWithText("Report notes").performScrollTo().assertExists()
        compose.onNodeWithText("Medication", substring = false).assertExists()
        compose.onNodeWithText("Today").performClick()
        lateinit var model: WellbeingViewModel
        compose.activityRule.scenario.onActivity { model = ViewModelProvider(it)[WellbeingViewModel::class.java]; model.setDark(true) }
        compose.waitUntil(30000) { model.dark.value }
        compose.waitForIdle()
        capture("velnumi-dark.png")
        compose.activityRule.scenario.onActivity { ViewModelProvider(it)[WellbeingViewModel::class.java].setDark(false) }
    }
    private fun capture(name: String) {
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
