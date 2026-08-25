package com.example.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val isRestricted: Boolean = false,
    val isAllowed: Boolean = false,
    val isEssential: Boolean = false,
    val isSystem: Boolean = false
)

object FocusShieldManager {

    private const val PREFS_NAME = "nova_focus_shield_prefs_v2"
    private const val KEY_SHIELD_ENABLED = "focus_shield_enabled"
    private const val KEY_RESTRICTED_PACKAGES = "restricted_packages"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
    private const val KEY_BLOCKED_DOMAINS = "blocked_domains"
    private const val KEY_BLOCKED_DOMAINS_JSON = "blocked_domains_json"
    private const val KEY_ACTIVE_WEBSITE_CATEGORIES = "active_website_categories"
    private const val KEY_BLOCK_SHORTS = "block_shorts"
    private const val KEY_BLOCK_REELS = "block_reels"
    private const val KEY_STUDY_ONLY_MODE = "study_only_mode"
    private const val KEY_PRESET_TYPE = "preset_type"
    private const val KEY_DEFAULT_MINUTES = "default_minutes"
    private const val KEY_CUSTOM_EXCEPTIONS = "custom_exceptions"

    // Default Distracting App Database
    val DEFAULT_DISTRACTING_APPS = listOf(
        InstalledAppInfo("com.google.android.youtube", "YouTube", "Streaming", isRestricted = true),
        InstalledAppInfo("com.instagram.android", "Instagram", "Social Media", isRestricted = true),
        InstalledAppInfo("com.facebook.katana", "Facebook", "Social Media", isRestricted = true),
        InstalledAppInfo("com.snapchat.android", "Snapchat", "Social Media", isRestricted = true),
        InstalledAppInfo("com.zhiliaoapp.musically", "TikTok", "Shorts & Videos", isRestricted = true),
        InstalledAppInfo("com.twitter.android", "X (Twitter)", "Social Media", isRestricted = true),
        InstalledAppInfo("com.reddit.frontpage", "Reddit", "Social Media", isRestricted = true),
        InstalledAppInfo("com.netflix.mediaclient", "Netflix", "Streaming", isRestricted = true),
        InstalledAppInfo("com.discord", "Discord", "Messaging", isRestricted = true),
        InstalledAppInfo("com.twitch.tv.android.app", "Twitch", "Streaming", isRestricted = true),
        InstalledAppInfo("com.pinterest", "Pinterest", "Social Media", isRestricted = false),
        InstalledAppInfo("com.spotify.music", "Spotify", "Streaming", isRestricted = false),
        InstalledAppInfo("com.supercell.clashofclans", "Clash of Clans", "Gaming", isRestricted = true),
        InstalledAppInfo("com.dts.freefireth", "Free Fire", "Gaming", isRestricted = true),
        InstalledAppInfo("com.pubg.imobile", "BGMI / PUBG", "Gaming", isRestricted = true),
        InstalledAppInfo("com.king.candycrushsaga", "Candy Crush", "Gaming", isRestricted = true),
        InstalledAppInfo("com.innersloth.spacemafia", "Among Us", "Gaming", isRestricted = true)
    )

    // Predefined Website Categories
    val PREDEFINED_WEBSITE_CATEGORIES = listOf(
        WebsiteCategory(
            id = "social",
            name = "Social Media",
            icon = "💬",
            domains = listOf("instagram.com", "facebook.com", "x.com", "twitter.com", "reddit.com", "snapchat.com", "threads.net"),
            description = "Blocks social feeds and timelines"
        ),
        WebsiteCategory(
            id = "entertainment",
            name = "Entertainment & Streaming",
            icon = "🎬",
            domains = listOf("netflix.com", "primevideo.com", "hotstar.com", "twitch.tv", "disneyplus.com", "hulu.com"),
            description = "Blocks video streaming portals and entertainment"
        ),
        WebsiteCategory(
            id = "gaming",
            name = "Online Games & Platforms",
            icon = "🎮",
            domains = listOf("store.steampowered.com", "roblox.com", "epicgames.com", "chess.com", "poki.com"),
            description = "Blocks browser gaming and game stores"
        ),
        WebsiteCategory(
            id = "shopping",
            name = "Shopping & Commerce",
            icon = "🛍️",
            domains = listOf("amazon.com", "amazon.in", "flipkart.com", "myntra.com", "meesho.com"),
            description = "Blocks shopping portals and flash sales"
        )
    )

