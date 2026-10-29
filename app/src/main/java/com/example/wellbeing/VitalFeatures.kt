@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.wellbeing

import android.os.SystemClock
import androidx.core.net.toUri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable fun VitalPlanner(vm: WellbeingViewModel, entries: List<Entry>, day: LocalDate, busy: Boolean, onDay: (LocalDate) -> Unit, onAdd: () -> Unit, onEdit: (Entry) -> Unit, onFocus: () -> Unit) {
    VitalTitle("A day with room", "YOUR PLANNER")
    VitalDateStrip(day, true, onDay)
    var week by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(!week, { week = false }, { Text("Day") }, Modifier.heightIn(min = 48.dp)); FilterChip(week, { week = true }, { Text("Week") }, Modifier.heightIn(min = 48.dp)) }
    if (week) {
        val start = day.minusDays((day.dayOfWeek.value - 1).toLong())
        VitalCard { (0L..6L).forEach { offset -> val date = start.plusDays(offset); val count = entries.count { it.taskOccurs(date) }; TextButton({ onDay(date); week = false }) { Text("${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))} · $count planned") } } }
    }
    val planned = entries.filter { it.taskOccurs(day) }.sortedBy { it.taskTime() }
    VitalCard(VelnumiBlush) { Text("${planned.size} planned · ${planned.count { it.taskDone(day) }} complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("${number(planned.sumOf { it.amount })} min scheduled · ${ZoneId.systemDefault().id}"); Text("Planned meals do not count as food eaten.", style = MaterialTheme.typography.bodySmall) }
    if (planned.isEmpty()) VitalCard { Text("A little breathing room.", style = MaterialTheme.typography.titleLarge); Text("Add a task, a meal or a moment for yourself.") }
    planned.forEach { entry -> VitalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(entry.taskDone(day), { vm.completeTask(entry, day) }, enabled = !busy)
            Column(Modifier.weight(1f)) { Text("${entry.taskTime().format(DateTimeFormatter.ofPattern("HH:mm"))}  ${entry.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("${number(entry.amount)} min · ${entry.metadata().optString("priority", "Normal")}"); if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton({ onEdit(entry) }) { Text("Edit plan") }; if (entry.kind == Kind.TASK) TextButton(onFocus) { Text("Start focus") } }
    } }
    VitalButton("Add task", onAdd, Modifier.fillMaxWidth(), !busy)
    OutlinedButton(onFocus, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { VitalIcon(R.drawable.vd_ic_clock); Spacer(Modifier.width(8.dp)); Text("Focus timer") }
}

@Composable fun VitalFocus(vm: WellbeingViewModel, entries: List<Entry>, busy: Boolean, onFollowUp: () -> Unit) {
    VitalTitle("One thing at a time", "FOCUS SPACE")
    var discardFocus by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(1000) } }
    val timer = vm.focusTimer()
    val elapsed = timer.elapsed(now, vm.boot)
    val running = timer.startedElapsed != null && timer.boot == vm.boot
    VitalCard(VelnumiSoftCoral) {
        Text("Your time, without pressure", style = MaterialTheme.typography.titleMedium)
        Text("%02d:%02d:%02d".format(elapsed / 3600000, elapsed / 60000 % 60, elapsed / 1000 % 60), style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold)
        Text("Pause whenever you need a break. Saved time survives an app restart.")
    }
    VitalButton(if (running) "Pause" else "Start or resume", { vm.updateFocus(if (running) timer.pause(now, vm.boot) else TimerState(elapsed, now, vm.boot)) }, Modifier.fillMaxWidth(), !busy)
    if (elapsed > 0) VitalButton("Save focus session", vm::finishFocus, Modifier.fillMaxWidth(), !busy && elapsed >= 1000)
    OutlinedButton(onFollowUp, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Capture a follow-up") }
    if (elapsed > 0 || running) TextButton({ discardFocus = true }) { Text("Discard focus session") }
    if (discardFocus) AlertDialog(onDismissRequest = { discardFocus = false }, title = { Text("Discard this session?") }, confirmButton = { TextButton({ vm.delete("focus-timer"); discardFocus = false }) { Text("Discard") } }, dismissButton = { TextButton({ discardFocus = false }) { Text("Keep session") } })
    VitalCard { Text("Recent focus", style = MaterialTheme.typography.titleLarge); val sessions = entries.filter { it.kind == Kind.FOCUS_SESSION }.sortedByDescending { it.at }.take(10); if (sessions.isEmpty()) Text("No focus sessions saved yet.") else sessions.forEach { Text("${Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate()} · ${number(it.amount)} min") } }
}

@Composable fun VitalSleep(entries: List<Entry>, onAdd: () -> Unit, onEdit: (Entry) -> Unit) {
    VitalTitle("Rest is part of the plan", "SLEEP & RECOVERY")
    VitalCard(VelnumiSoftCoral) { VitalIcon(R.drawable.vd_ic_sleep); Text("How did you rest?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Record your sleep and how rested you feel. This is your check-in, not a sleep diagnosis.") }
    VitalButton("Add sleep check-in", onAdd, Modifier.fillMaxWidth())
    val sleep = entries.filter { it.kind == Kind.SLEEP }.sortedByDescending { it.at }
    if (sleep.isEmpty()) VitalCard { Text("No sleep data yet. Add it manually or check your connection.") }
    sleep.take(30).forEach { entry -> VitalCard { Text(Instant.ofEpochMilli(entry.at).atZone(ZoneId.systemDefault()).toLocalDate().toString(), style = MaterialTheme.typography.labelLarge); Text("${number(entry.amount / 60)} h", style = MaterialTheme.typography.headlineSmall); Text(entry.note); Text("Self-reported · not device measured", style = MaterialTheme.typography.bodySmall); TextButton({ onEdit(entry) }) { Text("Edit check-in") } } }
}

@Composable fun VitalMedication(vm: WellbeingViewModel, entries: List<Entry>, busy: Boolean, onAdd: () -> Unit, onEdit: (Entry) -> Unit) {
    VitalTitle("Your prescribed routine", "MEDICATION")
    VitalCard(VelnumiWarm) { Text("Follow your prescribed instructions. For a missed dose, consult those instructions or your pharmacist. Do not double a dose based on this app.") }
    VitalButton("Add medication", onAdd, Modifier.fillMaxWidth(), !busy)
    val medicines = entries.filter { it.kind == Kind.MEDICATION }
    if (medicines.isEmpty()) VitalCard { Text("No medication entered. Add only what has been prescribed for you.") }
    medicines.forEach { medicine ->
        val id = "dose-${medicine.id}-${LocalDate.now()}"
        val status = entries.find { it.id == id }?.note
        VitalCard {
            Text(medicine.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(medicine.note)
            Text("Daily check-in · ${medicine.metadata().optString("time", "09:00")}")
            if (status != null) Text(status, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Taken", "Skipped").forEach { action -> OutlinedButton({ vm.save(Entry(id = id, kind = Kind.MEDICATION_EVENT, title = medicine.title, note = action, extra = JSONObject().put("medicationId", medicine.id).toString())) }, enabled = !busy && status != action) { Text(action) } }
                TextButton({ onEdit(medicine) }) { Text("Edit") }
            }
        }
    }
}

@Composable fun VitalReports(entries: List<Entry>, onAdd: () -> Unit, onEdit: (Entry) -> Unit) {
    VitalTitle("Your report notes", "PRIVATE HEALTH NOTES")
    VitalCard(VelnumiRose) { Text("Keep your own notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Manually record the report name, date and questions for your clinician. File upload, OCR, AI interpretation and clinician sharing are not connected. No diagnosis or reference range is generated.") }
    VitalButton("Add report note", onAdd, Modifier.fillMaxWidth())
    entries.filter { it.kind == Kind.REPORT }.sortedByDescending { it.at }.forEach { entry -> VitalCard { Text(entry.title, style = MaterialTheme.typography.titleLarge); Text(Instant.ofEpochMilli(entry.at).atZone(ZoneId.systemDefault()).toLocalDate().toString()); Text(entry.note); TextButton({ onEdit(entry) }) { Text("Edit note") } } }
}

@Composable fun VitalShopping(vm: WellbeingViewModel, entries: List<Entry>, busy: Boolean, onAdd: () -> Unit) {
    VitalTitle("A little preparation", "SHOPPING LIST")
    VitalButton("Add shopping item", onAdd, Modifier.fillMaxWidth(), !busy)
    val items = entries.filter { it.kind == Kind.SHOPPING }
    if (items.isEmpty()) VitalCard { Text("Your list is empty. Add ingredients and quantities before your next shop.") }
    items.forEach { item -> VitalCard { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(item.metadata().optBoolean("checked"), { vm.save(item.copy(extra = item.metadata().put("checked", it).toString())) }, enabled = !busy); Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.Bold); Text(item.note) }; IconButton({ vm.delete(item.id) }, enabled = !busy) { VitalIcon(R.drawable.vd_ic_close, description = "Remove item") } } } }
}

