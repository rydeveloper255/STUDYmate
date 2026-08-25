package com.example.data.model

enum class FocusPreset(
    val displayName: String,
    val badgeIcon: String,
    val subtitle: String,
    val recommendedDurationMinutes: Int = 25
) {
    DEEP_STUDY(
        displayName = "Deep Study",
        badgeIcon = "📖",
        subtitle = "Full distraction shield for serious syllabus mastery",
        recommendedDurationMinutes = 45
    ),
    MOCK_TEST(
        displayName = "Mock Test",
        badgeIcon = "📝",
        subtitle = "CBT Mock & exam resources only, strict distraction block",
        recommendedDurationMinutes = 90
    ),
    RESEARCH(
        displayName = "Research",
        badgeIcon = "🔍",
        subtitle = "Allows study browser, Wikipedia, & educational portals",
        recommendedDurationMinutes = 60
    ),
    LIGHT_FOCUS(
        displayName = "Light Focus",
        badgeIcon = "☕",
        subtitle = "Blocks short-form videos & heavy gaming, keeps tools open",
        recommendedDurationMinutes = 25
    ),
    CUSTOM(
        displayName = "Custom Policy",
        badgeIcon = "⚙️",
        subtitle = "Personalized app, website, and content restrictions",
        recommendedDurationMinutes = 30
    );

    val title: String get() = displayName
    val iconEmoji: String get() = badgeIcon
    val description: String get() = subtitle
}

// Alias for compatibility
typealias FocusPresetType = FocusPreset

enum class FocusProtectionStatus(
    val title: String,
    val icon: String,
    val description: String,
    val isProtected: Boolean
) {
    PROTECTION_ACTIVE(
        title = "Protection Active",
        icon = "🛡️",
        description = "Supported background app monitoring & domain protection active",
        isProtected = true
    ),
    GENTLE_FOCUS(
        title = "Gentle Focus Mode",
        icon = "⏱️",
        description = "Timer & study discipline active. Grant Usage Access for automatic redirection.",
        isProtected = true
    ),
    NEEDS_ATTENTION(
        title = "Needs Attention",
        icon = "⚠️",
        description = "Protection paused or permission needs review in Settings",
        isProtected = false
    )
}

data class ProtectionHealth(
    val isOperational: Boolean = true,
    val message: String = "Shield Operational",
    val actionRequired: Boolean = false,
    val permissionType: String = "NONE"
)

data class BlockedWebsiteItem(
    val domain: String,
    val category: String = "Distraction",
    val isEnabled: Boolean = true
)

data class WebsiteCategory(
    val id: String,
    val name: String,
    val icon: String,
    val domains: List<String>,
    val description: String
)

data class FocusPolicy(
    val preset: FocusPreset = FocusPreset.DEEP_STUDY,
    val activePreset: FocusPreset = FocusPreset.DEEP_STUDY,
    val blockedPackages: Set<String> = setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.zhiliaoapp.musically",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.netflix.mediaclient",
        "com.discord",
        "com.supercell.clashofclans",
        "com.dts.freefireth",
        "com.pubg.imobile"
    ),
    val allowedPackages: Set<String> = setOf(
        "com.google.android.calculator",
        "com.android.calculator2",
        "com.google.android.deskclock",
        "com.google.android.apps.docs",
        "com.adobe.reader"
    ),
    val blockedDomains: List<BlockedWebsiteItem> = listOf(
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
    ),
    val blockedWebsites: Set<String> = setOf(
        "instagram.com",
        "facebook.com",
        "twitter.com",
        "x.com",
        "tiktok.com",
        "reddit.com",
        "netflix.com",
        "twitch.tv",
        "primevideo.com",
        "hotstar.com"
    ),
    val activeWebsiteCategoryIds: Set<String> = setOf("social", "entertainment", "gaming"),
    val blockYouTubeShorts: Boolean = true,
    val blockInstagramReels: Boolean = true,
    val studyOnlyContentMode: Boolean = true,
    val studyModeContentFilter: Boolean = true,
    val customExceptions: Set<String> = emptySet(),
    val essentialAppExceptions: Set<String> = emptySet(),
    val defaultDurationMinutes: Int = 25,
    val quickStartEnabled: Boolean = true,
    val showNotificationReminder: Boolean = true,
    val ongoingNotificationEnabled: Boolean = true
)
