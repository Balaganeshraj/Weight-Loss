package com.example.wellbeing

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.time.*

@RunWith(AndroidJUnit4::class)
class VitalFlowsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun model(action: (WellbeingViewModel) -> Unit) {
        compose.activityRule.scenario.onActivity { action(ViewModelProvider(it)[WellbeingViewModel::class.java]) }
    }
    @Test fun plannerCompletionAndFocusSurviveRecreation() {
        model { it.clear() }
        compose.waitUntil(60000) { compose.onAllNodesWithText("Your age in years").fetchSemanticsNodes().isNotEmpty() }
        model { it.saveProfile(Profile(age = 30)) }
        compose.waitUntil(60000) { compose.onAllNodesWithText("Today").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Plan").performClick()
        compose.onNodeWithText("Add task").performScrollTo().performClick()
        compose.onNodeWithText("Title").performTextInput("Review proposal")
        compose.onNodeWithText("Save", substring = false).performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Review proposal", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(60000) { compose.onAllNodesWithText("Review proposal", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("1 planned · 1 complete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("VitalDay").performScrollTo()
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.getExternalFilesDir(null), "vitalday-plan.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithText("Focus timer").performScrollTo().performClick()
        compose.onNodeWithText("Start or resume").performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(60000) { compose.onAllNodesWithText("Pause").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        compose.onNodeWithText("Save focus session").performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("No focus sessions saved yet.").fetchSemanticsNodes().isEmpty() }
        model { it.clear() }
    }
    @Test fun plannerQuietHoursDoNotShiftMedicationInstructions() {
        val zone = ZoneId.of("Asia/Colombo")
        val now = ZonedDateTime.of(2026,10,28,20,0,0,0,zone)
        val task = Entry(kind=Kind.TASK, title="Plan", amount=30.0, at=now.withHour(22).toInstant().toEpochMilli(), extra=JSONObject().put("notify",true).put("recurrence","once").toString())
        assertEquals(now.plusDays(1).withHour(8), EntryReminders.next(task, now))
        val medicine = Entry(kind=Kind.MEDICATION,title="Synthetic",extra=JSONObject().put("notify",true).put("time","23:00").toString())
        assertEquals(now.withHour(23), EntryReminders.next(medicine,now))
        assertNull(EntryReminders.next(medicine.copy(extra=JSONObject().put("notify",false).toString()),now))
    }
}
