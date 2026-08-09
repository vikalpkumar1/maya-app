package com.vikalpai.maya.data

import android.app.ActivityManager
import android.content.Context

/**
 * Memory usage only — modern Android (8+) no longer lets regular apps
 * read system-wide CPU usage (that data was restricted for privacy
 * reasons), so there's no honest way to report live CPU stats here.
 */
class SystemStatsHelper(private val context: Context) {

    fun getMemorySummary(): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val totalGb = info.totalMem / (1024.0 * 1024 * 1024)
        val availGb = info.availMem / (1024.0 * 1024 * 1024)
        val usedPercent = ((info.totalMem - info.availMem) * 100 / info.totalMem)
        return "RAM: %.1f GB mein se %.1f GB free (%d%% use ho raha hai)".format(availGb, totalGb, usedPercent)
    }
}
