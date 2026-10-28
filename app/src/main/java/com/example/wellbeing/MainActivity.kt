@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.wellbeing

import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Forest = Color(0xFF164F40)
private val Lime = Color(0xFFD9E9A4)
private val Cream = Color(0xFFF7F8F2)
private val lightColors = lightColorScheme(primary = Forest, onPrimary = Color.White, primaryContainer = Color(0xFFE3EDDE), onPrimaryContainer = Forest, secondary = Color(0xFF596543), secondaryContainer = Lime, surface = Cream, background = Cream, surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFEEF1E9), outlineVariant = Color(0xFFDCE3D8))
private val darkColors = darkColorScheme(primary = Lime, onPrimary = Forest, primaryContainer = Forest, onPrimaryContainer = Lime, surface = Color(0xFF121C17), background = Color(0xFF121C17), surfaceContainer = Color(0xFF1C2921), surfaceContainerHigh = Color(0xFF28372D))
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Language.apply(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: WellbeingViewModel = viewModel()
            val dark by vm.dark.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = if (dark) darkColors else lightColors, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp))) {
                Surface(Modifier.fillMaxSize()) { WellbeingApp(vm) }
            }
        }
    }
}
private fun number(value: Double) = String.format(Locale.getDefault(), if (value % 1.0 == 0.0) "%.0f" else "%.1f", value)
private fun decimal(text: String) = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private data class Destination(val title: Int, val icon: ImageVector)
private val destinations = listOf(Destination(R.string.home, Icons.Outlined.Home), Destination(R.string.food, Icons.Outlined.Restaurant), Destination(R.string.move, Icons.AutoMirrored.Outlined.DirectionsWalk), Destination(R.string.insights, Icons.Outlined.BarChart), Destination(R.string.profile, Icons.Outlined.PersonOutline))

@Composable fun WellbeingApp(vm: WellbeingViewModel) {
    val ctx = LocalContext.current

    val entries by vm.entries.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dayString by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val day = LocalDate.parse(dayString)
    var form by remember { mutableStateOf<Kind?>(null) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var datePicker by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(error, loaded) { if (loaded) error?.let { snack.showSnackbar(ctx.getString(it)); vm.error.value = null } }
    if (!loaded) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (error == null) CircularProgressIndicator() else { Text(ctx.getString(R.string.load_error), style = MaterialTheme.typography.headlineSmall); Text(ctx.getString(error!!)) }
        }
        return
    }
    val profile = vm.profile()
    if (profile == null) { Box(Modifier.fillMaxSize()) { Onboarding(busy, onSave = vm::saveProfile); SnackbarHost(snack, Modifier.align(Alignment.BottomCenter)) }; return }
    val daily = entries.filter { WellnessMath.onDate(it, day, ZoneId.systemDefault()) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = { if (!wide) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) { destinations.forEachIndexed { index, d -> NavigationBarItem(tab == index, { tab = index }, icon = { Icon(d.icon, ctx.getString(d.title)) }, label = { Text(ctx.getString(d.title)) }) } } }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide) NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxHeight()) { Spacer(Modifier.height(28.dp)); destinations.forEachIndexed { index, d -> NavigationRailItem(tab == index, { tab = index }, icon = { Icon(d.icon, ctx.getString(d.title)) }, label = { Text(ctx.getString(d.title)) }); Spacer(Modifier.height(12.dp)) } }
                Column(Modifier.weight(1f).verticalScroll(remember(tab) { androidx.compose.foundation.ScrollState(0) }).padding(horizontal = if (wide) 32.dp else 20.dp).widthIn(max = 960.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Forest) { Icon(Icons.Outlined.Spa, null, tint = Lime, modifier = Modifier.padding(10.dp).size(24.dp)) }
                        Spacer(Modifier.width(10.dp)); Text(ctx.getString(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f)); SuggestionChip(onClick = { tab = 4 }, label = { Text(ctx.getString(R.string.on_device)) }, icon = { Icon(Icons.Outlined.Shield, null, Modifier.size(16.dp)) })
                    }
                    if (tab < 2) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { dayString = day.minusDays(1).toString() }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, ctx.getString(R.string.previous_day)) }
                        TextButton(onClick = { datePicker = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(if (day == LocalDate.now()) ctx.getString(R.string.today, day.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))) else day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))) }
                        IconButton(onClick = { dayString = day.plusDays(1).toString() }, enabled = day < LocalDate.now()) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, ctx.getString(R.string.next_day)) }
                    }
                    when (tab) {
                        0 -> HomeScreen(daily, profile, busy, day, onAdd = { form = it }, onFood = { tab = 1 }, onMood = { score -> vm.save(Entry(id = "mood-$day", kind = Kind.MOOD, at = timestamp(day), amount = score.toDouble())) }, onWater = { vm.save(Entry(kind = Kind.WATER, at = timestamp(day), amount = 250.0, title = ctx.getString(R.string.water))) }, onEdit = { editing = it; form = it.kind })
                        1 -> FoodScreen(daily, profile, onAdd = { form = Kind.FOOD }, onEdit = { editing = it; form = Kind.FOOD })
                        2 -> MoveScreen(vm, entries, busy, onAdd = { form = Kind.ACTIVITY }, onEdit = { editing = it; form = it.kind })
                        3 -> InsightsScreen(entries, profile, onAdd = { form = Kind.WEIGHT }, onEdit = { editing = it; form = it.kind })
                        4 -> ProfileScreen(vm, profile, busy, snack)
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
    form?.let { kind -> EntryDialog(kind, editing, if (tab < 2) day else LocalDate.now(), busy, onDismiss = { form = null; editing = null }, onSave = { vm.save(it); form = null; editing = null }, onDelete = { vm.delete(it); form = null; editing = null }) }
    if (datePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() })
        DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dayString = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; datePicker = false }) { Text(ctx.getString(R.string.choose_date)) } }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text(ctx.getString(R.string.cancel)) } }) { DatePicker(state) }
    }
}
private fun timestamp(day: LocalDate): Long = if (day == LocalDate.now()) System.currentTimeMillis() else day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable private fun Heading(title: String, subtitle: String) {
    val ctx = LocalContext.current
 Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp); Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val ctx = LocalContext.current
 Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) } }
