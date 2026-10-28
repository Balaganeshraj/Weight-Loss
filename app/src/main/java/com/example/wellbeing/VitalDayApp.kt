@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.wellbeing

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

val VitalLime = Color(0xFFD4F568)
val VitalTeal = Color(0xFFBCE9DE)
val VitalLilac = Color(0xFFD9D0F2)
val VitalAmber = Color(0xFFFFDBA4)
val VitalInk = Color(0xFF182116)
val LocalVitalMotion = staticCompositionLocalOf { false }

@Composable fun VitalButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduced = LocalVitalMotion.current
    val scale by animateFloatAsState(if (pressed && !reduced && enabled) .98f else 1f, tween(if (reduced) 0 else 120), label = "press")
    Button(onClick, modifier.heightIn(min = 52.dp).graphicsLayer { scaleX = scale; scaleY = scale }, enabled = enabled, interactionSource = source, shape = RoundedCornerShape(18.dp)) { Text(label) }
}
@Composable fun VitalIcon(id: Int, modifier: Modifier = Modifier, description: String? = null) = Icon(painterResource(id), description, modifier.size(24.dp))
@Composable fun VitalCard(color: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = color ?: MaterialTheme.colorScheme.surfaceContainer, contentColor = if (color != null) VitalInk else MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun VitalTitle(title: String, subtitle: String) {
    Text(subtitle.uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
}
@Composable fun VitalMetric(value: Double?, unit: String) {
    val reduced = LocalVitalMotion.current
    val context = LocalContext.current
    val manager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
    val immediate = reduced || manager.isEnabled
    val animated = remember { Animatable((value ?: 0.0).toFloat()) }
    var previous by remember { mutableStateOf(value) }
    LaunchedEffect(value, immediate) {
        if (value != null) {
            if (immediate || previous == null) animated.snapTo(value.toFloat()) else animated.animateTo(value.toFloat(), tween(650))
        }
        previous = value
    }
    val description = if (value == null) "Not logged" else "${number(value)} $unit"
    Text(if (value == null) "—" else "${number(if (immediate || previous == null) value else animated.value.toDouble())} $unit", style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold, modifier = Modifier.clearAndSetSemantics { contentDescription = description })
}

@Composable fun VitalDayApp(vm: WellbeingViewModel) {
    val ctx = LocalContext.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val reduced by vm.reducedMotion.collectAsStateWithLifecycle()
    val hidden by vm.hideMetrics.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableStateOf((ctx as? android.app.Activity)?.intent?.getStringExtra("destination") ?: "") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val day = LocalDate.parse(date)
    var quick by rememberSaveable { mutableStateOf(false) }
    var formName by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = entries.find { it.id == editId }
    fun edit(kind: Kind, entry: Entry? = null) { formName = kind.name; editId = entry?.id }
    fun closeForm() { formName = ""; editId = null }
    LaunchedEffect(error) { error?.let { snack.showSnackbar(ctx.getString(it)); vm.error.value = null } }
    if (!loaded) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (error == null) CircularProgressIndicator() else Text(ctx.getString(R.string.load_error)) }; return }
    val profile = vm.profile()
    if (profile == null) { Box(Modifier.fillMaxSize()) { Onboarding(busy, vm::saveProfile); SnackbarHost(snack, Modifier.align(Alignment.BottomCenter)) }; return }
    BackHandler(page.isNotEmpty()) { page = "" }
    val daily = entries.filter { WellnessMath.onDate(it, day, ZoneId.systemDefault()) }
    val titles = listOf("Today", "Food", "Move", "Plan", "Care")
    val icons = listOf(R.drawable.vd_ic_home, R.drawable.vd_ic_food, R.drawable.vd_ic_dumbbell, R.drawable.vd_ic_calendar, R.drawable.vd_ic_care)
    CompositionLocalProvider(LocalVitalMotion provides (reduced || !ValueAnimator.areAnimatorsEnabled())) {
        BoxWithConstraints {
            val wide = maxWidth >= 700.dp
            Scaffold(snackbarHost = { SnackbarHost(snack) }, bottomBar = {
                if (!wide) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    titles.forEachIndexed { i, label -> NavigationBarItem(selected = tab == i && page.isEmpty(), onClick = { tab = i; page = "" }, icon = { VitalIcon(icons[i]) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(indicatorColor = VitalLime, selectedIconColor = VitalInk)) }
                }
            }) { insets ->
                Row(Modifier.fillMaxSize().padding(insets)) {
                    if (wide) NavigationRail(Modifier.fillMaxHeight()) { titles.forEachIndexed { i, label -> NavigationRailItem(selected = tab == i && page.isEmpty(), onClick = { tab = i; page = "" }, icon = { VitalIcon(icons[i]) }, label = { Text(label) }) } }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(remember(page, tab) { ScrollState(0) }).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (page.isNotEmpty()) IconButton({ page = "" }) { VitalIcon(R.drawable.vd_ic_chevron_left, description = "Back") }
                                Text("VitalDay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton({ page = "profile" }) { VitalIcon(R.drawable.vd_ic_settings, description = "Profile and privacy") }
                                IconButton({ quick = true }) { VitalIcon(R.drawable.vd_ic_plus, description = "Quick add") }
                            }
                            when (page) {
                                "profile" -> {
                                    VitalCard {
                                        val system by vm.systemTheme.collectAsStateWithLifecycle()
                                        VitalToggle("Follow system theme", system) { vm.setting("system_theme", it) }
                                        VitalToggle("Reduce motion", reduced) { vm.setting("reduced_motion", it) }
                                        VitalToggle("Hide calories and weight", hidden) { vm.setting("hide_metrics", it) }
                                    }
                                    ProfileScreen(vm, profile, busy, snack)
                                }
                                "insights" -> if (hidden) VitalCard { Text("Calorie and weight displays are hidden. Change this in Profile and privacy.") } else InsightsScreen(entries, profile, { edit(Kind.WEIGHT) }, { edit(it.kind, it) })
                                "devices" -> VitalDeviceScreen(vm, entries)
                                "reports" -> VitalReports(entries, { edit(Kind.REPORT) }, { edit(Kind.REPORT, it) })
                                "coach" -> VitalUnavailable("Your optional coach", "Cloud AI is not connected. Your private journal stays on this phone. You can still plan your day and track your wellbeing.", "Open planner") { page = ""; tab = 3 }
                                "medication" -> VitalMedication(vm, entries, busy, { edit(Kind.MEDICATION) }, { edit(Kind.MEDICATION, it) })
                                "sleep" -> VitalSleep(entries, { edit(Kind.SLEEP) }, { edit(Kind.SLEEP, it) })
                                "relax" -> VitalBreathing()
                                "plan" -> VitalPlanner(vm, entries, day, busy, { date = it.toString() }, { edit(Kind.TASK) }, { edit(it.kind, it) }, { page = "focus" })
                                "workout" -> VitalWorkoutPlayer(vm, entries, busy)
                                "focus" -> VitalFocus(vm, entries, busy, { edit(Kind.TASK) })
                                "shopping" -> VitalShopping(vm, entries, busy, { edit(Kind.SHOPPING) })
                                else -> when (tab) {
                                    0 -> {
                                        VitalTitle("Your day, in balance", day.format(DateTimeFormatter.ofPattern("EEEE d MMMM")))
                                        VitalToday(daily, entries, day, hidden, busy, vm, onPage = { page = it }, onPlan = { tab = 3 }, onFood = { tab = 1 }, onAdd = { edit(it) })
                                    }
                                    1 -> {
                                        VitalDateStrip(day, false) { date = it.toString() }
                                        if (hidden) VitalCard { Text("Calorie and weight displays are hidden. Food entry keeps its nutrition information for your records."); VitalButton("Log a meal", { edit(Kind.FOOD) }); daily.filter { it.kind == Kind.FOOD }.forEach { food -> TextButton({ edit(Kind.FOOD, food) }) { Text(food.title) } } }
                                        else { VitalNutrients(daily); FoodScreen(daily, profile, { edit(Kind.FOOD) }, { edit(Kind.FOOD, it) }) }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton({ edit(Kind.MEAL_PLAN) }) { Text("Plan a meal") }
                                            OutlinedButton({ page = "shopping" }) { Text("Shopping list") }
                                        }
                                        VitalUnavailable("Photo and barcode", "Automated food recognition is not connected. Use the reference catalogue or enter the package label; no image will be uploaded.", "Manual entry") { edit(Kind.FOOD) }
                                    }
                                    2 -> {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton({ page = "sleep" }) { Text("Sleep diary") }
                                            OutlinedButton({ page = "relax" }) { Text("Relax") }
                                            OutlinedButton({ page = "workout" }) { Text("Workout log") }
                                        }
                                        MoveScreen(vm, entries, busy, { edit(Kind.ACTIVITY) }, { edit(it.kind, it) })
                                        VitalExerciseLibrary { title -> edit(Kind.ACTIVITY, Entry(kind = Kind.ACTIVITY, title = title, amount = 10.0)) }
                                    }
                                    3 -> VitalPlanner(vm, entries, day, busy, { date = it.toString() }, { edit(Kind.TASK) }, { edit(it.kind, it) }, { page = "focus" })
                                    4 -> VitalCare { page = it }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
        if (quick) ModalBottomSheet(onDismissRequest = { quick = false }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Make a little space", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Food" to Kind.FOOD, "Water" to Kind.WATER, "Workout" to Kind.ACTIVITY, "Task" to Kind.TASK, "Sleep" to Kind.SLEEP, "Medication" to Kind.MEDICATION, "Report note" to Kind.REPORT).forEach { (name, kind) -> OutlinedButton({ quick = false; edit(kind) }, Modifier.heightIn(min = 48.dp)) { Text(name) } }
                }
                TextButton({ quick = false; page = "coach" }) { Text("Open coach") }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (formName.isNotEmpty()) {
            val kind = Kind.valueOf(formName)
            if (kind in setOf(Kind.FOOD, Kind.WATER, Kind.ACTIVITY, Kind.WEIGHT)) EntryDialog(kind, editing, if (tab == 1) day else LocalDate.now(), busy, ::closeForm, { vm.saveConfirmed(it, ::closeForm) }, { vm.delete(it); closeForm() })
            else VitalEntryDialog(kind, editing, day, busy, ::closeForm, { vm.saveConfirmed(it, ::closeForm) }, { vm.delete(it); closeForm() }, entries)
        }
    }
}

@Composable fun VitalToggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, change) }
}
@Composable fun VitalDateStrip(day: LocalDate, allowFuture: Boolean, onDay: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ onDay(day.minusDays(1)) }) { VitalIcon(R.drawable.vd_ic_chevron_left, description = "Previous day") }
        Text(day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        IconButton({ onDay(day.plusDays(1)) }, enabled = allowFuture || day < LocalDate.now()) { VitalIcon(R.drawable.vd_ic_arrow_right, description = "Next day") }
    }
}
@Composable fun VitalToday(daily: List<Entry>, all: List<Entry>, day: LocalDate, hidden: Boolean, busy: Boolean, vm: WellbeingViewModel, onPage: (String) -> Unit, onPlan: () -> Unit, onFood: () -> Unit, onAdd: (Kind) -> Unit) {
    val tasks = all.filter { it.taskOccurs(day) }
    val done = tasks.count { it.taskDone(day) }
    val food = daily.filter { it.kind == Kind.FOOD }
    val sleep = daily.filter { it.kind == Kind.SLEEP }
    val health = all.find { it.id == "health-$day" }?.metadata()
    var dismissed by rememberSaveable(day.toString()) { mutableStateOf(false) }
    VitalCard(VitalLime) {
        Text("TODAY AT A GLANCE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Small steps count.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (health?.has("steps") == true) {
            VitalMetric(health.getDouble("steps"), "steps")
            Text("Health Connect · ${Instant.ofEpochMilli(health.getLong("stepsAt")).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM HH:mm"))}", style = MaterialTheme.typography.bodySmall)
        }
        Text(if (tasks.isEmpty()) "Give your day a little direction." else "$done / ${tasks.size} planned activities complete")
        if (tasks.isNotEmpty()) LinearProgressIndicator(progress = { done.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = VitalInk, trackColor = VitalInk.copy(alpha = .15f))
        TextButton(onPlan, colors = ButtonDefaults.textButtonColors(contentColor = VitalInk)) { Text("Make room in your plan"); VitalIcon(R.drawable.vd_ic_arrow_right) }
    }
    if (!hidden) VitalCard(VitalTeal) { Text("Nourishment", style = MaterialTheme.typography.titleMedium); VitalMetric(food.takeIf { it.isNotEmpty() }?.sumOf { it.energy }, "kcal"); Text("Confirmed food entries · $day", style = MaterialTheme.typography.bodySmall); TextButton(onFood, colors = ButtonDefaults.textButtonColors(contentColor = VitalInk)) { Text("Log a meal") } }
    VitalCard(VitalLilac) { Text("Sleep", style = MaterialTheme.typography.titleMedium); VitalMetric(sleep.takeIf { it.isNotEmpty() }?.sumOf { it.amount / 60.0 } ?: health?.takeIf { it.has("sleepMinutes") }?.getDouble("sleepMinutes")?.div(60), "h"); Text(if (sleep.isNotEmpty()) "Self-reported sleep · $day" else if (health?.has("sleepMinutes") == true) "Health Connect · $day · last saved import" else "No sleep data yet. Add your own check-in."); TextButton({ onPage("sleep") }, colors = ButtonDefaults.textButtonColors(contentColor = VitalInk)) { Text("Sleep diary") } }
    VitalCard {
        Row(verticalAlignment = Alignment.CenterVertically) { VitalIcon(R.drawable.vd_ic_water); Spacer(Modifier.width(10.dp)); Text("Water", style = MaterialTheme.typography.titleMedium) }
        val water = daily.filter { it.kind == Kind.WATER }
        VitalMetric(water.takeIf { it.isNotEmpty() }?.sumOf { it.amount }, "ml")
        VitalButton("Add water", { onAdd(Kind.WATER) }, enabled = !busy)
    }
    if (!dismissed) VitalCard(VitalAmber) {
        Text("One useful next step", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (tasks.isEmpty()) "Choose one manageable task for today." else "Leave room for a pause between your plans.")
        Text("Based on your saved plan for $day. You choose what works for you.", style = MaterialTheme.typography.bodySmall)
        TextButton({ dismissed = true }, colors = ButtonDefaults.textButtonColors(contentColor = VitalInk)) { Text("Dismiss") }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Coming up", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onPlan) { Text("View plan") } }
    VitalCard { if (tasks.none { !it.taskDone(day) }) Text("Nothing waiting. Make room for what matters.") else tasks.filter { !it.taskDone(day) }.sortedBy { it.taskTime() }.take(3).forEach { Text("${it.taskTime().format(DateTimeFormatter.ofPattern("HH:mm"))}  ${it.title}", fontWeight = FontWeight.SemiBold) } }
    VitalCard {
        Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(R.drawable.vd_connect_device), null, Modifier.size(80.dp)); Spacer(Modifier.width(12.dp)); Text("Connect your fitness device", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        Text("Connect supported health data, or continue with manual tracking.")
        VitalButton("Devices and data", { onPage("devices") }, Modifier.fillMaxWidth())
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ onPage("insights") }) { Text("Trends") }; OutlinedButton({ onPage("coach") }) { Text("Coach") }; OutlinedButton({ onPage("medication") }) { Text("Medication") } }
}

