package com.vikalpai.maya.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import com.vikalpai.maya.service.ReminderReceiver
import com.vikalpai.maya.voice.VoiceOutputManager
import org.json.JSONObject

/**
 * Runs the ACTION commands Maya's replies can contain.
 * Deliberately uses intents that don't need dangerous runtime
 * permissions where possible (ACTION_DIAL, ACTION_SENDTO, AlarmClock,
 * calendar insert, clipboard, settings, web search, WhatsApp deep link) —
 * the user always sees and confirms the final step, so nothing happens
 * silently in the background. Brightness needs a one-time special grant
 * (Settings.canWrite), contacts needs a normal runtime permission.
 */
class ActionExecutor(private val context: Context, private val voiceOutput: VoiceOutputManager) {

    private val contactsHelper = ContactsHelper(context)

    fun run(actionJson: String): String? {
        return try {
            val json = JSONObject(actionJson)
            when (json.getString("type")) {
                "open_app" -> {
                    if (openApp(json.getString("app"))) "App khol diya" else "Wo app nahi mili"
                }
                "call" -> {
                    val resolved = contactsHelper.resolveNumber(json.getString("number"))
                    when {
                        resolved != null -> { call(resolved); "Dialer khol diya" }
                        !contactsHelper.hasPermission() -> "Contacts permission chahiye — Settings (⚙) mein allow karo"
                        else -> "Wo contact nahi mila"
                    }
                }
                "sms" -> {
                    val resolved = contactsHelper.resolveNumber(json.getString("number"))
                    when {
                        resolved != null -> {
                            sendSms(resolved, json.optString("message", "")); "Message ready hai"
                        }
                        !contactsHelper.hasPermission() -> "Contacts permission chahiye — Settings (⚙) mein allow karo"
                        else -> "Wo contact nahi mila"
                    }
                }
                "whatsapp" -> {
                    val resolved = contactsHelper.resolveNumber(json.getString("number"))
                    when {
                        resolved != null -> {
                            sendWhatsApp(resolved, json.optString("message", "")); "WhatsApp ready hai"
                        }
                        !contactsHelper.hasPermission() -> "Contacts permission chahiye — Settings (⚙) mein allow karo"
                        else -> "Wo contact nahi mila"
                    }
                }
                "alarm" -> {
                    setAlarm(json.getInt("hour"), json.getInt("minute"), json.optString("label", "Maya alarm"))
                    "Alarm set kar diya"
                }
                "search" -> { openWebSearch(json.getString("query")); "Search khol diya" }
                "calendar" -> {
                    openCalendarEvent(json.getString("title"), json.optString("details", ""))
                    "Calendar event ready hai"
                }
                "copy" -> { copyToClipboard(json.getString("text")); "Copy kar diya" }
                "settings" -> { openSettings(); "Settings khol diya" }
                "volume" -> setVolume(json.optInt("percent", -1), json.optString("direction", ""))
                "brightness" -> setBrightness(json.getInt("percent"))
                "voice_speed" -> {
                    val rate = when (json.optString("speed", "normal")) {
                        "slow" -> 0.75f; "fast" -> 1.3f; else -> 1.0f
                    }
                    voiceOutput.setRate(rate)
                    "Awaaz ki speed badal di"
                }
                "voice_style" -> {
                    val pitch = when (json.optString("style", "normal")) {
                        "female" -> 1.3f; "male" -> 0.8f; else -> 1.0f
                    }
                    voiceOutput.setPitch(pitch)
                    "Awaaz badal di"
                }
                "weather" -> weatherHelper.getWeather(json.getString("city"))
                "read_sms" -> {
                    if (!smsHelper.hasPermission()) {
                        "SMS permission chahiye — Settings (⚙) mein allow karo"
                    } else {
                        val messages = smsHelper.getUnreadMessages()
                        if (messages.isEmpty()) "Koi naya unread message nahi hai"
                        else messages.joinToString("\n")
                    }
                }
                "memory_stats" -> systemStatsHelper.getMemorySummary()
                "open_url" -> { openUrl(json.getString("url")); "Webpage khol diya" }
                "bluetooth" -> { openBluetoothPanel(); "Bluetooth panel khol diya" }
                "email" -> {
                    sendEmail(json.getString("to"), json.optString("subject", ""), json.optString("body", ""))
                    "Email ready hai"
                }
                "reminder" -> {
                    val minutesFromNow = json.optInt("minutes_from_now", -1)
                    if (minutesFromNow > 0) {
                        scheduleReminder(minutesFromNow, json.getString("message"))
                        "Reminder set kar diya"
                    } else "Reminder ka time samajh nahi aaya"
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private val weatherHelper = WeatherHelper()
    private val smsHelper = SmsHelper(context)
    private val systemStatsHelper = SystemStatsHelper(context)

    // Common short forms people actually say, mapped to what the app is really called.
    private val appAliases = mapOf(
        "yt" to "youtube",
        "insta" to "instagram",
        "ig" to "instagram",
        "fb" to "facebook",
        "wa" to "whatsapp"
    )

    private fun openApp(appNameRaw: String): Boolean {
        val appName = appAliases[appNameRaw.trim().lowercase()] ?: appNameRaw
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val match = apps.firstOrNull {
            val label = pm.getApplicationLabel(it).toString()
            label.contains(appName, ignoreCase = true) || appName.contains(label, ignoreCase = true)
        }
        val launchIntent = match?.let { pm.getLaunchIntentForPackage(it.packageName) }
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else false
    }

    private fun call(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun sendSms(number: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
        intent.putExtra("sms_body", message)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun sendWhatsApp(number: String, message: String) {
        val cleanNumber = number.filter { it.isDigit() }
        val uri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun setAlarm(hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openWebSearch(query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra("query", query)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openCalendarEvent(title: String, details: String) {
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, details)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Maya", text))
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun setVolume(percent: Int, direction: String): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = when {
            percent in 0..100 -> max * percent / 100
            direction == "increase" -> (current + max / 10).coerceAtMost(max)
            direction == "decrease" -> (current - max / 10).coerceAtLeast(0)
            else -> current
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return "Volume set kar diya"
    }

    private fun setBrightness(percent: Int): String {
        if (!Settings.System.canWrite(context)) {
            return "Brightness control permission chahiye — Settings (⚙) mein allow karo"
        }
        val value = (percent.coerceIn(0, 100) * 255 / 100)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        return "Brightness set kar di"
    }

    private fun openUrl(url: String) {
        val fixedUrl = if (url.startsWith("http")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openBluetoothPanel() {
        // Android 12+ no longer lets apps silently flip Bluetooth on/off —
        // this opens the quick-toggle panel, which is Google's blessed
        // replacement for programmatic control.
        val intent = Intent(Settings.Panel.ACTION_BLUETOOTH)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun sendEmail(to: String, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun scheduleReminder(minutesFromNow: Int, message: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val triggerAt = System.currentTimeMillis() + minutesFromNow * 60_000L
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
