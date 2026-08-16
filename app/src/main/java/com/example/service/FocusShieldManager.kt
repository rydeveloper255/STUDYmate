package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FocusShieldApp(
    val packageName: String,
    val appName: String,
    val iconName: String = "apps",
    val category: String = "Social & Distraction",
    val isRestricted: Boolean = true
)

object FocusShieldManager {
    private const val PREFS_NAME = "studymate_focus_shield_prefs"
    private const val KEY_RESTRICTED_PACKAGES = "restricted_packages"
    private const val KEY_SHIELD_ENABLED = "shield_enabled"

    // Default popular distracting apps
    val DEFAULT_DISTRACTING_APPS = listOf(
        FocusShieldApp("com.google.android.youtube", "YouTube", "video_library", "Streaming"),
        FocusShieldApp("com.instagram.android", "Instagram", "camera_alt", "Social Media"),
        FocusShieldApp("com.facebook.katana", "Facebook", "people", "Social Media"),
        FocusShieldApp("com.snapchat.android", "Snapchat", "chat_bubble", "Social Media"),
        FocusShieldApp("com.zhiliaoapp.musically", "TikTok", "music_note", "Shorts & Videos"),
        FocusShieldApp("com.twitter.android", "X (Twitter)", "tag", "Social Media"),
        FocusShieldApp("com.reddit.frontpage", "Reddit", "forum", "Social Media"),
        FocusShieldApp("com.netflix.mediaclient", "Netflix", "movie", "Streaming"),
        FocusShieldApp("com.android.chrome", "Chrome Browser", "public", "Browsing"),
        FocusShieldApp("com.discord", "Discord", "chat", "Messaging"),
        FocusShieldApp("com.twitch.tv.android.app", "Twitch", "tv", "Streaming"),
        FocusShieldApp("org.telegram.messenger", "Telegram", "send", "Messaging"),
        FocusShieldApp("com.pinterest", "Pinterest", "image", "Social Media"),
        FocusShieldApp("com.spotify.music", "Spotify", "music_note", "Streaming"),
        FocusShieldApp("com.supercell.clashofclans", "Clash of Clans", "sports_esports", "Gaming")
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
        "com.android.systemui"
    )

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _currentSubject = MutableStateFlow("Physics")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    private val _currentTopic = MutableStateFlow("Current Electricity")
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

    private var customAppsList: MutableList<FocusShieldApp> = mutableListOf()

    private var isShieldFeatureEnabled: Boolean = true

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_RESTRICTED_PACKAGES, null)
        if (saved != null) {
            restrictedPackageSet = saved.toMutableSet()
        }
        isShieldFeatureEnabled = prefs.getBoolean(KEY_SHIELD_ENABLED, true)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val expectedServiceName = "${context.packageName}/${FocusShieldAccessibilityService::class.java.canonicalName}"
        return enabledServices.any { it.id.equals(expectedServiceName, ignoreCase = true) || it.id.contains("FocusShieldAccessibilityService") }
    }

    fun getRestrictedPackages(): Set<String> = restrictedPackageSet.toSet()

    fun isAppRestricted(pkgName: String): Boolean {
        if (!isShieldFeatureEnabled) return false
        if (ESSENTIAL_APPS_WHITELIST.contains(pkgName)) return false
        return restrictedPackageSet.contains(pkgName)
    }

    fun getAppNameForPackage(pkgName: String): String {
        val foundDefault = DEFAULT_DISTRACTING_APPS.find { it.packageName == pkgName }
        if (foundDefault != null) return foundDefault.appName
        val foundCustom = customAppsList.find { it.packageName == pkgName }
        if (foundCustom != null) return foundCustom.appName
        val lastSegment = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        return if (lastSegment.isNotBlank()) lastSegment else "Restricted App"
    }

    fun getAllApps(): List<FocusShieldApp> {
        val combined = (DEFAULT_DISTRACTING_APPS + customAppsList).distinctBy { it.packageName }
        return combined.map { app ->
            app.copy(isRestricted = restrictedPackageSet.contains(app.packageName))
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

    fun setAllAppsRestricted(context: Context, packageNames: List<String>, restricted: Boolean) {
        if (restricted) {
            restrictedPackageSet.addAll(packageNames)
        } else {
            restrictedPackageSet.removeAll(packageNames.toSet())
        }
        savePrefs(context)
    }

    fun addCustomApp(context: Context, appName: String, packageName: String, category: String) {
        if (packageName.isBlank()) return
        val app = FocusShieldApp(
            packageName = packageName,
            appName = appName.ifBlank { packageName.substringAfterLast('.') },
            category = category.ifBlank { "Custom App" },
            isRestricted = true
        )
        customAppsList.add(app)
        restrictedPackageSet.add(packageName)
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

    fun startFocusSession(subject: String, topic: String, durationMinutes: Int) {
        _currentSubject.value = subject
        _currentTopic.value = topic
        _initialMinutes.value = durationMinutes
        _remainingSeconds.value = durationMinutes * 60
        _isSessionActive.value = true
    }

    fun updateRemainingTime(seconds: Int) {
        _remainingSeconds.value = seconds
    }

    fun endFocusSession() {
        _isSessionActive.value = false
    }
}
