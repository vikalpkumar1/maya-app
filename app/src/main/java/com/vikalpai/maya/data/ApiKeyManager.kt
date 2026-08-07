package com.vikalpai.maya.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Every Maya install uses the owner's own free Gemini API key
 * (from https://aistudio.google.com/apikey) — stored only in local
 * SharedPreferences on the device, never bundled into the APK or
 * committed to the repo. This keeps the whole app free for everyone
 * who builds it, with no shared/shared-cost backend to run.
 */
class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String? = prefs.getString(KEY_GEMINI_API_KEY, null)

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }
}
