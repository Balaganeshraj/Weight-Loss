package com.example.wellbeing

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun adultOnboardingFoodHydrationAndPrivacy() {
        compose.waitUntil(60000) { compose.onAllNodesWithText("Your age in years").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Your age in years").performScrollTo().performTextInput("17")
        compose.onNodeWithText("Start my wellbeing journal").performScrollTo().performClick()
        compose.onNodeWithText("For adults 18–120. No date of birth needed.").assertExists()
        compose.onNodeWithText("Your age in years").performTextReplacement("30")
        compose.onNodeWithText("I understand the privacy information and wellness limitations.").performScrollTo().performClick()
        compose.onNodeWithText("Start my wellbeing journal").performScrollTo().performClick()
        compose.waitUntil(60000) { compose.onAllNodesWithText("Log a meal").fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.getExternalFilesDir(null), "vitalday-today.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithText("Add water").performScrollTo().performClick()
        compose.onNodeWithText("Save entry").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Save entry").fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(30000) { compose.onAllNodesWithContentDescription("250 ml").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log a meal").performScrollTo().performClick()
        compose.onNodeWithText("Add food").performScrollTo().performClick()
        compose.onNodeWithText("Food name").performScrollTo().performTextInput("My rice")
        compose.onNodeWithText("Amount eaten (g)").performScrollTo().performTextReplacement("150")
        compose.onNodeWithText("Energy per 100 g").performScrollTo().performTextInput("130")
        compose.onNodeWithText("Your portion: 195 kcal estimate").performScrollTo().assertExists()
        compose.onNodeWithText("Save entry").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Save entry").fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(30000) { compose.onAllNodesWithText("My rice").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("150 g · 195 kcal estimate", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithContentDescription("195 kcal").performScrollTo().assertExists()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "home.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(60000) { compose.onAllNodesWithContentDescription("195 kcal").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Move").performClick()
        compose.onAllNodesWithText("Start session").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(60000) { compose.onAllNodesWithText("Move").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Move").performClick()
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Resume").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save session").performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Start session").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Profile and privacy").performScrollTo().performClick()
        compose.onNodeWithText("Delete all my data").performScrollTo().performClick()
        compose.onNodeWithText("Type DELETE").performTextInput("DELETE")
        compose.onNodeWithText("Delete permanently").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Your age in years").fetchSemanticsNodes().isNotEmpty() }
    }
}
