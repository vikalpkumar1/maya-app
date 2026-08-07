package com.vikalpai.maya.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import org.json.JSONObject

/**
 * Runs the ACTION commands Maya's replies can contain.
 * Deliberately uses intents that don't need dangerous runtime
 * permissions (ACTION_DIAL, ACTION_SENDTO, AlarmClock, calendar insert,
 * clipboard, settings, web search) — the user always sees and confirms
 * the final step, so nothing happens silently in the background.
 */
class ActionExecutor(private val context: Context) {

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
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

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
}
