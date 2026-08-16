package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val category: String = "Apps",
    val isRestricted: Boolean = false,
    val isSystem: Boolean = false
)

object FocusShieldManager {
    private const val PREFS_NAME = "studymate_focus_shield_prefs"
    private const val KEY_RESTRICTED_PACKAGES = "restricted_packages"
    private const val KEY_SHIELD_ENABLED = "shield_enabled"
    private const val KEY_CUSTOM_APPS = "custom_apps_json"

    // Default popular distracting apps
    val DEFAULT_DISTRACTING_APPS = listOf(
        InstalledAppInfo("com.google.android.youtube", "YouTube", "Streaming", isRestricted = true),
        InstalledAppInfo("com.instagram.android", "Instagram", "Social Media", isRestricted = true),
        InstalledAppInfo("com.facebook.katana", "Facebook", "Social Media", isRestricted = true),
        InstalledAppInfo("com.snapchat.android", "Snapchat", "Social Media", isRestricted = true),
        InstalledAppInfo("com.zhiliaoapp.musically", "TikTok", "Shorts & Videos", isRestricted = true),
        InstalledAppInfo("com.twitter.android", "X (Twitter)", "Social Media", isRestricted = true),
        InstalledAppInfo("com.reddit.frontpage", "Reddit", "Social Media", isRestricted = true),
        InstalledAppInfo("com.netflix.mediaclient", "Netflix", "Streaming", isRestricted = true),
        InstalledAppInfo("com.android.chrome", "Chrome Browser", "Browsing", isRestricted = false),
        InstalledAppInfo("com.discord", "Discord", "Messaging", isRestricted = true),
        InstalledAppInfo("com.twitch.tv.android.app", "Twitch", "Streaming", isRestricted = true),
        InstalledAppInfo("org.telegram.messenger", "Telegram", "Messaging", isRestricted = false),
        InstalledAppInfo("com.pinterest", "Pinterest", "Social Media", isRestricted = false),
        InstalledAppInfo("com.spotify.music", "Spotify", "Streaming", isRestricted = false),
        InstalledAppInfo("com.supercell.clashofclans", "Clash of Clans", "Gaming", isRestricted = true),
        InstalledAppInfo("com.dts.freefireth", "Free Fire", "Gaming", isRestricted = true),
        InstalledAppInfo("com.pubg.imobile", "BGMI / PUBG", "Gaming", isRestricted = true)
    )

    // Essential system apps that should NEVER be blocked
    val ESSENTIAL_APPS_WHITELIST = setOf(
        "com.example",
        "com.aistudio.studymate.kqxmvp",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.android.settings",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.google.android.calculator",
        "com.android.calculator2",
        "com.google.android.contacts",
        "com.android.contacts",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.android.systemui",
        "com.google.android.googlequicksearchbox"
    )

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _currentSubject = MutableStateFlow("Physics")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    private val _currentTopic = MutableStateFlow("Core Concepts")
    val currentTopic: StateFlow<String> = _currentTopic.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _initialMinutes = MutableStateFlow(25)
    val initialMinutes: StateFlow<Int> = _initialMinutes.asStateFlow()

