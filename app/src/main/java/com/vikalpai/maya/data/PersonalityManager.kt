package com.vikalpai.maya.data

import android.content.Context

/**
 * TARS-style adjustable personality — Humor % and Formality %, set from
 * the in-app Settings dialog and fed into GeminiService's system prompt.
 */
class PersonalityManager(context: Context) {
    private val prefs = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)

    var humor: Int
        get() = prefs.getInt(KEY_HUMOR, 50)
        set(value) = prefs.edit().putInt(KEY_HUMOR, value).apply()

    var formality: Int
        get() = prefs.getInt(KEY_FORMALITY, 40)
        set(value) = prefs.edit().putInt(KEY_FORMALITY, value).apply()

    companion object {
        private const val KEY_HUMOR = "personality_humor"
        private const val KEY_FORMALITY = "personality_formality"
    }
}
