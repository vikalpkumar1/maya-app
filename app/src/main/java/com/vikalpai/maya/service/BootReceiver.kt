package com.vikalpai.maya.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.vikalpai.maya.data.HotwordPrefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (HotwordPrefs(context).enabled) {
                val serviceIntent = Intent(context, HotwordService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