    private var restrictedPackageSet: MutableSet<String> = mutableSetOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.zhiliaoapp.musically",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.netflix.mediaclient",
        "com.discord",
        "com.supercell.clashofclans"
    )

    private var cachedInstalledApps: List<InstalledAppInfo>? = null
    private val iconCache = mutableMapOf<String, Bitmap?>()
    private var isShieldFeatureEnabled: Boolean = true

    private var monitoringJob: Job? = null
    private var lastTriggeredPackage: String = ""
    private var lastTriggerTime: Long = 0L

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_RESTRICTED_PACKAGES, null)
        if (saved != null) {
            restrictedPackageSet = saved.toMutableSet()
        }
        isShieldFeatureEnabled = prefs.getBoolean(KEY_SHIELD_ENABLED, true)
    }

    /**
     * Checks if accessibility service is active
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            val expectedServiceName = "${context.packageName}/${FocusShieldAccessibilityService::class.java.canonicalName}"
            enabledServices.any { it.id.equals(expectedServiceName, ignoreCase = true) || it.id.contains("FocusShieldAccessibilityService") }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if Usage Access permission is granted
     */
    fun isUsageAccessGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun getRestrictedPackages(): Set<String> = restrictedPackageSet.toSet()

    fun isAppRestricted(pkgName: String): Boolean {
        if (!isShieldFeatureEnabled) return false
        if (ESSENTIAL_APPS_WHITELIST.contains(pkgName)) return false
        return restrictedPackageSet.contains(pkgName)
    }

    fun getAppNameForPackage(context: Context, pkgName: String): String {
        cachedInstalledApps?.find { it.packageName == pkgName }?.let { return it.appName }
        DEFAULT_DISTRACTING_APPS.find { it.packageName == pkgName }?.let { return it.appName }
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            val lastSegment = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            if (lastSegment.isNotBlank()) lastSegment else "Restricted App"
        }
    }

    /**
     * Loads real installed applications with names, categories, and icons
     */
    fun loadInstalledApps(context: Context, forceRefresh: Boolean = false): List<InstalledAppInfo> {
        if (!forceRefresh && cachedInstalledApps != null) {
            return cachedInstalledApps!!.map { it.copy(isRestricted = restrictedPackageSet.contains(it.packageName)) }
        }

        val pm = context.packageManager
        val discovered = mutableListOf<InstalledAppInfo>()
        val seenPackages = mutableSetOf<String>()

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName || seenPackages.contains(pkgName)) continue
                if (ESSENTIAL_APPS_WHITELIST.contains(pkgName)) continue

                val appName = resolveInfo.loadLabel(pm).toString()
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val category = categorizeApp(pkgName, appName)

                seenPackages.add(pkgName)
                discovered.add(
                    InstalledAppInfo(
                        packageName = pkgName,
                        appName = appName,
                        category = category,
                        isRestricted = restrictedPackageSet.contains(pkgName),
                        isSystem = isSystem
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback to default list if query failed
        }

        // Also add popular defaults that might not have been returned by launcher query
        for (defaultApp in DEFAULT_DISTRACTING_APPS) {
            if (!seenPackages.contains(defaultApp.packageName)) {
                seenPackages.add(defaultApp.packageName)
                discovered.add(
                    defaultApp.copy(isRestricted = restrictedPackageSet.contains(defaultApp.packageName))
                )
            }
        }

        discovered.sortBy { it.appName.lowercase() }
        cachedInstalledApps = discovered
        return discovered
    }

    private fun categorizeApp(pkg: String, name: String): String {
        val lowerPkg = pkg.lowercase()
        val lowerName = name.lowercase()
        return when {
            lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
                    lowerPkg.contains("snapchat") || lowerPkg.contains("reddit") || lowerPkg.contains("pinterest") ||
                    lowerPkg.contains("threads") || lowerPkg.contains("linkedin") -> "Social Media"

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("twitch") ||
                    lowerPkg.contains("primevideo") || lowerPkg.contains("hotstar") || lowerPkg.contains("hulu") ||
                    lowerPkg.contains("disney") || lowerPkg.contains("spotify") || lowerPkg.contains("music") -> "Streaming"

            lowerPkg.contains("tiktok") || lowerPkg.contains("musically") || lowerPkg.contains("shorts") ||
                    lowerPkg.contains("reels") -> "Shorts & Videos"

            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("discord") ||
                    lowerPkg.contains("messenger") || lowerPkg.contains("signal") || lowerPkg.contains("wechat") -> "Messaging"

            lowerPkg.contains("game") || lowerPkg.contains("clash") || lowerPkg.contains("pubg") ||
                    lowerPkg.contains("freefire") || lowerPkg.contains("roblox") || lowerPkg.contains("minecraft") ||
                    lowerPkg.contains("candycrush") || lowerPkg.contains("supercell") -> "Gaming"

            lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("firefox") ||
                    lowerPkg.contains("opera") || lowerPkg.contains("edge") -> "Browsing"

            else -> "Apps"
        }
    }

    /**
     * Retrieves application icon as Bitmap with caching
     */
    fun getAppIconBitmap(context: Context, packageName: String): Bitmap? {
        if (iconCache.containsKey(packageName)) {
            return iconCache[packageName]
        }
        val bitmap = try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
        iconCache[packageName] = bitmap
        return bitmap
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun setAppRestricted(context: Context, packageName: String, restricted: Boolean) {
        if (restricted) {
            restrictedPackageSet.add(packageName)
        } else {
            restrictedPackageSet.remove(packageName)
        }
        savePrefs(context)
    }

    fun selectAllApps(context: Context, packageNames: List<String>) {
        restrictedPackageSet.addAll(packageNames)
        savePrefs(context)
    }

    fun deselectAllApps(context: Context, packageNames: List<String>) {
        restrictedPackageSet.removeAll(packageNames.toSet())
        savePrefs(context)
    }

    fun saveRestrictedPackages(context: Context, packageNames: Set<String>) {
        restrictedPackageSet = packageNames.toMutableSet()
        savePrefs(context)
    }

    fun setShieldFeatureEnabled(context: Context, enabled: Boolean) {
        isShieldFeatureEnabled = enabled
        savePrefs(context)
    }

    fun isShieldEnabled(): Boolean = isShieldFeatureEnabled

    private fun savePrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_RESTRICTED_PACKAGES, restrictedPackageSet)
            .putBoolean(KEY_SHIELD_ENABLED, isShieldFeatureEnabled)
            .apply()
    }

    /**
     * Start Focus Session & activate foreground monitoring
     */
    fun startFocusSession(context: Context, subject: String, topic: String, durationMinutes: Int) {
        _currentSubject.value = subject
        _currentTopic.value = topic
        _initialMinutes.value = durationMinutes
        _remainingSeconds.value = durationMinutes * 60
        _isSessionActive.value = true

        startForegroundWatcher(context.applicationContext)
    }

    fun updateRemainingTime(seconds: Int) {
        _remainingSeconds.value = seconds
    }

    fun endFocusSession() {
        _isSessionActive.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Trigger the full screen StudyMate interruption screen safely
     */
    fun triggerInterruption(context: Context, blockedPkg: String) {
        if (!_isSessionActive.value || !isShieldFeatureEnabled) return
        if (ESSENTIAL_APPS_WHITELIST.contains(blockedPkg)) return

        val now = System.currentTimeMillis()
        if (blockedPkg == lastTriggeredPackage && (now - lastTriggerTime) < 1800L) {
            return
        }
        lastTriggeredPackage = blockedPkg
        lastTriggerTime = now

        val intent = Intent(context, FocusShieldBlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("BLOCKED_PACKAGE", blockedPkg)
        }
        context.startActivity(intent)
    }

    /**
     * Dual background watcher for devices with UsageStats granted
     */
    private fun startForegroundWatcher(appContext: Context) {
        monitoringJob?.cancel()
        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (_isSessionActive.value && isActive) {
                delay(1200)
                if (!_isSessionActive.value) break

                if (isUsageAccessGranted(appContext) && usageStatsManager != null) {
                    try {
                        val endTime = System.currentTimeMillis()
                        val beginTime = endTime - 1000 * 5
                        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
                        if (!stats.isNullOrEmpty()) {
                            val top = stats.maxByOrNull { it.lastTimeUsed }
                            val pkg = top?.packageName
                            if (pkg != null && pkg != appContext.packageName && isAppRestricted(pkg)) {
                                withContext(Dispatchers.Main) {
                                    triggerInterruption(appContext, pkg)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Handled safely
                    }
                }
            }
        }
    }
}
