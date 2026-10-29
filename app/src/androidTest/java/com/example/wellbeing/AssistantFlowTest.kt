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
class AssistantFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun promotionSlotsAndBilingualAssistantDemo() {
        compose.activityRule.scenario.onActivity { ViewModelProvider(it)[WellbeingViewModel::class.java].saveProfile(Profile(age = 30)) }
        compose.waitUntil(60000) { compose.onAllNodesWithText("Today").fetchSemanticsNodes().isNotEmpty() }
        listOf(1,2,3).forEach { slot -> compose.onNodeWithText("PROMOTION SPACE $slot").performScrollTo().assertIsDisplayed() }
        compose.onNodeWithText("Type a message").performScrollTo()
        capture("vitalday-promotions.png")
        compose.onNodeWithText("Type a message").performClick()
        compose.onNodeWithText("What would you like help with?").performScrollTo().performTextInput("I have chest pain after exercise")
        compose.onNodeWithText("Send message").performScrollTo().performClick()
        compose.onNodeWithText("This demo cannot assess symptoms", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear conversation").performScrollTo().performClick()
        compose.onNodeWithText("தமிழ்").performScrollTo().performClick()
        compose.onNodeWithText("என் நாளைத் திட்டமிடு").performScrollTo().performClick()
        compose.onNodeWithText("ஒரு சிறிய செயலையும்", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("English").performScrollTo().performClick()
        compose.onNodeWithText("Voice demo").performScrollTo().performClick()
        compose.onNodeWithText("Preview a spoken reply", substring = true).performScrollTo().assertIsDisplayed()
        capture("vitalday-assistant.png")
        compose.onNodeWithText("Play sample reply").performScrollTo().assertExists()
        // Leaving clears typed content rather than persisting sensitive questions.
        compose.onNodeWithText("Open my planner").performScrollTo().performClick()
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("Type a message").performScrollTo().performClick()
        compose.onNodeWithText("I have chest pain after exercise").assertDoesNotExist()
    }
    private fun capture(name: String) {
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