    // Essential system apps & UPI / Banking apps that MUST NEVER be blocked
    val ESSENTIAL_APPS_WHITELIST = setOf(
        // StudyMate & Core OS
        "com.example",
        "com.aistudio.studymate.kqxmvp",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.settings",
        "android",

        // Essential Communication & Phone
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.google.android.contacts",
        "com.android.contacts",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.android.emergency",

        // UPI & Digital Payment Apps
        "net.one97.paytm",
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "in.org.npci.upiapp",
        "in.amazon.mShop.android.shopping",
        "com.cred.android",
        "com.freecharge.android",
        "com.mobikwik_new",
        "co.tapapp.payviaupi",

        // Indian & Global Banking Apps
        "com.sbi.lotusintouch",
        "com.msf.kbank.mobile",
        "com.csam.icici.bank.imobile",
        "com.axis.mobile",
        "com.hdfcbank.payzapp",
        "com.snapwork.hdfc",
        "com.canarabank.mobility",
        "com.pnb.pnbone",
        "com.bankofbaroda.mconnect",
        "com.unionbank.ecommerce.mobile.android",
        "com.infrasofttech.uboi",
        "com.idfcfirstbank.mConnect",
        "com.indusind.mobile.ibkl",
        "com.x8bit.bitwarden",
        "com.onepassword.android",
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator"
    )

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _currentSubject = MutableStateFlow("General Science")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    private val _currentTopic = MutableStateFlow("Sound & Waves")
    val currentTopic: StateFlow<String> = _currentTopic.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _initialMinutes = MutableStateFlow(25)
    val initialMinutes: StateFlow<Int> = _initialMinutes.asStateFlow()

    private val _focusPolicy = MutableStateFlow(FocusPolicy())
    val focusPolicy: StateFlow<FocusPolicy> = _focusPolicy.asStateFlow()
    val currentPolicy: StateFlow<FocusPolicy> get() = _focusPolicy

    private val _activePreset = MutableStateFlow(FocusPreset.DEEP_STUDY)
    val activePreset: StateFlow<FocusPreset> = _activePreset.asStateFlow()

    private val _protectionStatus = MutableStateFlow(FocusProtectionStatus.PROTECTION_ACTIVE)
    val protectionStatus: StateFlow<FocusProtectionStatus> = _protectionStatus.asStateFlow()

    private val _protectionHealth = MutableStateFlow(ProtectionHealth())
    val protectionHealth: StateFlow<ProtectionHealth> = _protectionHealth.asStateFlow()

    private var restrictedPackageSet = mutableSetOf<String>()
    private var allowedPackageSet = mutableSetOf<String>()
    private var customExceptionsSet = mutableSetOf<String>()
    private var blockedWebsitesList = mutableListOf<BlockedWebsiteItem>()

    private var isShieldFeatureEnabled: Boolean = true
    private var blockShortsEnabled: Boolean = true
    private var blockReelsEnabled: Boolean = true
    private var studyOnlyModeEnabled: Boolean = true

    private var cachedInstalledApps: List<InstalledAppInfo>? = null
    private val iconCache = mutableMapOf<String, Bitmap?>()

    private var monitoringJob: Job? = null
    private var lastTriggeredPackage: String = ""
    private var lastTriggerTime: Long = 0L

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isShieldFeatureEnabled = prefs.getBoolean(KEY_SHIELD_ENABLED, true)

        val savedRestricted = prefs.getStringSet(KEY_RESTRICTED_PACKAGES, null)
        restrictedPackageSet = if (savedRestricted != null) {
            savedRestricted.toMutableSet()
        } else {
            DEFAULT_DISTRACTING_APPS.filter { it.isRestricted }.map { it.packageName }.toMutableSet()
        }