@Composable private fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    val ctx = LocalContext.current
 Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); if (action != null) TextButton(onClick = onAction) { Text(action) } } }
@Composable private fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ctx = LocalContext.current
 FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp)) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(label) } }
@Composable private fun EmptyState(icon: ImageVector, title: String, detail: String) {
    val ctx = LocalContext.current
 Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Text(title, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun HomeScreen(entries: List<Entry>, profile: Profile, busy: Boolean, day: LocalDate, onAdd: (Kind) -> Unit, onFood: () -> Unit, onMood: (Int) -> Unit, onWater: () -> Unit, onEdit: (Entry) -> Unit) {
    val ctx = LocalContext.current

    Heading(ctx.getString(R.string.home_title), ctx.getString(R.string.home_subtitle))
    val foods = entries.filter { it.kind == Kind.FOOD }
    Surface(color = Forest, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Eco, null, tint = Lime); Spacer(Modifier.width(8.dp)); Text(ctx.getString(R.string.daily_nourishment), color = Lime, style = MaterialTheme.typography.labelLarge) }
            Text(if (foods.isEmpty()) ctx.getString(R.string.fresh_page) else ctx.getString(R.string.kcal_value, number(foods.sumOf { it.energy })), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text(if (foods.isEmpty()) ctx.getString(R.string.no_food) else ctx.getString(R.string.estimated_total), color = Color(0xFFE0EADB))
            Button(onClick = onFood, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Forest), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) { Text(ctx.getString(R.string.log_meal)); Spacer(Modifier.width(10.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp)) }
        }
    }
    BoxWithConstraints {
        val cell = if (maxWidth < 340.dp) maxWidth else (maxWidth - 12.dp) / 2
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.width(cell)) {
                Icon(Icons.Outlined.WaterDrop, null, tint = Color(0xFF3A7185))
                Text(ctx.getString(R.string.hydration), fontWeight = FontWeight.SemiBold)
                val water = entries.filter { it.kind == Kind.WATER }
                Text(if (water.isEmpty()) ctx.getString(R.string.not_logged) else ctx.getString(R.string.ml_value, number(water.sumOf { it.amount })), style = MaterialTheme.typography.titleLarge)
                Text(if (profile.fluidRestriction) ctx.getString(R.string.fluid_plan) else ctx.getString(R.string.sip_prompt), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onWater, enabled = !busy) { Text(ctx.getString(R.string.water_quick)) }
                TextButton(onClick = { onAdd(Kind.WATER) }) { Text(ctx.getString(R.string.custom_amount)) }
            }
            Panel(Modifier.width(cell)) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null, tint = MaterialTheme.colorScheme.primary)
                Text(ctx.getString(R.string.movement), fontWeight = FontWeight.SemiBold)
                val activities = entries.filter { it.kind == Kind.ACTIVITY }
                Text(if (activities.isEmpty()) ctx.getString(R.string.not_logged) else ctx.getString(R.string.minutes_value, number(activities.sumOf { it.amount })), style = MaterialTheme.typography.titleLarge)
                Text(ctx.getString(R.string.movement_prompt), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onAdd(Kind.ACTIVITY) }) { Text(ctx.getString(R.string.add_movement)) }
            }
        }
    }
    Panel {
        Text(ctx.getString(R.string.mood_question), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(ctx.getString(R.string.mood_hint), style = MaterialTheme.typography.bodyMedium)
        val selected = entries.firstOrNull { it.kind == Kind.MOOD }?.amount?.toInt()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(ctx.getString(R.string.mood_low), ctx.getString(R.string.mood_uneasy), ctx.getString(R.string.mood_okay), ctx.getString(R.string.mood_good), ctx.getString(R.string.mood_great)).forEachIndexed { index, label -> FilterChip(selected = selected == index + 1, onClick = { onMood(index + 1) }, label = { Text(label) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) } }
    }
    SectionTitle(ctx.getString(R.string.journal))
    val journal = entries.filter { it.kind in listOf(Kind.FOOD, Kind.WATER, Kind.ACTIVITY, Kind.WEIGHT) }.sortedByDescending { it.at }
    if (journal.isEmpty()) Panel { EmptyState(Icons.Outlined.EditNote, ctx.getString(R.string.nothing_logged), ctx.getString(R.string.entries_hint)) }
    else Panel { journal.forEach { LogRow(it, onClick = { onEdit(it) }) } }
    Text(ctx.getString(R.string.health_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun LogRow(entry: Entry, onClick: () -> Unit) {
    val ctx = LocalContext.current

    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val icon = when (entry.kind) { Kind.FOOD -> Icons.Outlined.Restaurant; Kind.WATER -> Icons.Outlined.WaterDrop; Kind.WEIGHT -> Icons.Outlined.MonitorWeight; else -> Icons.AutoMirrored.Outlined.DirectionsWalk }
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(icon, null, Modifier.padding(12.dp).size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entryTitle(ctx, entry), fontWeight = FontWeight.Medium)
            Text(when (entry.kind) { Kind.FOOD -> ctx.getString(R.string.food_row, number(entry.amount), number(entry.energy)); Kind.WATER -> ctx.getString(R.string.ml_value, number(entry.amount)); Kind.WEIGHT -> ctx.getString(R.string.weight_value, number(entry.amount)); else -> ctx.getString(R.string.minutes_value, number(entry.amount)) + " · " + label(ctx, entry.note) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.Edit, ctx.getString(R.string.edit, entry.title), Modifier.size(18.dp))
    }
}

@Composable private fun FoodScreen(entries: List<Entry>, profile: Profile, onAdd: () -> Unit, onEdit: (Entry) -> Unit) {
    val ctx = LocalContext.current
    var recipesMode by rememberSaveable { mutableStateOf(false) }
    var cuisine by rememberSaveable { mutableStateOf("All") }
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    Heading(ctx.getString(R.string.food_title), ctx.getString(R.string.food_subtitle))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!recipesMode, { recipesMode = false }, { Text(ctx.getString(R.string.food_journal)) }, modifier = Modifier.heightIn(min = 48.dp))
        FilterChip(recipesMode, { recipesMode = true }, { Text(ctx.getString(R.string.recipe_ideas)) }, modifier = Modifier.heightIn(min = 48.dp))
    }
    if (!recipesMode) {
        ActionButton(ctx.getString(R.string.add_food), Icons.Outlined.Add, onAdd, Modifier.fillMaxWidth())
        Panel {
            val foods = entries.filter { it.kind == Kind.FOOD }
            if (foods.isEmpty()) EmptyState(Icons.Outlined.Restaurant, ctx.getString(R.string.food_empty), ctx.getString(R.string.food_empty_hint))
            else foods.forEach { entry -> LogRow(entry) { onEdit(entry) }; EntryMacros(entry) }
        }
        Panel { Text(ctx.getString(R.string.food_control)); Text(ctx.getString(R.string.reference_disclaimer)); Text(ctx.getString(R.string.future_features), style = MaterialTheme.typography.bodySmall) }
    } else {
        Text(ctx.getString(R.string.recipe_note), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Sri Lankan", "South Indian", "International").forEach { key -> FilterChip(cuisine == key, { cuisine = key }, { Text(label(ctx, key)) }, modifier = Modifier.heightIn(min = 48.dp)) } }
        val available = recipes.filter { (cuisine == "All" || it.cuisine == cuisine) && it.allergens.intersect(profile.allergens).isEmpty() }
        if (available.isEmpty()) Panel { Text(ctx.getString(R.string.no_recipes)) }
        available.forEach { item ->
            Surface(onClick = { recipe = item }, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text(item.emoji, Modifier.padding(20.dp), fontSize = 36.sp) }
                    Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(ctx.getString(recipeTitle(item)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(label(ctx, item.cuisine)); Text(ctx.getString(R.string.recipe_time, item.minutes), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
    recipe?.let { item -> AlertDialog(onDismissRequest = { recipe = null }, confirmButton = { TextButton(onClick = { recipe = null }) { Text(ctx.getString(R.string.done)) } }, title = { Text(ctx.getString(recipeTitle(item))) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ctx.getString(when(recipes.indexOf(item)) { 0 -> R.string.recipe_dhal_steps; 1 -> R.string.recipe_salad_steps; else -> R.string.recipe_oats_steps }))
            Text(ctx.getString(R.string.recipe_note), style = MaterialTheme.typography.bodySmall)
        }
    }) }
}
private fun recipeTitle(item: Recipe) = when(recipes.indexOf(item)) { 0 -> R.string.recipe_dhal; 1 -> R.string.recipe_salad; else -> R.string.recipe_oats }

@Composable private fun MoveScreen(vm: WellbeingViewModel, entries: List<Entry>, busy: Boolean, onAdd: () -> Unit, onEdit: (Entry) -> Unit) {
    val ctx = LocalContext.current

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(500) } }
    val timer = vm.timer()
    val elapsed = timer.elapsed(now, vm.boot)
    val running = timer.startedElapsed != null && timer.boot == vm.boot
    var activity by rememberSaveable { mutableStateOf("Walk") }
    var intensity by rememberSaveable { mutableStateOf("Comfortable") }
    var discard by remember { mutableStateOf(false) }
    Heading(ctx.getString(R.string.move_title), ctx.getString(R.string.move_subtitle))
    Panel {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Walk", "Workout", "Stretch").forEach { FilterChip(activity == it, { activity = it }, { Text(label(ctx, it)) }, modifier = Modifier.heightIn(min = 48.dp)) } }
        Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(230.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Outlined.Spa, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(16.dp)); Text(String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsed / 3600000, elapsed / 60000 % 60, elapsed / 1000 % 60), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text(if (running) ctx.getString(R.string.move_subtitle) else ctx.getString(R.string.start_session), style = MaterialTheme.typography.bodySmall) }
            }
        }
        ActionButton(if (running) ctx.getString(R.string.pause) else if (elapsed > 0) ctx.getString(R.string.resume) else ctx.getString(R.string.start_session), if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, { vm.updateTimer(if (running) timer.pause(now, vm.boot) else TimerState(elapsed, now, vm.boot)) }, Modifier.fillMaxWidth(), !busy)
        if (elapsed > 0 || timer.startedElapsed != null) {
            Text(ctx.getString(R.string.how_felt), fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Easy", "Comfortable", "Challenging").forEach { FilterChip(intensity == it, { intensity = it }, { Text(label(ctx, it)) }, modifier = Modifier.heightIn(min = 48.dp)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = { vm.finishTimer(activity, intensity) }, enabled = !busy && elapsed >= 1000, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(ctx.getString(R.string.save_session)) }; TextButton(onClick = { discard = true }, enabled = !busy) { Text(ctx.getString(R.string.discard)) } }
        }
        Text(ctx.getString(R.string.timer_hint), style = MaterialTheme.typography.bodySmall)
    }
    SectionTitle(ctx.getString(R.string.recent_movement), ctx.getString(R.string.add_manually), onAdd)
    val history = entries.filter { it.kind == Kind.ACTIVITY }.sortedByDescending { it.at }.take(20)
    Panel { if (history.isEmpty()) EmptyState(Icons.AutoMirrored.Outlined.DirectionsWalk, ctx.getString(R.string.move_title), ctx.getString(R.string.entries_hint)) else history.forEach { Text(Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate().toString(), style = MaterialTheme.typography.labelSmall); LogRow(it) { onEdit(it) } } }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(ctx.getString(R.string.discard_question)) }, text = { Text(ctx.getString(R.string.discard_hint)) }, confirmButton = { TextButton(onClick = { vm.delete("timer"); discard = false }) { Text(ctx.getString(R.string.discard)) } }, dismissButton = { TextButton(onClick = { discard = false }) { Text(ctx.getString(R.string.keep_session)) } })
}

