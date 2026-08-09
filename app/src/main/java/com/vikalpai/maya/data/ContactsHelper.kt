package com.vikalpai.maya.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class ContactsHelper(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Returns a phone number for a contact name, the input itself if it
     * already looks like a number, or null if nothing could be resolved
     * (either no matching contact, or contacts permission not granted).
     */
    fun resolveNumber(nameOrNumber: String): String? {
        val digitCount = nameOrNumber.count { it.isDigit() }
        if (digitCount >= 6) return nameOrNumber
        if (!hasPermission()) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%${nameOrNumber.trim()}%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return null
    }
}
