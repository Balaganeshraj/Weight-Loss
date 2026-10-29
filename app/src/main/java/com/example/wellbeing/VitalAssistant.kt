@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.wellbeing

import android.content.res.Configuration
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

/** Reserved inventory, not advertisements: no network, impression tracking, or invented sponsors. */
@Composable fun PromotionBanner(slot: Int, tint: Color, modifier: Modifier = Modifier) {
    val largeText = LocalConfiguration.current.fontScale > 1.3f
    Column(modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.promo_slot, slot), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = .6f)))), contentAlignment = Alignment.Center) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-24).dp).size(112.dp).background(Color.White.copy(alpha = .25f), CircleShape))
            Box(Modifier.align(Alignment.BottomStart).offset(x = (-20).dp, y = 35.dp).size(100.dp).background(Color.White.copy(alpha = .25f), CircleShape))
            if (!largeText) Text(stringResource(R.string.promo_title), Modifier.padding(18.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = VitalInk)
        }
        if (largeText) Text(stringResource(R.string.promo_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.promo_preview), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun PromotionPair() {
    Text(stringResource(R.string.promo_partners), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 440.dp && LocalConfiguration.current.fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PromotionBanner(2, VitalLilac, Modifier.weight(1f))
                PromotionBanner(3, VitalAmber, Modifier.weight(1f))
            }
        } else Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PromotionBanner(2, VitalLilac)
            PromotionBanner(3, VitalAmber)
        }
    }
}

@Composable fun AssistantInvitation(open: (String) -> Unit) {
    VitalCard {
        Text(stringResource(R.string.assistant_demo), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.assistant_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.assistant_intro))
        VitalButton(stringResource(R.string.assistant_type), { open("coach") }, Modifier.fillMaxWidth())
        OutlinedButton({ open("coach_voice") }, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) { Text(stringResource(R.string.assistant_talk)) }
    }
}

private fun AssistantDemo.Topic.replyId(): Int = when (this) {
    AssistantDemo.Topic.PLAN -> R.string.assistant_plan_reply
    AssistantDemo.Topic.SLEEP -> R.string.assistant_sleep_reply
    AssistantDemo.Topic.FOOD -> R.string.assistant_food_reply
    AssistantDemo.Topic.MOVE -> R.string.assistant_move_reply
    AssistantDemo.Topic.MEDICAL -> R.string.assistant_medical_reply
    AssistantDemo.Topic.URGENT -> R.string.assistant_urgent_reply
    AssistantDemo.Topic.GENERAL -> R.string.assistant_general_reply
}
private data class DemoExchange(val question: String, val reply: String)

