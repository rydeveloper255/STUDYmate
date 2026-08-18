package com.example.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val usageMinutes: Int,
    val isDistracting: Boolean = true
)

data class AppUsageSummary(
    val isPermissionGranted: Boolean,
    val totalDistractingMinutes: Int,
    val topDistractingAppName: String?,
    val topDistractingAppMinutes: Int,
    val appList: List<AppUsageItem>
)

object NovaUsageStatsHelper {

    val MONITORED_DISTRACTING_PACKAGES = mapOf(
        "com.google.android.youtube" to "YouTube",
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.snapchat.android" to "Snapchat",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.twitter.android" to "X (Twitter)",
        "com.reddit.frontpage" to "Reddit",
        "com.netflix.mediaclient" to "Netflix",
        "com.twitch.tv.android.app" to "Twitch",
        "com.discord" to "Discord",
        "com.supercell.clashofclans" to "Clash of Clans",
        "com.dts.freefireth" to "Free Fire",
        "com.pubg.imobile" to "BGMI / PUBG"
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic settings
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun getTodayDistractingAppUsage(context: Context): AppUsageSummary {
        if (!hasUsageStatsPermission(context)) {
            return AppUsageSummary(
                isPermissionGranted = false,
                totalDistractingMinutes = 0,
                topDistractingAppName = null,
                topDistractingAppMinutes = 0,
                appList = emptyList()
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return AppUsageSummary(false, 0, null, 0, emptyList())

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats: List<UsageStats> = try {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val items = mutableListOf<AppUsageItem>()
        var totalDistractingMillis = 0L
        var maxMillis = 0L
        var topAppName: String? = null

        val customRestricted = FocusShieldManager.getRestrictedPackages()

        for (usage in stats) {
            val pkg = usage.packageName
            val totalTimeInForeground = usage.totalTimeInForeground
            if (totalTimeInForeground > 60_000) { // > 1 min
                val isMonitored = MONITORED_DISTRACTING_PACKAGES.containsKey(pkg) || customRestricted.contains(pkg)
                if (isMonitored) {
                    val appName = MONITORED_DISTRACTING_PACKAGES[pkg]
                        ?: FocusShieldManager.getAppNameForPackage(context, pkg)
                    val minutes = (totalTimeInForeground / 60_000).toInt()
                    items.add(AppUsageItem(pkg, appName, minutes, isDistracting = true))
                    totalDistractingMillis += totalTimeInForeground
                    if (totalTimeInForeground > maxMillis) {
                        maxMillis = totalTimeInForeground
                        topAppName = appName
                    }
                }
            }
        }

        items.sortByDescending { it.usageMinutes }

        val totalMinutes = (totalDistractingMillis / 60_000).toInt()
        val topMinutes = (maxMillis / 60_000).toInt()

        return AppUsageSummary(
            isPermissionGranted = true,
            totalDistractingMinutes = totalMinutes,
            topDistractingAppName = topAppName,
            topDistractingAppMinutes = topMinutes,
            appList = items
        )
    }
}
