package com.example.wellbeing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.util.concurrent.TimeUnit

private val Context.reminderStore by preferencesDataStore("reminders")
data class ReminderPreferences(val enabled: Boolean = false, val hour: Int = 10, val minute: Int = 0)
object ReminderSchedule {
    fun next(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
        require(hour in 8..20 && minute in 0..59)
        val today = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(now.zone)
    }
    fun withinQuietHours(now: ZonedDateTime): Boolean = now.hour < 8 || now.hour >= 21
}
class ReminderController(private val context: Context) {
    companion object {
        private val mutex = Mutex()
        const val WORK = "wellbeing.daily.reminder"
        const val CHANNEL = "wellbeing.gentle.reminders"
        const val NOTIFICATION = 2001
        private val ENABLED = booleanPreferencesKey("enabled")
        private val HOUR = intPreferencesKey("hour")
        private val MINUTE = intPreferencesKey("minute")
        private val LAST_DAY = stringPreferencesKey("last_day")
    }
    val preferences = context.reminderStore.data.map { ReminderPreferences(it[ENABLED] ?: false, it[HOUR] ?: 10, it[MINUTE] ?: 0) }
    fun canNotify(): Boolean = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun createChannel() {
        val localized = Language.context(context)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, localized.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = localized.getString(R.string.reminder_channel_description)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        })
    }
    suspend fun configure(value: ReminderPreferences) = mutex.withLock {
        require(value.hour in 8..20 && value.minute in 0..59)
        context.reminderStore.edit { it[ENABLED] = value.enabled; it[HOUR] = value.hour; it[MINUTE] = value.minute }
        if (value.enabled) schedule(value, ExistingWorkPolicy.REPLACE) else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION)
        }
    }
    suspend fun disable() = configure(preferences.first().copy(enabled = false))
    suspend fun reschedule() { val value = preferences.first(); if (value.enabled) schedule(value, ExistingWorkPolicy.REPLACE) }
    private fun schedule(value: ReminderPreferences, policy: ExistingWorkPolicy, snooze: Boolean = false) {
        val now = ZonedDateTime.now()
        val target = if (snooze && !ReminderSchedule.withinQuietHours(now.plusHours(1))) now.plusHours(1) else ReminderSchedule.next(now, value.hour, value.minute)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(Duration.between(now, target).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("snooze" to snooze)).addTag(WORK).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, policy, request)
    }
    suspend fun snooze() { val value = preferences.first(); if (value.enabled) schedule(value, ExistingWorkPolicy.REPLACE, true); NotificationManagerCompat.from(context).cancel(NOTIFICATION) }
    suspend fun deliver(isSnooze: Boolean) = mutex.withLock {
        val value = preferences.first()
        if (!value.enabled) return@withLock
        createChannel()
        val now = ZonedDateTime.now()
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)
        val eligible = canNotify() && channel.importance != NotificationManager.IMPORTANCE_NONE && !ReminderSchedule.withinQuietHours(now) && (isSnooze || !now.toLocalTime().isBefore(LocalTime.of(value.hour, value.minute)))
        var claimed = false
        if (eligible) context.reminderStore.edit { prefs ->
            if (prefs[ENABLED] == true && (isSnooze || prefs[LAST_DAY] != now.toLocalDate().toString())) { prefs[LAST_DAY] = now.toLocalDate().toString(); claimed = true }
        }
        if (claimed && canNotify()) {
            val localized = Language.context(context)
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            fun action(name: String, code: Int) = PendingIntent.getBroadcast(context, code, Intent(context, ReminderActionReceiver::class.java).setAction(name), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(localized.getString(R.string.reminder_title)).setContentText(localized.getString(R.string.reminder_body))
                .setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .addAction(0, localized.getString(R.string.snooze), action("snooze", 1))
                .addAction(0, localized.getString(R.string.disable_reminders), action("disable", 2)).build()
            try { NotificationManagerCompat.from(context).notify(NOTIFICATION, notification) } catch (_: SecurityException) { /* Permission may be revoked between check and notify. */ }
        }
        // A successor is appended rather than cancelling this running worker.
        if (preferences.first().enabled) schedule(preferences.first(), ExistingWorkPolicy.APPEND_OR_REPLACE)
    }
}
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try { ReminderController(applicationContext).deliver(inputData.getBoolean("snooze", false)); Result.success() } catch (_: java.io.IOException) { Result.retry() }
}
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try {
            val controller = ReminderController(context.applicationContext)
            when (intent.action) {
                "disable" -> controller.disable()
                "snooze" -> controller.snooze()
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> { controller.reschedule(); EntryReminders.reschedule(context.applicationContext) }
            }
        } finally { pending.finish() } }
    }
}
