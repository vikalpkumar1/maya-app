package com.vikalpai.maya.data

import android.content.Context

class PermissionsManager(context: Context) {
    private val prefs = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    companion object {
        private const val KEY_ONBOARDING_DONE = "permissions_onboarding_done"
    }
}