@Composable private fun InsightsScreen(entries: List<Entry>, profile: Profile, onAdd: () -> Unit, onEdit: (Entry) -> Unit) {
    val ctx = LocalContext.current

    Heading(ctx.getString(R.string.insights_title), ctx.getString(R.string.insights_subtitle))
    val weights = entries.filter { it.kind == Kind.WEIGHT }.sortedBy { it.at }
    Panel {
        SectionTitle(ctx.getString(R.string.measurements), ctx.getString(R.string.add), onAdd)
        if (weights.isEmpty()) EmptyState(Icons.Outlined.MonitorWeight, ctx.getString(R.string.measurements), ctx.getString(R.string.optional_measurements)) else {
            Text(ctx.getString(R.string.weight_value, number(weights.last().amount)), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text(ctx.getString(R.string.weight), style = MaterialTheme.typography.bodySmall)
            if (weights.size >= 2) {
                val points = weights.takeLast(12)
                val color = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxWidth().height(130.dp).semantics { contentDescription = ctx.getString(R.string.measurement_history) + " " + points.joinToString { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate().toString() + ": " + ctx.getString(R.string.weight_value, number(it.amount)) } }) {
                    val low = points.minOf { it.amount } - 1
                    val high = points.maxOf { it.amount } + 1
                    val path = Path()
                    points.forEachIndexed { i, entry -> val x = 8f + i * (size.width - 16) / (points.size - 1); val y = size.height - 8 - ((entry.amount - low) / (high - low) * (size.height - 16)).toFloat(); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y); drawCircle(color, 5.dp.toPx(), Offset(x, y)) }
                    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                }
            }
            if (profile.age >= 20 && !profile.bmiExcluded && profile.heightCm != null) Text(ctx.getString(R.string.bmi_value, number(WellnessMath.bmi(weights.last().amount, profile.heightCm))), style = MaterialTheme.typography.bodySmall)
            else Text(ctx.getString(R.string.bmi_hidden), style = MaterialTheme.typography.bodySmall)
        }
    }
    Panel {
        Text(ctx.getString(R.string.last_seven_days), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        days.forEach { date ->
            val daily = entries.filter { WellnessMath.onDate(it, date, ZoneId.systemDefault()) }
            val minutes = daily.filter { it.kind == Kind.ACTIVITY }.sumOf { it.amount }
            val water = daily.filter { it.kind == Kind.WATER }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(date.format(DateTimeFormatter.ofPattern("EEE d")), Modifier.width(70.dp), style = MaterialTheme.typography.labelLarge); Column(Modifier.weight(1f)) { Text(if (daily.none { it.kind == Kind.ACTIVITY }) ctx.getString(R.string.movement_missing) else ctx.getString(R.string.minutes_value, number(minutes)), style = MaterialTheme.typography.bodyMedium); Text(if (water.isEmpty()) ctx.getString(R.string.water_missing) else ctx.getString(R.string.ml_value, number(water.sumOf { it.amount })), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            if (date != days.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    if (weights.isNotEmpty()) { SectionTitle(ctx.getString(R.string.measurement_history)); Panel { weights.reversed().forEach { Text(Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate().toString(), style = MaterialTheme.typography.labelSmall); LogRow(it) { onEdit(it) } } } }
}

@Composable private fun Onboarding(busy: Boolean, onSave: (Profile) -> Unit) {
    val ctx = LocalContext.current

    var age by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }
    val valid = age.toIntOrNull() in 18..120
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(28.dp), color = Forest) { Icon(Icons.Outlined.Spa, null, Modifier.padding(28.dp).size(52.dp), tint = Lime) }
        Text(ctx.getString(R.string.welcome_title), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        LanguagePicker()
        Text(ctx.getString(R.string.welcome), style = MaterialTheme.typography.titleLarge)
        Text(ctx.getString(R.string.welcome_hint), style = MaterialTheme.typography.bodyLarge)
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Shield, null); Spacer(Modifier.width(12.dp)); Text(ctx.getString(R.string.privacy_title), fontWeight = FontWeight.Bold) }
            Text(ctx.getString(R.string.privacy_summary))
            Text(ctx.getString(R.string.health_disclaimer), style = MaterialTheme.typography.bodyMedium)
            Text(ctx.getString(R.string.reference_disclaimer), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(age, { age = it.take(3) }, label = { Text(ctx.getString(R.string.age)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), isError = tried && !valid, supportingText = { Text(if (tried && !valid) ctx.getString(R.string.age_hint) else ctx.getString(R.string.age_hint)) })
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { agreed = !agreed }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(agreed, { agreed = it }); Text(ctx.getString(R.string.privacy_agree), Modifier.weight(1f)) }
        Button(onClick = { tried = true; if (valid && agreed) onSave(Profile(age = age.toInt())) }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(ctx.getString(R.string.start_journal)); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null) }
        if (tried && !agreed) Text(ctx.getString(R.string.onboarding_error), color = MaterialTheme.colorScheme.error)
        Text(ctx.getString(R.string.optional_measurements), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun EntryDialog(kind: Kind, existing: Entry?, date: LocalDate, busy: Boolean, onDismiss: () -> Unit, onSave: (Entry) -> Unit, onDelete: (String) -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember { NutritionCatalog.load(ctx) }
    var title by remember { mutableStateOf(existing?.let { entryTitle(ctx, it) } ?: when(kind) { Kind.WATER -> ctx.getString(R.string.water); Kind.WEIGHT -> ctx.getString(R.string.weight); Kind.ACTIVITY -> ctx.getString(R.string.walk); else -> "" }) }
    var amount by remember { mutableStateOf(existing?.amount?.let(::number) ?: when(kind) { Kind.FOOD -> "100"; Kind.WATER -> "250"; Kind.ACTIVITY -> "20"; else -> "" }) }
    var energyPer100 by remember { mutableStateOf(existing?.let { (it.energy * 100 / it.amount).toString() } ?: "") }
    var unit by remember { mutableStateOf("kcal") }
    var source by remember { mutableStateOf(existing?.source ?: ctx.getString(R.string.package_label)) }
    var selected by remember { mutableStateOf(catalog.find { it.id == existing?.source && it.version == existing.sourceVersion }) }
    var note by remember { mutableStateOf(existing?.note ?: "Comfortable") }
    var query by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val grams = decimal(amount)
    val kcal100 = decimal(energyPer100)?.let { if(unit == "kJ") it / 4.184 else it }
    val valid = when(kind) {
        Kind.FOOD -> title.isNotBlank() && source.isNotBlank() && grams != null && grams in 1.0..3000.0 && kcal100 != null && kcal100 in 0.0..1000.0
        Kind.WATER -> grams != null && grams in 1.0..3000.0
        Kind.ACTIVITY -> title.isNotBlank() && grams != null && grams in 0.016..1440.0
        Kind.WEIGHT -> grams != null && grams in 20.0..500.0
        else -> false
    }
    fun manual() { selected = null; source = ctx.getString(R.string.package_label) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(ctx.getString(if(existing == null) R.string.add_entry else R.string.edit_entry)) }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ctx.getString(R.string.for_date, existing?.let { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate() } ?: date))
            if(kind == Kind.FOOD) {
                OutlinedTextField(query, { query=it }, label={ Text(ctx.getString(R.string.search_foods)) }, leadingIcon={ Icon(Icons.Outlined.Search,null) }, modifier=Modifier.fillMaxWidth(), singleLine=true)
                catalog.filter { food -> foodSearch(ctx, food, query) }.forEach { food ->
                    TextButton(onClick={ selected=food; title=ctx.getString(food.nameResource); energyPer100=food.kcal.toString(); unit="kcal"; source=food.id }, modifier=Modifier.fillMaxWidth()) {
                        Text(ctx.getString(food.nameResource),Modifier.weight(1f)); if(selected==food) Icon(Icons.Outlined.Check,null)
                    }
                }
                Text(ctx.getString(R.string.reference_disclaimer),style=MaterialTheme.typography.bodySmall)
                HorizontalDivider(); Text(ctx.getString(R.string.manual_food),fontWeight=FontWeight.SemiBold)
            }
            if(kind==Kind.FOOD || kind==Kind.ACTIVITY) OutlinedTextField(title, { title=it.take(120); if(kind==Kind.FOOD) manual() }, label={ Text(ctx.getString(if(kind==Kind.FOOD) R.string.food_name else R.string.activity_name)) },modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(amount, { amount=it.take(10) }, label={ Text(ctx.getString(when(kind) { Kind.FOOD->R.string.amount_grams;Kind.WATER->R.string.volume_ml;Kind.ACTIVITY->R.string.duration_minutes;else->R.string.weight_kg })) },keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),singleLine=true)
            if(kind==Kind.FOOD) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("kcal","kJ").forEach { candidate -> FilterChip(unit==candidate,{ if(unit!=candidate) { energyPer100=decimal(energyPer100)?.let { if(candidate=="kJ") (it*4.184).toString() else (it/4.184).toString() } ?: ""; unit=candidate } },{ Text(candidate) },modifier=Modifier.heightIn(min=48.dp)) } }
                OutlinedTextField(energyPer100,{ energyPer100=it.take(12);manual() },label={ Text(ctx.getString(R.string.energy_per100)) },keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),singleLine=true)
                OutlinedTextField(source,{ source=it.take(200);selected=null },label={ Text(ctx.getString(R.string.source_label)) },modifier=Modifier.fillMaxWidth())
                if(valid) {
                    Text(ctx.getString(R.string.portion_estimate,number(WellnessMath.portion(kcal100!!,grams!!))),fontWeight=FontWeight.Bold)
                    selected?.let { food -> MacroText(food.macros(grams)); Text(ctx.getString(R.string.source_reference,food.id,food.version),style=MaterialTheme.typography.bodySmall) } ?: Text(ctx.getString(R.string.macros_unknown),style=MaterialTheme.typography.bodySmall)
                }
                Text(ctx.getString(R.string.confirm_food),style=MaterialTheme.typography.bodySmall)
            }
            if(kind==Kind.ACTIVITY) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Easy","Comfortable","Challenging").forEach { value -> FilterChip(note==value,{note=value},{Text(label(ctx,value))},modifier=Modifier.heightIn(min=48.dp)) } }
            if(tried&&!valid) Text(ctx.getString(when(kind) {Kind.FOOD->R.string.invalid_food;Kind.WATER->R.string.invalid_water;Kind.ACTIVITY->R.string.invalid_activity;else->R.string.invalid_weight}),color=MaterialTheme.colorScheme.error)
            if(existing!=null) TextButton(onClick={deleting=true},enabled=!busy) { Text(ctx.getString(R.string.delete_entry)) }
        }
    },confirmButton={Button(onClick={
        tried=true
        if(valid) {
            val macro=selected?.macros(grams!!)
            val metadata=macro?.let { org.json.JSONObject().put("proteinG",it.protein).put("carbohydrateG",it.carbohydrate).put("fatG",it.fat).put("fiberG",it.fiber).toString() } ?: ""
            onSave(Entry(id=existing?.id ?: java.util.UUID.randomUUID().toString(),kind=kind,at=existing?.at ?: timestamp(date),title=title.trim(),amount=grams!!,energy=if(kind==Kind.FOOD) WellnessMath.portion(kcal100!!,grams) else 0.0,source=source,sourceVersion=selected?.version ?: "user-v1",note=note,extra=metadata))
        }
    },enabled=!busy){Text(ctx.getString(R.string.save_entry))}},dismissButton={TextButton(onClick=onDismiss){Text(ctx.getString(R.string.cancel))}})
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text(ctx.getString(R.string.delete_entry_question))},text={Text(ctx.getString(R.string.delete_entry_hint))},confirmButton={TextButton(onClick={existing?.let{onDelete(it.id)}}){Text(ctx.getString(R.string.delete))}},dismissButton={TextButton(onClick={deleting=false}){Text(ctx.getString(R.string.keep_entry))}})
}