@Composable fun VitalBreathing() {
    VitalTitle("A moment for yourself", "RELAX")
    var active by rememberSaveable { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) { seconds = 0; while (active) { delay(1000); seconds++ } }
    VitalCard(VelnumiSoftCoral) {
        VitalIcon(R.drawable.vd_ic_heart)
        Text(if (!active) "Breathe at your own pace" else if (seconds % 8 < 4) "Breathe in gently" else "Breathe out gently", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Let your breathing stay comfortable. No breath holding is needed. Stop if you feel dizzy or uncomfortable.")
        if (active) Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}")
    }
    VitalButton(if (active) "Stop" else "Begin a pause", { active = !active }, Modifier.fillMaxWidth())
    VitalCard { Text("A quiet reset", style = MaterialTheme.typography.titleLarge); Text("Sit comfortably. Relax your shoulders. Notice the support beneath you. Let your attention return to a natural breath. You can stop at any time.") }
}

@Composable fun VitalExerciseLibrary(onStart: (String) -> Unit) {
    Text("Movement ideas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("General examples, not a prescribed program. Choose a comfortable activity and stop for pain, dizziness or unusual breathlessness.")
    listOf("Easy walk" to "Begin slowly, choose a comfortable route and finish at an easy pace. Adjust duration to how you feel.", "Gentle mobility" to "Use slow, comfortable shoulder and ankle movements. Stay within your comfortable range; never force a stretch.", "Your own gym session" to "Follow a program agreed with a qualified professional. Log the session without estimating calories burned.").forEach { (title, description) -> VitalCard { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(description); TextButton({ onStart(title) }) { Text("Log movement") } } }
}

@Composable fun VitalDeviceScreen(vm: WellbeingViewModel, entries: List<Entry>) {
    val context = LocalContext.current
    val syncing by vm.healthData.syncing.collectAsStateWithLifecycle()
    val result by vm.healthData.result.collectAsStateWithLifecycle()
    var availability by remember { mutableIntStateOf(vm.healthData.availability()) }
    var steps by rememberSaveable { mutableStateOf(false) }
    var sleep by rememberSaveable { mutableStateOf(false) }
    var granted by remember { mutableStateOf(emptySet<String>()) }
    var launchError by remember { mutableStateOf(false) }
    var deleteImported by rememberSaveable { mutableStateOf(false) }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()) { granted = it }
    LaunchedEffect(availability) {
        if (availability == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) runCatching {
            granted = androidx.health.connect.client.HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
            steps = HealthData.STEPS in granted; sleep = HealthData.SLEEP in granted
        }
    }
    VitalTitle("Your devices, your choice", "DEVICES & DATA")
    VitalCard {
        Image(androidx.compose.ui.res.painterResource(R.drawable.velnumi_watch), null, Modifier.fillMaxWidth().height(170.dp))
        Text("Connect your fitness device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Your watch or fitness app must first write data to Health Connect. VELNUMI reads only permitted steps and sleep on this phone. No cloud upload, heart-rate access or universal Bluetooth pairing.")
        Text("Imported data is encrypted locally and can be exported or deleted in Profile and privacy. Access can be revoked in Health Connect.", style = MaterialTheme.typography.bodySmall)
    }
    if (availability != androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
        VitalCard(VelnumiWarm) {
            Text("Health Connect needs setup or is unavailable on this device.")
            if (availability == androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) VitalButton("Open Health Connect setup", {
                launchError = runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri())) }.isFailure
            })
            TextButton({ availability = vm.healthData.availability() }) { Text("Check again") }
            Text("Manual tracking always works.")
        }
    } else {
        VitalCard {
            Text("Choose permissions to request. To revoke existing access, use Manage permissions.")
            VitalToggle("Steps", steps) { steps = it }
            VitalToggle("Sleep", sleep) { sleep = it }
            VitalButton("Review Android permissions", {
                val requested = buildSet { if (steps) add(HealthData.STEPS); if (sleep) add(HealthData.SLEEP) }
                launchError = runCatching { permission.launch(requested) }.isFailure
            }, Modifier.fillMaxWidth(), !syncing && (steps || sleep))
            TextButton({ launchError = runCatching { context.startActivity(android.content.Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")) }.isFailure }) { Text("Manage permissions") }
            Text("Sync reads the permissions currently granted in Android, not just these request switches.", style = MaterialTheme.typography.bodySmall)
            VitalButton(if (syncing) "Reading allowed data…" else "Sync here", vm::syncHealth, Modifier.fillMaxWidth(), !syncing)
            if (syncing) TextButton(vm::cancelHealth) { Text("Cancel sync") }
        }
    }
    if (launchError) VitalCard { Text("This action is unavailable. Open Health Connect from your phone settings, then try again.") }
    if (result.isNotBlank()) VitalCard {
        Text("Sync result", style = MaterialTheme.typography.titleLarge)
        result.split('|').forEach { status -> Text(when(status) {
            "steps:updated" -> "Steps updated from Health Connect."
            "sleep:updated" -> "Sleep updated from Health Connect."
            "steps:no_new" -> "No new steps data. Previous source time is retained."
            "sleep:no_new" -> "No new sleep data. Previous source time is retained."
            "steps:denied" -> "Steps permission is not granted."
            "sleep:denied" -> "Sleep permission is not granted."
            "steps:failed" -> "Steps could not be updated. Try again."
            "sleep:failed" -> "Sleep could not be updated. Try again."
            "cancelled" -> "Sync cancelled. Previously saved data is unchanged."
            "timeout" -> "Sync timed out. Try again."
            "setup" -> "Health Connect needs setup."
            else -> "Sync failed. Previously saved data is unchanged."
        }) }
    }
    TextButton({ deleteImported = true }, enabled = !syncing) { Text("Delete imported health data") }
    if (deleteImported) AlertDialog(onDismissRequest = { deleteImported = false }, title = { Text("Delete imported health data?") }, text = { Text("This removes VELNUMI copies only. Source records in Health Connect and your manual journal stay unchanged.") }, confirmButton = { TextButton({ vm.clearImportedHealth(); deleteImported = false }) { Text("Delete") } }, dismissButton = { TextButton({ deleteImported = false }) { Text("Cancel") } })
    val snapshot = entries.find { it.id == "health-${LocalDate.now()}" }?.metadata()
    if (snapshot != null) VitalCard(VelnumiRose) {
        Text("Last confirmed data", style = MaterialTheme.typography.titleLarge)
        if (snapshot.has("steps")) { VitalMetric(snapshot.getDouble("steps"), "steps"); Text("Health Connect · ${Instant.ofEpochMilli(snapshot.getLong("stepsAt")).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM HH:mm"))}") }
        if (snapshot.has("sleepMinutes")) { VitalMetric(snapshot.getDouble("sleepMinutes") / 60, "h"); Text("Sleep sessions ending today · ${Instant.ofEpochMilli(snapshot.getLong("sleepAt")).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM HH:mm"))}") }
        Text("Saved readings may be stale. Sync again to recheck access and freshness. Food intake is never changed by device sync.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun VitalEntryDialog(kind: Kind, existing: Entry?, day: LocalDate, busy: Boolean, dismiss: () -> Unit, save: (Entry) -> Unit, delete: (String) -> Unit, allEntries: List<Entry>) {
    val initial = existing?.metadata() ?: JSONObject()
    var title by rememberSaveable(kind, existing?.id) { mutableStateOf(existing?.title ?: "") }
    var note by rememberSaveable(kind, existing?.id) { mutableStateOf(existing?.note ?: "") }
    var date by rememberSaveable(kind, existing?.id) { mutableStateOf(existing?.let { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate().toString() } ?: day.toString()) }
    var time by rememberSaveable(kind, existing?.id) { mutableStateOf(existing?.let { if (kind == Kind.MEDICATION) initial.optString("time", "09:00") else it.taskTime().format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "09:00") }
    var minutes by rememberSaveable(kind, existing?.id) { mutableStateOf(existing?.amount?.toInt()?.toString() ?: "30") }
    var recurrence by rememberSaveable(kind, existing?.id) { mutableStateOf(initial.optString("recurrence", "once")) }
    var priority by rememberSaveable(kind, existing?.id) { mutableStateOf(initial.optString("priority", "Normal")) }
    var project by rememberSaveable(kind, existing?.id) { mutableStateOf(initial.optString("project")) }
    var fixed by rememberSaveable(kind, existing?.id) { mutableStateOf(initial.optBoolean("fixed")) }
    var bed by rememberSaveable(kind, existing?.id) { mutableStateOf(initial.optString("bed", "${day.minusDays(1)} 22:00")) }
    var wake by rememberSaveable(kind, existing?.id) { mutableStateOf(initial.optString("wake", "$day 06:00")) }
    var notify by rememberSaveable(kind, existing?.id) { mutableStateOf(initial.optBoolean("notify")) }
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted -> notify = granted; denied = !granted }
    var tried by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val isPlan = kind in setOf(Kind.TASK, Kind.MEAL_PLAN)
    val format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)
    val parsed = runCatching { LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault()) }.getOrNull()
    val sleep = runCatching { val start = LocalDateTime.parse(bed, format).atZone(ZoneId.systemDefault()); val end = LocalDateTime.parse(wake, format).atZone(ZoneId.systemDefault()); require(!end.isAfter(ZonedDateTime.now())); PlanRules.sleepMinutes(start, end) to end }.getOrNull()
    val conflict = if (isPlan && parsed != null && minutes.toLongOrNull() in 1L..1440L) allEntries.any { other ->
        other.id != existing?.id && other.kind in setOf(Kind.TASK, Kind.MEAL_PLAN) && (fixed || other.metadata().optBoolean("fixed")) && (-1L..1L).any { offset ->
            val occurrenceDay = parsed.toLocalDate().plusDays(offset)
            other.taskOccurs(occurrenceDay) && PlanRules.overlaps(parsed.toInstant(), minutes.toLong(), occurrenceDay.atTime(other.taskTime()).atZone(ZoneId.systemDefault()).toInstant(), other.amount.toLong())
        }
    } else false
    val valid = !conflict && when (kind) { Kind.SLEEP -> sleep != null; Kind.TASK, Kind.MEAL_PLAN -> title.isNotBlank() && parsed != null && minutes.toIntOrNull() in 1..1440; Kind.MEDICATION -> title.isNotBlank() && note.isNotBlank() && runCatching { LocalTime.parse(time) }.isSuccess; else -> title.isNotBlank() && parsed != null }
    val heading = when (kind) { Kind.TASK -> "Task"; Kind.MEAL_PLAN -> "Plan a meal"; Kind.SLEEP -> "Sleep check-in"; Kind.MEDICATION -> "Medication"; Kind.REPORT -> "Report note"; else -> "Shopping item" }
    AlertDialog(onDismissRequest = { discard = true }, title = { Text(heading) }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (kind != Kind.SLEEP) OutlinedTextField(title, { title = it.take(120) }, label = { Text(if (kind == Kind.MEDICATION) "Medicine name and strength" else "Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (kind == Kind.SLEEP) {
                OutlinedTextField(bed, { bed = it.take(16) }, label = { Text("Bedtime · YYYY-MM-DD HH:MM") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(wake, { wake = it.take(16) }, label = { Text("Wake time · YYYY-MM-DD HH:MM") }, modifier = Modifier.fillMaxWidth())
                Text("Dates use your phone time zone. Sleep duration must be between 1 minute and 24 hours, ending in the past.", style = MaterialTheme.typography.bodySmall)
                if (sleep != null) Text("${number(sleep.first / 60.0)} h in this sleep interval")
            } else if (isPlan || kind == Kind.REPORT) OutlinedTextField(date, { date = it.take(10) }, label = { Text("Date · YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (isPlan || kind == Kind.MEDICATION) OutlinedTextField(time, { time = it.take(5) }, label = { Text("Time · HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (isPlan) {
                OutlinedTextField(minutes, { minutes = it.take(4) }, label = { Text("Planned minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(project, { project = it.take(80) }, label = { Text("Project · optional") }, modifier = Modifier.fillMaxWidth())
                VitalToggle("Fixed time", fixed) { fixed = it }
                Text("Repeat", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("once" to "Once", "daily" to "Daily", "weekdays" to "Weekdays", "weekly" to "Weekly").forEach { (id, label) -> FilterChip(recurrence == id, { recurrence = id }, { Text(label) }, Modifier.heightIn(min = 48.dp)) } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Low", "Normal", "High").forEach { label -> FilterChip(priority == label, { priority = label }, { Text(label) }, Modifier.heightIn(min = 48.dp)) } }
            }
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text(when(kind) { Kind.MEDICATION -> "Prescribed instructions and route"; Kind.SLEEP -> "How rested do you feel?"; Kind.SHOPPING -> "Quantity and notes"; else -> "Notes" }) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            if (isPlan || kind == Kind.MEDICATION) {
                VitalToggle("Local reminder", notify) { enabled ->
                    if (!enabled) notify = false
                    else if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    else { notify = true; denied = !ReminderController(context).canNotify() }
                }
                Text("Delivery may be delayed by Android. Planner reminders respect 21:00–08:00 quiet hours; medication uses your chosen time.", style = MaterialTheme.typography.bodySmall)
                if (denied) Text("Notifications are disabled. Your entry remains in the app; enable notifications in phone settings.")
            }
            if (kind == Kind.MEDICATION) Text("This is a daily check-in list, not a dosing schedule. Keep every prescribed instruction and do not change a dose here.")
            if (conflict) Text("This overlaps a fixed plan. Choose another time.", color = MaterialTheme.colorScheme.error)
            if (tried && !valid) Text("Check the required fields, dates and amounts.", color = MaterialTheme.colorScheme.error)
            if (existing != null) TextButton({ deleting = true }, enabled = !busy) { Text("Delete entry") }
        }
    }, confirmButton = { VitalButton("Save", {
        tried = true
        if (valid) {
            val meta = initial.put("project", project.trim()).put("fixed", fixed).put("notify", notify).put("recurrence", recurrence).put("priority", priority).put("time", time).put("bed", bed).put("wake", wake)
            save(Entry(id = existing?.id ?: UUID.randomUUID().toString(), kind = kind, at = if (kind == Kind.SLEEP) sleep!!.second.toInstant().toEpochMilli() else parsed?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis(), title = if (kind == Kind.SLEEP) "Sleep" else title.trim(), amount = when { kind == Kind.SLEEP -> sleep!!.first.toDouble(); isPlan -> minutes.toDouble(); else -> 0.0 }, note = note.trim(), extra = meta.toString()))
        }
    }, enabled = !busy) }, dismissButton = { TextButton({ discard = true }) { Text("Cancel") } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this draft?") }, text = { Text("Keep editing to preserve your changes.") }, confirmButton = { TextButton(dismiss) { Text("Discard") } }, dismissButton = { TextButton({ discard = false }) { Text("Keep editing") } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Delete this entry?") }, confirmButton = { TextButton({ existing?.let { delete(it.id) } }) { Text("Delete") } }, dismissButton = { TextButton({ deleting = false }) { Text("Cancel") } })
}

@Composable fun VitalWorkoutPlayer(vm: WellbeingViewModel, entries: List<Entry>, busy: Boolean) {
    VitalTitle("Move at your own pace", "WORKOUT LOG")
    var discardWorkout by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(1000) } }
    val draft = entries.find { it.id == "workout-draft" }
    val sets = draft?.metadata()?.optJSONArray("sets") ?: org.json.JSONArray()
    val timer = vm.workoutTimer()
    val elapsed = timer.elapsed(now, vm.boot)
    val running = timer.startedElapsed != null && timer.boot == vm.boot
    var exercise by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var load by rememberSaveable { mutableStateOf("") }
    VitalCard(VelnumiWarm) { Text("Stop for pain, dizziness or unusual breathlessness. Seek appropriate care for concerning symptoms. Follow a routine suited to you; these logs do not prescribe exercises or loads.") }
    VitalCard(VelnumiBlush) { Text("%02d:%02d".format(elapsed / 60000, elapsed / 1000 % 60), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Text("Pause for rest. Completed sets are saved on this phone.") }
    VitalButton(if (running) "Pause" else "Start or resume", { vm.updateWorkout(if (running) timer.pause(now, vm.boot) else TimerState(elapsed, now, vm.boot)) }, Modifier.fillMaxWidth(), !busy)
    VitalCard {
        OutlinedTextField(exercise, { exercise = it.take(120) }, label = { Text("Exercise") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(reps, { reps = it.take(3) }, label = { Text("Repetitions completed") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(load, { load = it.take(6) }, label = { Text("Load in kg · optional") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        val valid = exercise.isNotBlank() && reps.toIntOrNull() in 1..200 && (load.isBlank() || (decimal(load)?.let { it in 0.0..500.0 } == true))
        VitalButton("Save completed set", {
            val changed = org.json.JSONArray(sets.toString())
            changed.put(JSONObject().put("exercise", exercise.trim()).put("reps", reps.toInt()).put("loadKg", decimal(load) ?: JSONObject.NULL))
            vm.updateWorkout(sets = changed) { exercise = ""; reps = ""; load = "" }
        }, Modifier.fillMaxWidth(), valid && !busy && sets.length() < 200)
    }
    if (sets.length() > 0) VitalCard {
        Text("Completed sets", style = MaterialTheme.typography.titleLarge)
        (0 until sets.length()).forEach { i -> val set = sets.getJSONObject(i)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}. ${set.getString("exercise")} · ${set.getInt("reps")} reps" + if (set.isNull("loadKg")) "" else " · ${number(set.getDouble("loadKg"))} kg", Modifier.weight(1f))
                IconButton({ val changed = org.json.JSONArray(sets.toString()); changed.remove(i); vm.updateWorkout(sets = changed) }, enabled = !busy) { VitalIcon(R.drawable.vd_ic_close, description = "Remove set") }
            }
        }
    }
    VitalButton("Finish workout", { vm.finishWorkout(false) }, Modifier.fillMaxWidth(), !busy && elapsed >= 1000)
    OutlinedButton({ vm.finishWorkout(true) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !busy && elapsed >= 1000) { Text("Finish early and save") }
    if (draft != null) TextButton({ discardWorkout = true }) { Text("Discard workout draft") }
    if (discardWorkout) AlertDialog(onDismissRequest = { discardWorkout = false }, title = { Text("Discard this workout and its sets?") }, confirmButton = { TextButton({ vm.delete("workout-draft"); discardWorkout = false }) { Text("Discard") } }, dismissButton = { TextButton({ discardWorkout = false }) { Text("Keep session") } })
    Text("A finished session appears in Move. No exercise calories are added to food allowances.", style = MaterialTheme.typography.bodySmall)
}

@Composable fun VitalNutrients(entries: List<Entry>) {
    val food = entries.filter { it.kind == Kind.FOOD }
    VitalCard(VelnumiRose) {
        Text("Your nourishment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val nutrients = listOf("Protein" to "proteinG", "Carbohydrate" to "carbohydrateG", "Fat" to "fatG", "Fibre" to "fiberG", "Sodium" to "sodiumMg")
        nutrients.forEach { (label, key) ->
            val known = food.map { it.metadata() }.filter { it.has(key) && !it.isNull(key) }
            val amount = if (food.isNotEmpty() && known.size == food.size) known.sumOf { it.getDouble(key) } else null
            Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Text(if (amount == null) "Not available" else "${number(amount)} ${if (key == "sodiumMg") "mg" else "g"}", fontWeight = FontWeight.SemiBold) }
        }
        Text("Totals appear only when every logged food has that nutrient. Missing values are not counted as zero.", style = MaterialTheme.typography.bodySmall)
    }
}