        val savedAllowed = prefs.getStringSet(KEY_ALLOWED_PACKAGES, null)
        allowedPackageSet = if (savedAllowed != null) {
            savedAllowed.toMutableSet()
        } else {
            mutableSetOf(
                "com.google.android.calculator",
                "com.android.calculator2",
                "com.google.android.deskclock",
                "com.google.android.apps.docs",
                "com.adobe.reader"
            )
        }

        val savedCustomExceptions = prefs.getStringSet(KEY_CUSTOM_EXCEPTIONS, null)
        if (savedCustomExceptions != null) {
            customExceptionsSet = savedCustomExceptions.toMutableSet()
        }

        val presetName = prefs.getString(KEY_PRESET_TYPE, FocusPreset.DEEP_STUDY.name) ?: FocusPreset.DEEP_STUDY.name
        val preset = try {
            FocusPreset.valueOf(presetName)
        } catch (e: Exception) {
            FocusPreset.DEEP_STUDY
        }
        _activePreset.value = preset

        blockShortsEnabled = prefs.getBoolean(KEY_BLOCK_SHORTS, true)
        blockReelsEnabled = prefs.getBoolean(KEY_BLOCK_REELS, true)
        studyOnlyModeEnabled = prefs.getBoolean(KEY_STUDY_ONLY_MODE, true)

        val domainsJson = prefs.getString(KEY_BLOCKED_DOMAINS_JSON, null)
        blockedWebsitesList = if (!domainsJson.isNullOrBlank()) {
            parseDomainsJson(domainsJson).toMutableList()
        } else {
            mutableListOf(
                BlockedWebsiteItem("instagram.com", "Social Media"),
                BlockedWebsiteItem("facebook.com", "Social Media"),
                BlockedWebsiteItem("twitter.com", "Social Media"),
                BlockedWebsiteItem("x.com", "Social Media"),
                BlockedWebsiteItem("tiktok.com", "Shorts & Videos"),
                BlockedWebsiteItem("reddit.com", "Social Media"),
                BlockedWebsiteItem("netflix.com", "Streaming"),
                BlockedWebsiteItem("twitch.tv", "Streaming"),
                BlockedWebsiteItem("primevideo.com", "Streaming"),
                BlockedWebsiteItem("hotstar.com", "Streaming")
            )
        }