@Composable private fun ProfileScreen(vm: WellbeingViewModel, profile: Profile, busy: Boolean, snack: SnackbarHostState) {
    val ctx=LocalContext.current
    val dark by vm.dark.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }
    var exporting by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null) {
            val data=vm.export()
            scope.launch {
                val ok=withContext(Dispatchers.IO) { runCatching { val stream=ctx.contentResolver.openOutputStream(uri) ?: error("Missing document stream");stream.bufferedWriter().use{it.write(data)} }.isSuccess }
                snack.showSnackbar(ctx.getString(if(ok) R.string.export_success else R.string.export_failed))
            }
        }
    }
    Heading(ctx.getString(R.string.profile_title),ctx.getString(R.string.profile_subtitle))
    Panel {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Icon(Icons.Outlined.PersonOutline,null,Modifier.padding(18.dp).size(32.dp))}
            Spacer(Modifier.width(16.dp));Column{Text(ctx.getString(R.string.offline_journal),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(ctx.getString(R.string.no_account))}
        }
        ActionButton(ctx.getString(R.string.edit_preferences),Icons.Outlined.Tune,{edit=true},Modifier.fillMaxWidth())
    }
    Panel {
        LanguagePicker()
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.DarkMode,null);Spacer(Modifier.width(12.dp));Text(ctx.getString(R.string.dark_mode),Modifier.weight(1f));Switch(dark,vm::setDark)}
    }
    ReminderSettings(vm)
    Panel {
        Text(ctx.getString(R.string.privacy_title),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        Text(ctx.getString(R.string.privacy_details))
        Text(ctx.getString(R.string.sync_status),style=MaterialTheme.typography.bodySmall)
        ActionButton(ctx.getString(R.string.export_journal),Icons.Outlined.FileDownload,{exporting=true},Modifier.fillMaxWidth(),!busy)
        OutlinedButton(onClick={deleting=true},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),enabled=!busy){Icon(Icons.Outlined.DeleteOutline,null);Spacer(Modifier.width(8.dp));Text(ctx.getString(R.string.delete_all))}
    }
    Text(ctx.getString(R.string.health_disclaimer),style=MaterialTheme.typography.bodySmall)
    Text(ctx.getString(R.string.version_label),style=MaterialTheme.typography.bodySmall)
    if(exporting) AlertDialog(onDismissRequest={exporting=false},title={Text(ctx.getString(R.string.export_question))},text={Text(ctx.getString(R.string.export_hint))},confirmButton={TextButton(onClick={exporting=false;exporter.launch("wellbeing-${LocalDate.now()}.json")}){Text(ctx.getString(R.string.choose_location))}},dismissButton={TextButton(onClick={exporting=false}){Text(ctx.getString(R.string.cancel))}})
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text(ctx.getString(R.string.delete_all_question))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text(ctx.getString(R.string.delete_all_hint));OutlinedTextField(deleteText,{deleteText=it},label={Text(ctx.getString(R.string.type_delete))},singleLine=true)}},confirmButton={TextButton(onClick={vm.clear();deleting=false},enabled=deleteText=="DELETE"&&!busy){Text(ctx.getString(R.string.delete_all_confirm))}},dismissButton={TextButton(onClick={deleting=false;deleteText=""}){Text(ctx.getString(R.string.keep_data))}})
    if(edit) ProfileDialog(profile,busy,{edit=false}){vm.saveProfile(it);edit=false}
}

