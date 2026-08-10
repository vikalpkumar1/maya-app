package com.vikalpai.maya.data

import android.content.Context

class AppLockManager(context: Context) {
    private val prefs = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)

    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    companion object {
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"
    }
}