        AccessibilitySafetyManager.init(context)
        syncCurrentPolicy(context)
        updateProtectionHealth(context)
        updateProtectionStatus(context)
    }

    /**
     * Checks if Usage Access permission is granted (Non-Accessibility standard mechanism)
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

    /**
     * Legacy check for accessibility service (Optional / Secondary).
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(context.packageName, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Evaluate diagnostic health of focus protection engine
     */
    fun updateProtectionHealth(context: Context): ProtectionHealth {
        val hasUsage = isUsageAccessGranted(context)
        val health = when {
            !isShieldFeatureEnabled -> ProtectionHealth(
                isOperational = false,
                message = "Focus Shield is paused in settings",
                actionRequired = false
            )
            hasUsage -> ProtectionHealth(
                isOperational = true,
                message = "Non-accessibility shield active (Zero payment interference)",
                actionRequired = false,
                permissionType = "NONE"
            )
            else -> ProtectionHealth(
                isOperational = false,
                message = "Usage Access needed to monitor distractions accurately",
                actionRequired = true,
                permissionType = "USAGE_STATS"
            )
        }
        _protectionHealth.value = health
        return health
    }

    fun updateProtectionStatus(context: Context): FocusProtectionStatus {
        val hasUsage = isUsageAccessGranted(context)
        val status = when {
            !isShieldFeatureEnabled -> FocusProtectionStatus.NEEDS_ATTENTION
            hasUsage -> FocusProtectionStatus.PROTECTION_ACTIVE
            else -> FocusProtectionStatus.GENTLE_FOCUS
        }
        _protectionStatus.value = status
        return status
    }

    fun getRestrictedPackages(): Set<String> = restrictedPackageSet.toSet()
    fun getAllowedPackages(): Set<String> = allowedPackageSet.toSet()
    fun getBlockedWebsites(): List<BlockedWebsiteItem> = blockedWebsitesList.toList()
    fun getCustomExceptions(): Set<String> = customExceptionsSet.toSet()

    fun isAppRestricted(pkgName: String): Boolean {
        if (!isShieldFeatureEnabled) return false
        if (isEssentialApp(pkgName)) return false
        if (allowedPackageSet.contains(pkgName)) return false
        if (AccessibilitySafetyManager.shouldSuppressInterruption(pkgName)) return false
        return restrictedPackageSet.contains(pkgName)
    }

    fun isEssentialApp(pkgName: String): Boolean {
        if (ESSENTIAL_APPS_WHITELIST.contains(pkgName)) return true
        if (customExceptionsSet.contains(pkgName)) return true
        return AccessibilitySafetyManager.isSensitiveApp(pkgName)
    }

    fun selectAllApps(context: Context, packageNames: List<String>) {
        packageNames.forEach { pkg ->
            if (!isEssentialApp(pkg)) {
                restrictedPackageSet.add(pkg)
                allowedPackageSet.remove(pkg)
            }
        }
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun deselectAllApps(context: Context, packageNames: List<String>) {
        packageNames.forEach { pkg ->
            restrictedPackageSet.remove(pkg)
        }
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun toggleWebsiteCategory(context: Context, categoryId: String, isEnabled: Boolean) {
        val cat = PREDEFINED_WEBSITE_CATEGORIES.find { it.id == categoryId } ?: return
        cat.domains.forEach { domain ->
            if (isEnabled) {
                if (blockedWebsitesList.none { it.domain.equals(domain, ignoreCase = true) }) {
                    blockedWebsitesList.add(BlockedWebsiteItem(domain, cat.name, true))
                }
            } else {
                blockedWebsitesList.removeAll { it.domain.equals(domain, ignoreCase = true) }
            }
        }
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun setShortsBlocking(context: Context, blocked: Boolean) = setYouTubeShortsBlocked(context, blocked)
    fun setReelsBlocking(context: Context, blocked: Boolean) = setInstagramReelsBlocked(context, blocked)
    fun setStudyModeContentFilter(context: Context, enabled: Boolean) = setStudyOnlyMode(context, enabled)

    fun applyPreset(context: Context, preset: FocusPreset) {
        _activePreset.value = preset
        when (preset) {
            FocusPreset.DEEP_STUDY -> {
                restrictedPackageSet.clear()
                restrictedPackageSet.addAll(DEFAULT_DISTRACTING_APPS.map { it.packageName })
                blockShortsEnabled = true
                blockReelsEnabled = true
                studyOnlyModeEnabled = true
            }
            FocusPreset.MOCK_TEST -> {
                restrictedPackageSet.clear()
                restrictedPackageSet.addAll(DEFAULT_DISTRACTING_APPS.map { it.packageName })
                blockShortsEnabled = true
                blockReelsEnabled = true
                studyOnlyModeEnabled = true
            }
            FocusPreset.RESEARCH -> {
                restrictedPackageSet.clear()
                restrictedPackageSet.addAll(
                    DEFAULT_DISTRACTING_APPS
                        .filter { it.category != "Browsing" && it.category != "Messaging" }
                        .map { it.packageName }
                )
                blockShortsEnabled = true
                blockReelsEnabled = true
                studyOnlyModeEnabled = false
            }
            FocusPreset.LIGHT_FOCUS -> {
                restrictedPackageSet.clear()
                restrictedPackageSet.addAll(
                    DEFAULT_DISTRACTING_APPS
                        .filter { it.category == "Shorts & Videos" || it.category == "Gaming" }
                        .map { it.packageName }
                )
                blockShortsEnabled = true
                blockReelsEnabled = true
                studyOnlyModeEnabled = false
            }
            FocusPreset.CUSTOM -> {
                // Keep existing customizations
            }
        }
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun setAppRestricted(context: Context, packageName: String, restricted: Boolean) {
        if (restricted) {
            restrictedPackageSet.add(packageName)
            allowedPackageSet.remove(packageName)
        } else {
            restrictedPackageSet.remove(packageName)
        }
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun setAppAllowed(context: Context, packageName: String, allowed: Boolean) {
        if (allowed) {
            allowedPackageSet.add(packageName)
            restrictedPackageSet.remove(packageName)
        } else {
            allowedPackageSet.remove(packageName)
        }
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun addBlockedWebsite(context: Context, domain: String, category: String = "Custom") {
        val cleanDomain = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/')
        if (cleanDomain.isBlank()) return
        if (blockedWebsitesList.none { it.domain.equals(cleanDomain, ignoreCase = true) }) {
            blockedWebsitesList.add(BlockedWebsiteItem(domain = cleanDomain, category = category, isEnabled = true))
            _activePreset.value = FocusPreset.CUSTOM
            savePrefs(context)
            syncCurrentPolicy(context)
        }
    }

    fun removeBlockedWebsite(context: Context, domain: String) {
        blockedWebsitesList.removeAll { it.domain.equals(domain, ignoreCase = true) }
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun toggleBlockedWebsite(context: Context, domain: String, isEnabled: Boolean) {
        val idx = blockedWebsitesList.indexOfFirst { it.domain.equals(domain, ignoreCase = true) }
        if (idx >= 0) {
            val item = blockedWebsitesList[idx]
            blockedWebsitesList[idx] = item.copy(isEnabled = isEnabled)
            _activePreset.value = FocusPreset.CUSTOM
            savePrefs(context)
            syncCurrentPolicy(context)
        }
    }

    fun setYouTubeShortsBlocked(context: Context, blocked: Boolean) {
        blockShortsEnabled = blocked
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun setInstagramReelsBlocked(context: Context, blocked: Boolean) {
        blockReelsEnabled = blocked
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun setStudyOnlyMode(context: Context, enabled: Boolean) {
        studyOnlyModeEnabled = enabled
        _activePreset.value = FocusPreset.CUSTOM
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun addCustomException(context: Context, packageName: String) {
        customExceptionsSet.add(packageName)
        restrictedPackageSet.remove(packageName)
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun removeCustomException(context: Context, packageName: String) {
        customExceptionsSet.remove(packageName)
        savePrefs(context)
        syncCurrentPolicy(context)
    }

    fun setShieldFeatureEnabled(context: Context, enabled: Boolean) {
        isShieldFeatureEnabled = enabled
        savePrefs(context)
        syncCurrentPolicy(context)
        updateProtectionHealth(context)
        updateProtectionStatus(context)
    }

    fun isShieldEnabled(): Boolean = isShieldFeatureEnabled
    fun isShortsBlocked(): Boolean = blockShortsEnabled
    fun isReelsBlocked(): Boolean = blockReelsEnabled
    fun isStudyOnlyMode(): Boolean = studyOnlyModeEnabled

    private fun syncCurrentPolicy(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _focusPolicy.value = FocusPolicy(
            preset = _activePreset.value,
            activePreset = _activePreset.value,
            defaultDurationMinutes = prefs.getInt(KEY_DEFAULT_MINUTES, 25),
            blockedPackages = restrictedPackageSet.toSet(),
            allowedPackages = allowedPackageSet.toSet(),
            blockedDomains = blockedWebsitesList.toList(),
            blockedWebsites = blockedWebsitesList.map { it.domain }.toSet(),
            blockYouTubeShorts = blockShortsEnabled,
            blockInstagramReels = blockReelsEnabled,
            studyOnlyContentMode = studyOnlyModeEnabled,
            studyModeContentFilter = studyOnlyModeEnabled,
            customExceptions = customExceptionsSet.toSet(),
            essentialAppExceptions = customExceptionsSet.toSet(),
            ongoingNotificationEnabled = true
        )
    }

    private fun savePrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val domainsArray = JSONArray()
        blockedWebsitesList.forEach {
            val obj = JSONObject()
            obj.put("domain", it.domain)
            obj.put("category", it.category)
            obj.put("isEnabled", it.isEnabled)
            domainsArray.put(obj)
        }

        prefs.edit()
            .putStringSet(KEY_RESTRICTED_PACKAGES, restrictedPackageSet)
            .putStringSet(KEY_ALLOWED_PACKAGES, allowedPackageSet)
            .putStringSet(KEY_CUSTOM_EXCEPTIONS, customExceptionsSet)
            .putString(KEY_PRESET_TYPE, _activePreset.value.name)
            .putBoolean(KEY_SHIELD_ENABLED, isShieldFeatureEnabled)
            .putBoolean(KEY_BLOCK_SHORTS, blockShortsEnabled)
            .putBoolean(KEY_BLOCK_REELS, blockReelsEnabled)
            .putBoolean(KEY_STUDY_ONLY_MODE, studyOnlyModeEnabled)
            .putString(KEY_BLOCKED_DOMAINS_JSON, domainsArray.toString())
            .apply()
    }

    private fun parseDomainsJson(jsonStr: String): List<BlockedWebsiteItem> {
        val list = mutableListOf<BlockedWebsiteItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BlockedWebsiteItem(
                        domain = obj.getString("domain"),
                        category = obj.optString("category", "Distraction"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
        } catch (e: Exception) {
            // Handled
        }
        return list
    }

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
     * Trigger the full screen distraction interruption screen safely
     */
    fun triggerInterruption(context: Context, blockedPkg: String) {
        if (!_isSessionActive.value || !isShieldFeatureEnabled) return
        if (isEssentialApp(blockedPkg)) return
        if (AccessibilitySafetyManager.shouldSuppressInterruption(blockedPkg)) return

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
     * Non-Accessibility foreground watcher using UsageStatsManager
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
                        // Handled
                    }
                }
            }
        }
    }

    fun getAppNameForPackage(context: Context, pkgName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            val lastSegment = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            if (lastSegment.isNotBlank()) lastSegment else "Restricted App"
        }
    }

    fun loadInstalledApps(context: Context, forceRefresh: Boolean = false): List<InstalledAppInfo> {
        if (!forceRefresh && cachedInstalledApps != null) {
            return cachedInstalledApps!!.map {
                it.copy(
                    isRestricted = restrictedPackageSet.contains(it.packageName),
                    isAllowed = allowedPackageSet.contains(it.packageName),
                    isEssential = isEssentialApp(it.packageName)
                )
            }
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

                val appName = resolveInfo.loadLabel(pm).toString()
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isEssential = isEssentialApp(pkgName)
                val category = categorizeApp(pkgName, appName)

                seenPackages.add(pkgName)
                discovered.add(
                    InstalledAppInfo(
                        packageName = pkgName,
                        appName = appName,
                        category = category,
                        isRestricted = restrictedPackageSet.contains(pkgName),
                        isAllowed = allowedPackageSet.contains(pkgName),
                        isEssential = isEssential,
                        isSystem = isSystem
                    )
                )
            }
        } catch (e: Exception) {
            // Handled
        }

        for (defaultApp in DEFAULT_DISTRACTING_APPS) {
            if (!seenPackages.contains(defaultApp.packageName)) {
                seenPackages.add(defaultApp.packageName)
                discovered.add(
                    defaultApp.copy(
                        isRestricted = restrictedPackageSet.contains(defaultApp.packageName),
                        isAllowed = allowedPackageSet.contains(defaultApp.packageName),
                        isEssential = isEssentialApp(defaultApp.packageName)
                    )
                )
            }
        }

        discovered.sortBy { it.appName.lowercase() }
        cachedInstalledApps = discovered
        return discovered
    }

    private fun categorizeApp(pkg: String, name: String): String {
        val lowerPkg = pkg.lowercase()
        return when {
            lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
                    lowerPkg.contains("snapchat") || lowerPkg.contains("reddit") || lowerPkg.contains("threads") ||
                    lowerPkg.contains("pinterest") -> "Social Media"

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("twitch") ||
                    lowerPkg.contains("primevideo") || lowerPkg.contains("hotstar") || lowerPkg.contains("spotify") -> "Streaming"

            lowerPkg.contains("tiktok") || lowerPkg.contains("musically") || lowerPkg.contains("shorts") ||
                    lowerPkg.contains("reels") -> "Shorts & Videos"

            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("discord") ||
                    lowerPkg.contains("messenger") -> "Messaging"

            lowerPkg.contains("game") || lowerPkg.contains("clash") || lowerPkg.contains("pubg") ||
                    lowerPkg.contains("freefire") || lowerPkg.contains("candycrush") -> "Gaming"

            lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("firefox") ||
                    lowerPkg.contains("edge") -> "Browsing"

            else -> "Apps"
        }
    }

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
}