@Composable private fun LanguagePicker() {
    val ctx=LocalContext.current
    Text(ctx.getString(R.string.language),fontWeight=FontWeight.SemiBold)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("en" to "English","si" to "සිංහල","ta" to "தமிழ்").forEach { (tag,name) ->
            FilterChip(Language.tag(ctx)==tag,{Language.set(ctx,tag)},{Text(name)},modifier=Modifier.heightIn(min=48.dp))
        }
    }
}

@Composable private fun ReminderSettings(vm: WellbeingViewModel) {
    val ctx=LocalContext.current
    val settings by vm.reminderPreferences.collectAsStateWithLifecycle()
    var hour by rememberSaveable(settings.hour) { mutableStateOf(settings.hour.toString()) }
    var minute by rememberSaveable(settings.minute) { mutableStateOf(settings.minute.toString().padStart(2,'0')) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    var pendingHour by rememberSaveable { mutableIntStateOf(10) }
    var pendingMinute by rememberSaveable { mutableIntStateOf(0) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if(allowed){vm.configureReminders(ReminderPreferences(true,pendingHour,pendingMinute));blocked=false}else{blocked=true;vm.configureReminders(settings.copy(enabled=false))}
    }
    val valid=hour.toIntOrNull() in 8..20 && minute.toIntOrNull() in 0..59
    Panel {
        Text(ctx.getString(R.string.reminders),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        Text(ctx.getString(R.string.reminder_hint),style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(hour,{hour=it.take(2)},label={Text(ctx.getString(R.string.reminder_hour))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f),singleLine=true)
            OutlinedTextField(minute,{minute=it.take(2)},label={Text(ctx.getString(R.string.reminder_minute))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f),singleLine=true)
        }
        if(attempted&&!valid) Text(ctx.getString(R.string.reminder_time_error),color=MaterialTheme.colorScheme.error)
        Button(onClick={
            attempted=true
            if(valid){
                pendingHour=hour.toInt();pendingMinute=minute.toInt()
                vm.reminderController.createChannel()
                if(android.os.Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(ctx,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else if(vm.reminderController.canNotify()){vm.configureReminders(ReminderPreferences(true,pendingHour,pendingMinute));blocked=false}
                else blocked=true
            }
        },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(ctx.getString(if(settings.enabled) R.string.save else R.string.enable_reminders))}
        if(settings.enabled) OutlinedButton(onClick={vm.configureReminders(settings.copy(enabled=false))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(ctx.getString(R.string.disable_reminders))}
        if(blocked || (settings.enabled&&!vm.reminderController.canNotify())) {
            Text(ctx.getString(R.string.notifications_blocked))
            TextButton(onClick={ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,ctx.packageName))}){Text(ctx.getString(R.string.open_settings))}
        }
    }
}

private val allergenKeys=listOf("Peanuts","Gluten","Milk","Eggs","Fish","Shellfish","Soy","Sesame","Tree nuts")
private val labels=mapOf("User entered" to R.string.user_entered,"Peanuts" to R.string.peanuts,"Gluten" to R.string.gluten,"Milk" to R.string.milk,"Eggs" to R.string.eggs,"Fish" to R.string.fish,"Shellfish" to R.string.shellfish,"Soy" to R.string.soy,"Sesame" to R.string.sesame,"Tree nuts" to R.string.tree_nuts,"All" to R.string.all,"Sri Lankan" to R.string.sri_lankan,"South Indian" to R.string.south_indian,"International" to R.string.international,"Walk" to R.string.walk,"Workout" to R.string.workout,"Stretch" to R.string.stretch,"Easy" to R.string.easy,"Comfortable" to R.string.comfortable,"Challenging" to R.string.challenging,"Water" to R.string.water,"Weight" to R.string.weight)
private fun label(context: android.content.Context,key:String)=labels[key]?.let{context.getString(it)} ?: key
private fun entryTitle(context: android.content.Context,entry:Entry):String = when(entry.kind) {
    Kind.WATER -> context.getString(R.string.water)
    Kind.WEIGHT -> context.getString(R.string.weight)
    Kind.FOOD -> NutritionCatalog.load(context).find{it.id==entry.source && it.version==entry.sourceVersion}?.let{context.getString(it.nameResource)} ?: entry.title
    else -> label(context,entry.title)
}
private fun foodSearch(context: android.content.Context,food:ReferenceFood,query:String):Boolean {
    if(query.isBlank())return true
    return (listOf("en","si","ta").joinToString(" "){tag -> context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply{setLocale(Locale.forLanguageTag(tag))}).getString(food.nameResource)} + food.sourceDescription).contains(query,true)
}
@Composable private fun MacroText(m:Macros) {
    val ctx=LocalContext.current
    Text(ctx.getString(R.string.macros,number(m.protein),number(m.carbohydrate),number(m.fat),number(m.fiber)),style=MaterialTheme.typography.bodySmall)
}
@Composable private fun EntryMacros(entry:Entry) {
    val ctx=LocalContext.current
    val macros=remember(entry.extra){runCatching{org.json.JSONObject(entry.extra).let{Macros(it.getDouble("proteinG"),it.getDouble("carbohydrateG"),it.getDouble("fatG"),it.getDouble("fiberG"))}}.getOrNull()}
    if(macros!=null) MacroText(macros) else Text(ctx.getString(R.string.macros_unknown),style=MaterialTheme.typography.bodySmall)
    Text(ctx.getString(R.string.source_reference,label(ctx,entry.source),entry.sourceVersion),style=MaterialTheme.typography.labelSmall)
}

@Composable private fun ProfileDialog(profile: Profile, busy: Boolean, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    val ctx = LocalContext.current

    var age by remember { mutableStateOf(profile.age.toString()) }
    var height by remember { mutableStateOf(profile.heightCm?.let(::number) ?: "") }
    var fluid by remember { mutableStateOf(profile.fluidRestriction) }
    var excluded by remember { mutableStateOf(profile.bmiExcluded) }
    var allergens by remember { mutableStateOf(profile.allergens) }
    var tried by remember { mutableStateOf(false) }
    val valid = age.toIntOrNull() in 18..120 && (height.isBlank() || decimal(height)?.let { it in 100.0..250.0 } == true)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(ctx.getString(R.string.preferences)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(age, { age = it.take(3) }, label = { Text(ctx.getString(R.string.age)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(height, { height = it.take(6) }, label = { Text(ctx.getString(R.string.height_cm)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(fluid, { fluid = it }); Text(ctx.getString(R.string.fluid_restriction)) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(excluded, { excluded = it }); Text(ctx.getString(R.string.hide_bmi)) }
            Text(ctx.getString(R.string.allergen_exclusions), fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { allergenKeys.forEach { item -> FilterChip(item in allergens, { allergens = if (item in allergens) allergens - item else allergens + item }, { Text(label(ctx, item)) }, modifier = Modifier.heightIn(min = 48.dp)) } }
            Text(ctx.getString(R.string.recipe_note), style = MaterialTheme.typography.bodySmall)
            if (tried && !valid) Text(ctx.getString(R.string.preferences_error), color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(onClick = { tried = true; if (valid) onSave(Profile(age.toInt(), decimal(height), fluid, excluded, allergens)) }, enabled = !busy) { Text(ctx.getString(R.string.save_preferences)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(ctx.getString(R.string.cancel)) } })
}
