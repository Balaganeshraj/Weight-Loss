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
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val android.content.Context.preferences by preferencesDataStore("appearance")
class WellbeingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: JournalRepository = LocalRepository(app)
    val reminderController = ReminderController(app)
    val healthData = HealthData(app, repository)
    private var healthJob: kotlinx.coroutines.Job? = null
    fun syncHealth() { if (healthJob?.isActive == true) return; healthJob = viewModelScope.launch(Dispatchers.IO) { healthData.sync() } }
    fun cancelHealth() { healthJob?.cancel() }
    fun clearImportedHealth() { cancelHealth(); write { healthJob?.join(); repository.entries.first().filter { it.kind == Kind.DEVICE_SNAPSHOT }.forEach { repository.delete(it.id) } } }
    val reminderPreferences = reminderController.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderPreferences())
    fun configureReminders(value: ReminderPreferences) = write { reminderController.configure(value) }
    val entries = MutableStateFlow<List<Entry>>(emptyList())
    val loaded = MutableStateFlow(false)
    val error = MutableStateFlow<Int?>(null)
    val busy = MutableStateFlow(false)
    val dark = app.preferences.data.map { it[booleanPreferencesKey("dark")] ?: false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val systemTheme = app.preferences.data.map { it[booleanPreferencesKey("system_theme")] ?: true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reducedMotion = app.preferences.data.map { it[booleanPreferencesKey("reduced_motion")] ?: false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hideMetrics = app.preferences.data.map { it[booleanPreferencesKey("hide_metrics")] ?: false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setting(key: String, value: Boolean) { require(key in setOf("system_theme", "reduced_motion", "hide_metrics")); viewModelScope.launch { getApplication<Application>().preferences.edit { it[booleanPreferencesKey(key)] = value } } }
    val boot get() = Settings.Global.getInt(getApplication<Application>().contentResolver, Settings.Global.BOOT_COUNT, -1)
    init { viewModelScope.launch { repository.entries.catch { error.value = R.string.load_error }.collect { entries.value = it; loaded.value = true } } }
    fun setDark(value: Boolean) { viewModelScope.launch { getApplication<Application>().preferences.edit { it[booleanPreferencesKey("dark")] = value; it[booleanPreferencesKey("system_theme")] = false } } }
    internal fun write(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try { EntryReminders.mutex.withLock { action() } } catch (_: Exception) { error.value = R.string.save_error } finally { busy.value = false }
        }
    }
    fun save(entry: Entry) = write { repository.save(entry); EntryReminders.schedule(getApplication(), entry) }
    fun saveConfirmed(entry: Entry, onSaved: () -> Unit) = write {
        repository.save(entry)
        EntryReminders.schedule(getApplication(), entry)
        kotlinx.coroutines.withContext(Dispatchers.Main) { onSaved() }
    }
    fun delete(id: String) = write { repository.delete(id); EntryReminders.cancel(getApplication(), id) }
    fun clear() { cancelHealth(); write { healthJob?.join(); reminderController.disable(); repository.clear(); EntryReminders.clear(getApplication()) } }
    fun profile(): Profile? = entries.value.firstOrNull { it.kind == Kind.PROFILE }?.let {
        val j = JSONObject(it.extra)
        Profile(j.getInt("age"), j.optDouble("height").takeIf { n -> n.isFinite() }, j.optBoolean("fluidRestriction"), j.optBoolean("bmiExcluded"), j.optString("allergens").split('|').filter(String::isNotBlank).toSet())
    }
    fun saveProfile(profile: Profile) {
        require(profile.age in 18..120)
        require(profile.heightCm == null || profile.heightCm in 100.0..250.0)
        save(Entry(id = "profile", kind = Kind.PROFILE, title = "Offline profile", extra = JSONObject().put("age", profile.age).put("height", profile.heightCm).put("fluidRestriction", profile.fluidRestriction).put("bmiExcluded", profile.bmiExcluded).put("allergens", profile.allergens.joinToString("|")).put("privacyVersion", "offline-v1").put("acceptedAt", System.currentTimeMillis()).toString()))
    }
    fun timer(): TimerState = entries.value.firstOrNull { it.id == "timer" }?.let {
        val j = JSONObject(it.extra)
        TimerState(j.getLong("accumulated"), if (j.isNull("started")) null else j.getLong("started"), j.getInt("boot"))
    } ?: TimerState()
    fun updateTimer(timer: TimerState) = save(Entry(id = "timer", kind = Kind.TIMER, extra = JSONObject().put("accumulated", timer.accumulatedMs).put("started", timer.startedElapsed).put("boot", timer.boot).toString()))
    fun finishTimer(title: String, intensity: String) {
        val ms = timer().elapsed(SystemClock.elapsedRealtime(), boot)
        if (ms !in 1000..86_400_000) { error.value = R.string.timer_error; return }
        write {
            // Stable session ID makes retries replace the same local session.
            val sessionId = entries.value.firstOrNull { it.id == "timer" }?.at ?: System.currentTimeMillis()
            repository.finishSession(Entry(id = "session-$sessionId", kind = Kind.ACTIVITY, title = title, amount = ms / 60000.0, note = intensity))
        }
    }
    fun focusTimer(): TimerState = entries.value.firstOrNull { it.id == "focus-timer" }?.let {
        val j = JSONObject(it.extra)
        TimerState(j.getLong("accumulated"), if (j.isNull("started")) null else j.getLong("started"), j.getInt("boot"))
    } ?: TimerState()
    fun updateFocus(timer: TimerState) = save(Entry(id = "focus-timer", kind = Kind.TIMER, extra = JSONObject().put("accumulated", timer.accumulatedMs).put("started", timer.startedElapsed).put("boot", timer.boot).toString()))
    fun finishFocus() {
        val ms = focusTimer().elapsed(SystemClock.elapsedRealtime(), boot)
        if (ms !in 1000..86_400_000) { error.value = R.string.timer_error; return }
        val sessionId = entries.value.firstOrNull { it.id == "focus-timer" }?.at ?: return
        write { repository.finishSession(Entry(id = "focus-$sessionId", kind = Kind.FOCUS_SESSION, title = "Focus", amount = ms / 60000.0), "focus-timer") }
    }
    fun completeTask(entry: Entry, day: java.time.LocalDate) {
        val j = entry.metadata()
        val dates = j.optString("completed").split('|').filter(String::isNotBlank).toMutableSet()
        if (!dates.add(day.toString())) dates.remove(day.toString())
        save(entry.copy(extra = j.put("completed", dates.sorted().joinToString("|")).toString()))
    }
    fun workoutDraft() = entries.value.firstOrNull { it.id == "workout-draft" }
    fun workoutTimer(): TimerState = workoutDraft()?.metadata()?.let { j ->
        TimerState(j.optLong("accumulated"), if (!j.has("started") || j.isNull("started")) null else j.getLong("started"), j.optInt("boot", -1))
    } ?: TimerState()
    fun updateWorkout(timer: TimerState? = null, sets: JSONArray? = null, onSaved: () -> Unit = {}) {
        val old = workoutDraft()
        val j = old?.metadata() ?: JSONObject().put("sessionId", java.util.UUID.randomUUID().toString())
        timer?.let { j.put("accumulated", it.accumulatedMs).put("started", it.startedElapsed).put("boot", it.boot) }
        sets?.let { j.put("sets", it) }
        saveConfirmed(Entry(id = "workout-draft", kind = Kind.TIMER, at = old?.at ?: System.currentTimeMillis(), title = "Workout draft", extra = j.toString()), onSaved)
    }
    fun finishWorkout(partial: Boolean) {
        val draft = workoutDraft() ?: return
        val ms = workoutTimer().elapsed(SystemClock.elapsedRealtime(), boot)
        if (ms !in 1000..86_400_000) { error.value = R.string.timer_error; return }
        val j = draft.metadata().put("partial", partial)
        write { repository.finishSession(Entry(id = "workout-${j.getString("sessionId")}", kind = Kind.ACTIVITY, title = "Workout", amount = ms / 60000.0, note = if (partial) "Finished early" else "Completed", extra = j.toString()), "workout-draft") }
    }
    fun export(): String = JSONObject().put("format", "wellbeing-offline-v1").put("exportedAt", System.currentTimeMillis()).put("records", JSONArray(entries.value.filter { it.kind != Kind.TIMER }.map { it.json() })).toString(2)
}
