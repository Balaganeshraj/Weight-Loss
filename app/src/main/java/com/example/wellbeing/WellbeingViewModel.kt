package com.example.wellbeing

import android.app.Application
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val android.content.Context.preferences by preferencesDataStore("appearance")
class WellbeingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: JournalRepository = LocalRepository(app)
    val reminderController = ReminderController(app)
    val reminderPreferences = reminderController.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderPreferences())
    fun configureReminders(value: ReminderPreferences) = write { reminderController.configure(value) }
    val entries = MutableStateFlow<List<Entry>>(emptyList())
    val loaded = MutableStateFlow(false)
    val error = MutableStateFlow<Int?>(null)
    val busy = MutableStateFlow(false)
    val dark = app.preferences.data.map { it[booleanPreferencesKey("dark")] ?: false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val boot get() = Settings.Global.getInt(getApplication<Application>().contentResolver, Settings.Global.BOOT_COUNT, -1)
    init { viewModelScope.launch { repository.entries.catch { error.value = R.string.load_error }.collect { entries.value = it; loaded.value = true } } }
    fun setDark(value: Boolean) { viewModelScope.launch { getApplication<Application>().preferences.edit { it[booleanPreferencesKey("dark")] = value } } }
    private fun write(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try { action() } catch (_: Exception) { error.value = R.string.save_error } finally { busy.value = false }
        }
    }
    fun save(entry: Entry) = write { repository.save(entry) }
    fun delete(id: String) = write { repository.delete(id) }
    fun clear() = write { reminderController.disable(); repository.clear() }
    fun profile(): Profile? = entries.value.firstOrNull { it.kind == Kind.PROFILE }?.let {
        val j = JSONObject(it.extra)
        Profile(j.getInt("age"), j.optDouble("height").takeIf { n -> n.isFinite() }, j.optBoolean("fluidRestriction"), j.optBoolean("bmiExcluded"), j.optString("allergens").split('|').filter(String::isNotBlank).toSet())
    }
    fun saveProfile(profile: Profile) {
        require(profile.age in 18..120)
        require(profile.heightCm == null || profile.heightCm in 100.0..250.0)
        save(Entry(id = "profile", kind = Kind.PROFILE, title = "Offline profile", extra = JSONObject().put("age", profile.age).put("height", profile.heightCm).put("fluidRestriction", profile.fluidRestriction).put("bmiExcluded", profile.bmiExcluded).put("allergens", profile.allergens.joinToString("|")).put("privacyVersion", "offline-v1").put("acceptedAt", System.currentTimeMillis()).toString()))
    }
    fun timer(): TimerState = entries.value.firstOrNull { it.kind == Kind.TIMER }?.let {
        val j = JSONObject(it.extra)
        TimerState(j.getLong("accumulated"), if (j.isNull("started")) null else j.getLong("started"), j.getInt("boot"))
    } ?: TimerState()
    fun updateTimer(timer: TimerState) = save(Entry(id = "timer", kind = Kind.TIMER, extra = JSONObject().put("accumulated", timer.accumulatedMs).put("started", timer.startedElapsed).put("boot", timer.boot).toString()))
    fun finishTimer(title: String, intensity: String) {
        val ms = timer().elapsed(SystemClock.elapsedRealtime(), boot)
        if (ms !in 1000..86_400_000) { error.value = R.string.timer_error; return }
        write {
            // Stable session ID makes retries replace the same local session.
            val sessionId = entries.value.firstOrNull { it.kind == Kind.TIMER }?.at ?: System.currentTimeMillis()
            repository.finishSession(Entry(id = "session-$sessionId", kind = Kind.ACTIVITY, title = title, amount = ms / 60000.0, note = intensity))
        }
    }
    fun export(): String = JSONObject().put("format", "wellbeing-offline-v1").put("exportedAt", System.currentTimeMillis()).put("records", JSONArray(entries.value.filter { it.kind != Kind.TIMER }.map { it.json() })).toString(2)
}