@Composable fun VitalAssistant(startWithVoice: Boolean, onPlan: () -> Unit) {
    val context = LocalContext.current
    var language by rememberSaveable { mutableStateOf(Language.tag(context)) }
    var voiceMode by rememberSaveable(startWithVoice) { mutableStateOf(startWithVoice) }
    // Transcripts intentionally stay in memory, not saved state, disk, analytics or the cloud.
    var draft by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<DemoExchange>() }
    var selected by remember { mutableStateOf(AssistantDemo.Topic.PLAN) }
    val configuration = LocalConfiguration.current
    val localized = remember(language, context, configuration) {
        context.createConfigurationContext(Configuration(configuration).apply { setLocale(Locale.forLanguageTag(language)) })
    }
    fun label(id: Int) = localized.getString(id)
    fun send(question: String, topic: AssistantDemo.Topic = AssistantDemo.topic(question)) {
        if (question.isBlank()) return
        if (history.size >= 8) history.removeAt(0)
        history.add(DemoExchange(question.trim(), label(topic.replyId())))
        draft = ""
    }
    Text(label(R.string.assistant_demo), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(label(R.string.assistant_name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(label(R.string.assistant_disclosure), style = MaterialTheme.typography.bodyMedium)
    Text(label(R.string.assistant_languages), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("en" to "English", "ta" to "தமிழ்").forEach { (tag, title) ->
            FilterChip(selected = language == tag, onClick = { language = tag }, label = { Text(title) })
        }
    }
    Text(label(R.string.assistant_language_note), style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(selected = !voiceMode, onClick = { voiceMode = false }, label = { Text(label(R.string.assistant_text_tab)) })
        FilterChip(selected = voiceMode, onClick = { voiceMode = true }, label = { Text(label(R.string.assistant_voice_tab)) })
    }
    val topics = listOf(AssistantDemo.Topic.PLAN to R.string.assistant_plan_topic, AssistantDemo.Topic.SLEEP to R.string.assistant_sleep_topic, AssistantDemo.Topic.FOOD to R.string.assistant_food_topic, AssistantDemo.Topic.MOVE to R.string.assistant_move_topic)
    if (voiceMode) {
        VitalCard(VitalLilac) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(112.dp).background(Brush.radialGradient(listOf(VitalTeal, VitalLime, VitalLilac)), CircleShape), contentAlignment = Alignment.Center) {
                    VitalIcon(R.drawable.vd_ic_voice, description = null)
                }
            }
            Text(label(R.string.assistant_voice_ready), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label(R.string.assistant_voice_hint))
        }
        Text(label(R.string.assistant_voice_disclosure))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { (topic, title) -> FilterChip(selected == topic, { selected = topic }, label = { Text(label(title)) }) }
        }
        VitalCard { Text(label(selected.replyId())) }
        OfflineDemoSpeech(language, label(selected.replyId()), ::label)
    } else {
        VitalCard(VitalTeal) { Text(label(R.string.assistant_greeting)) }
        history.forEach { exchange ->
            VitalCard {
                Text(label(R.string.assistant_you), style = MaterialTheme.typography.labelSmall)
                Text(exchange.question)
                HorizontalDivider()
                Text(label(R.string.assistant_reply), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(exchange.reply)
            }
        }
        OutlinedTextField(draft, { if (it.length <= 1000) draft = it }, label = { Text(label(R.string.assistant_input)) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
        VitalButton(label(R.string.assistant_send), { send(draft) }, Modifier.fillMaxWidth(), enabled = draft.isNotBlank())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { (topic, title) -> SuggestionChip({ send(label(title), topic) }, label = { Text(label(title)) }) }
        }
        if (history.isNotEmpty() || draft.isNotEmpty()) TextButton({ history.clear(); draft = "" }) { Text(label(R.string.assistant_clear)) }
    }
    OutlinedButton(onPlan, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label(R.string.assistant_open_plan)) }
}

@Composable private fun OfflineDemoSpeech(language: String, reply: String, label: (Int) -> String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    DisposableEffect(context) {
        var active = true
        val tts = TextToSpeech(context.applicationContext) { result ->
            if (active) { initialized = result == TextToSpeech.SUCCESS; loading = false }
        }
        engine = tts
        onDispose { active = false; tts.stop(); tts.shutdown(); engine = null }
    }
    LaunchedEffect(initialized, language, reply) {
        engine?.stop()
        failed = false
        ready = false
        if (initialized) {
            // Never request a network voice or send a user-entered message to TTS.
            val offlineVoice = engine?.voices?.firstOrNull { it.locale.language == language && !it.isNetworkConnectionRequired && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            ready = offlineVoice != null && engine?.setVoice(offlineVoice) == TextToSpeech.SUCCESS
        }
    }
    DisposableEffect(lifecycle, engine) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) engine?.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (loading) Text(label(R.string.assistant_voice_loading))
    else if (!ready) Text(label(R.string.assistant_voice_missing))
    if (failed) Text(label(R.string.assistant_voice_error), color = MaterialTheme.colorScheme.error)
    VitalButton(label(R.string.assistant_play), {
        failed = engine?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "vitalday-demo") != TextToSpeech.SUCCESS
    }, Modifier.fillMaxWidth(), enabled = ready)
    OutlinedButton({ engine?.stop() }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = ready) { Text(label(R.string.assistant_stop)) }
}