@Composable fun VitalUnavailable(title: String, description: String, action: String, onClick: () -> Unit) {
    VitalCard { VitalIcon(R.drawable.vd_ic_info); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(description); VitalButton(action, onClick) }
}
@Composable fun VitalCare(onPage: (String) -> Unit) {
    VitalTitle("Care, on your terms", "YOUR HEALTH SPACE")
    VitalCard(VitalLilac) { VitalIcon(R.drawable.vd_ic_shield); Text("Your information. Your choice.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Keep personal notes and prescribed instructions privately on this phone.") }
    VitalButton("Report notes", { onPage("reports") }, Modifier.fillMaxWidth())
    VitalButton("Medication", { onPage("medication") }, Modifier.fillMaxWidth())
    VitalUnavailable("Find a professional", "No verified clinician service is connected yet. Booking, payments and consultations are unavailable; no appointment will be created.", "Manage my information") { onPage("profile") }
    VitalUnavailable("Pharmacy access", "No pharmacy partner is connected. Contact your own pharmacist for availability and prescribed guidance. This app cannot confirm stock or dispense medicine.", "My medication list") { onPage("medication") }
    VitalCard(VitalAmber) { Text("Need urgent help?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("If symptoms feel severe or urgent, contact your local emergency service or nearest emergency department now. Do not wait for this app, AI or a booking. VitalDay is not an emergency monitoring service.") }
}
