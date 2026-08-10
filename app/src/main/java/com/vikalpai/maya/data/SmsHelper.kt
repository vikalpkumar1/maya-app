package com.vikalpai.maya.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

class SmsHelper(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    /** Returns up to [limit] unread messages as "Sender: message" lines, newest first. */
    fun getUnreadMessages(limit: Int = 5): List<String> {
        if (!hasPermission()) return emptyList()

        val results = mutableListOf<String>()
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY)
        val selection = "${Telephony.Sms.READ} = 0"
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit"

        context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            while (cursor.moveToNext()) {
                val sender = if (addressIdx >= 0) cursor.getString(addressIdx) else "Unknown"
                val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) else ""
                results.add("$sender: $body")
            }
        }
        return results
    }
}
