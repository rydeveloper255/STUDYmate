package com.example.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

enum class SensitiveCategory(val displayName: String, val description: String) {
    PAYMENTS("Payments & UPI", "BHIM, Google Pay, PhonePe, Paytm, Mobikwik, Cred"),
    BANKING("Banking & Savings", "YONO SBI, HDFC, ICICI, Axis, Kotak, PNB, BoB, Canara"),
    FINANCIAL("Investments & Wealth", "Groww, Zerodha Kite, Angel One, Upstox, ET Money"),
    PASSWORD_MANAGERS("Password Managers", "Bitwarden, 1Password, Dashlane, LastPass, KeePass"),
    AUTHENTICATION("Authentication & Security", "Google Authenticator, Microsoft Auth, Authy, Aegis"),
    SECURITY_SENSITIVE("Crypto & Sensitive", "Binance, CoinDCX, WazirX, Trust Wallet"),
    STUDY_ALLOWED("Standard Study Apps", "YouTube, Browser, Study Notes, Educational Apps")
}

enum class SafetyMode {
    SENSITIVE_PASSIVE, // Zero interaction, zero overlays, zero clicking/typing, zero text reading
    STANDARD           // Standard focus shield behavior
}

data class PackageSafetyProfile(
    val packageName: String,
    val appName: String,
    val category: SensitiveCategory,
    val mode: SafetyMode = SafetyMode.SENSITIVE_PASSIVE
)

object AccessibilitySafetyManager {

    private const val PREFS_NAME = "studymate_accessibility_safety_prefs"
    private const val KEY_SAFETY_MODE_ENABLED = "safety_mode_enabled"
    private const val KEY_ACCESSIBILITY_PAUSED = "accessibility_paused_by_user"
    private const val KEY_CUSTOM_SENSITIVE_PACKAGES = "custom_sensitive_packages"

    // Default Sensitive App Registry
    private val DEFAULT_SENSITIVE_PACKAGES = mapOf(
        // Payments & UPI
        "net.one97.paytm" to PackageSafetyProfile("net.one97.paytm", "Paytm", SensitiveCategory.PAYMENTS),
        "com.phonepe.app" to PackageSafetyProfile("com.phonepe.app", "PhonePe", SensitiveCategory.PAYMENTS),
        "com.google.android.apps.nbu.paisa.user" to PackageSafetyProfile("com.google.android.apps.nbu.paisa.user", "Google Pay", SensitiveCategory.PAYMENTS),
        "in.org.npci.upiapp" to PackageSafetyProfile("in.org.npci.upiapp", "BHIM UPI", SensitiveCategory.PAYMENTS),
        "com.mobikwik_new" to PackageSafetyProfile("com.mobikwik_new", "MobiKwik", SensitiveCategory.PAYMENTS),
        "com.dreamplug.androidapp" to PackageSafetyProfile("com.dreamplug.androidapp", "Cred", SensitiveCategory.PAYMENTS),
        "com.amazon.mShop.android.shopping" to PackageSafetyProfile("com.amazon.mShop.android.shopping", "Amazon Pay", SensitiveCategory.PAYMENTS),
        "com.freecharge.android" to PackageSafetyProfile("com.freecharge.android", "Freecharge", SensitiveCategory.PAYMENTS),

        // Banking
        "com.sbi.lotusintouch" to PackageSafetyProfile("com.sbi.lotusintouch", "YONO SBI", SensitiveCategory.BANKING),
        "com.sbi.quick" to PackageSafetyProfile("com.sbi.quick", "SBI Quick", SensitiveCategory.BANKING),
        "com.snapwork.hdfc" to PackageSafetyProfile("com.snapwork.hdfc", "HDFC Bank MobileBanking", SensitiveCategory.BANKING),
        "com.csam.icici.bank.imobile" to PackageSafetyProfile("com.csam.icici.bank.imobile", "iMobile Pay ICICI", SensitiveCategory.BANKING),
        "com.axis.mobile" to PackageSafetyProfile("com.axis.mobile", "Axis Mobile", SensitiveCategory.BANKING),
        "com.kotak.mobilebanking" to PackageSafetyProfile("com.kotak.mobilebanking", "Kotak Bank 811", SensitiveCategory.BANKING),
        "com.pnb.pnbone" to PackageSafetyProfile("com.pnb.pnbone", "PNB One", SensitiveCategory.BANKING),
        "com.bankofbaroda.mconnect" to PackageSafetyProfile("com.bankofbaroda.mconnect", "bob World", SensitiveCategory.BANKING),
        "com.canarabank.ai1" to PackageSafetyProfile("com.canarabank.ai1", "Canara ai1", SensitiveCategory.BANKING),
        "com.infrasofttech.uboi" to PackageSafetyProfile("com.infrasofttech.uboi", "Vyom Union Bank", SensitiveCategory.BANKING),
        "com.idfcfirstbank.mConnect" to PackageSafetyProfile("com.idfcfirstbank.mConnect", "IDFC FIRST Bank", SensitiveCategory.BANKING),
        "com.indusind.mobile.ibkl" to PackageSafetyProfile("com.indusind.mobile.ibkl", "IndusMobile", SensitiveCategory.BANKING),

        // Investments & Wealth
        "com.nextbillion.groww" to PackageSafetyProfile("com.nextbillion.groww", "Groww", SensitiveCategory.FINANCIAL),
        "com.zerodha.kite3" to PackageSafetyProfile("com.zerodha.kite3", "Zerodha Kite", SensitiveCategory.FINANCIAL),
        "com.msf.angelmobile" to PackageSafetyProfile("com.msf.angelmobile", "Angel One", SensitiveCategory.FINANCIAL),
        "com.paytmmoney" to PackageSafetyProfile("com.paytmmoney", "Paytm Money", SensitiveCategory.FINANCIAL),
        "com.etmoney.app" to PackageSafetyProfile("com.etmoney.app", "ET Money", SensitiveCategory.FINANCIAL),
        "com.policybazaar" to PackageSafetyProfile("com.policybazaar", "PolicyBazaar", SensitiveCategory.FINANCIAL),
        "com.zerodha.coin" to PackageSafetyProfile("com.zerodha.coin", "Coin by Zerodha", SensitiveCategory.FINANCIAL),
        "com.kuvera.app" to PackageSafetyProfile("com.kuvera.app", "Kuvera", SensitiveCategory.FINANCIAL),
        "com.upstox.pro" to PackageSafetyProfile("com.upstox.pro", "Upstox Pro", SensitiveCategory.FINANCIAL),

        // Password Managers
        "com.x8bit.bitwarden" to PackageSafetyProfile("com.x8bit.bitwarden", "Bitwarden", SensitiveCategory.PASSWORD_MANAGERS),
        "com.onepassword.android" to PackageSafetyProfile("com.onepassword.android", "1Password", SensitiveCategory.PASSWORD_MANAGERS),
        "com.dashlane" to PackageSafetyProfile("com.dashlane", "Dashlane", SensitiveCategory.PASSWORD_MANAGERS),
        "com.lastpass.lpandroid" to PackageSafetyProfile("com.lastpass.lpandroid", "LastPass", SensitiveCategory.PASSWORD_MANAGERS),
        "keepass2android.keepass2android" to PackageSafetyProfile("keepass2android.keepass2android", "KeePass2Android", SensitiveCategory.PASSWORD_MANAGERS),
        "io.enpass.app" to PackageSafetyProfile("io.enpass.app", "Enpass", SensitiveCategory.PASSWORD_MANAGERS),

        // Authentication & Security
        "com.google.android.apps.authenticator2" to PackageSafetyProfile("com.google.android.apps.authenticator2", "Google Authenticator", SensitiveCategory.AUTHENTICATION),
        "com.azure.authenticator" to PackageSafetyProfile("com.azure.authenticator", "Microsoft Authenticator", SensitiveCategory.AUTHENTICATION),
        "com.authy.authy" to PackageSafetyProfile("com.authy.authy", "Authy", SensitiveCategory.AUTHENTICATION),
        "com.beemdevelopment.aegis" to PackageSafetyProfile("com.beemdevelopment.aegis", "Aegis Authenticator", SensitiveCategory.AUTHENTICATION),
        "com.duosecurity.duomobile" to PackageSafetyProfile("com.duosecurity.duomobile", "Duo Mobile", SensitiveCategory.AUTHENTICATION),

        // Crypto & Sensitive
        "com.binance.dev" to PackageSafetyProfile("com.binance.dev", "Binance", SensitiveCategory.SECURITY_SENSITIVE),
        "com.coindcx.btc" to PackageSafetyProfile("com.coindcx.btc", "CoinDCX", SensitiveCategory.SECURITY_SENSITIVE),
        "com.wazirx.exchange" to PackageSafetyProfile("com.wazirx.exchange", "WazirX", SensitiveCategory.SECURITY_SENSITIVE),
        "com.wallet.crypto.trustapp" to PackageSafetyProfile("com.wallet.crypto.trustapp", "Trust Wallet", SensitiveCategory.SECURITY_SENSITIVE),
        "com.coinswitch.kuber" to PackageSafetyProfile("com.coinswitch.kuber", "CoinSwitch", SensitiveCategory.SECURITY_SENSITIVE)
    )

    private val _isSafetyModeEnabled = MutableStateFlow(true)
    val isSafetyModeEnabled: StateFlow<Boolean> = _isSafetyModeEnabled.asStateFlow()

    private val _isAccessibilityPausedByUser = MutableStateFlow(false)
    val isAccessibilityPausedByUser: StateFlow<Boolean> = _isAccessibilityPausedByUser.asStateFlow()

    private val _isInSensitiveAppMode = MutableStateFlow(false)
    val isInSensitiveAppMode: StateFlow<Boolean> = _isInSensitiveAppMode.asStateFlow()

    private val _activeCategory = MutableStateFlow(SensitiveCategory.STUDY_ALLOWED)
    val activeCategory: StateFlow<SensitiveCategory> = _activeCategory.asStateFlow()

    private val _activePackageName = MutableStateFlow<String?>(null)
    val activePackageName: StateFlow<String?> = _activePackageName.asStateFlow()

    private var sensitiveAppOpenedAt: Long? = null
    private var sensitiveAppClosedAt: Long? = null

    private val customSensitiveMap = mutableMapOf<String, PackageSafetyProfile>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        _isSafetyModeEnabled.value = prefs.getBoolean(KEY_SAFETY_MODE_ENABLED, true)
        _isAccessibilityPausedByUser.value = prefs.getBoolean(KEY_ACCESSIBILITY_PAUSED, false)

        val customPackages = prefs.getStringSet(KEY_CUSTOM_SENSITIVE_PACKAGES, emptySet()) ?: emptySet()
        customPackages.forEach { pkg ->
            val appName = FocusShieldManager.getAppNameForPackage(context, pkg)
            customSensitiveMap[pkg] = PackageSafetyProfile(pkg, appName, SensitiveCategory.SECURITY_SENSITIVE)
        }
        isInitialized = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSafetyModeEnabled(context: Context, enabled: Boolean) {
        _isSafetyModeEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_SAFETY_MODE_ENABLED, enabled).apply()
        logSafetyEvent(if (enabled) "Accessibility_Safety_Mode_Enabled" else "Accessibility_Safety_Mode_Disabled")
    }

    fun setAccessibilityPausedByUser(context: Context, paused: Boolean) {
        _isAccessibilityPausedByUser.value = paused
        getPrefs(context).edit().putBoolean(KEY_ACCESSIBILITY_PAUSED, paused).apply()
        logSafetyEvent(if (paused) "Accessibility_Paused_By_User" else "Accessibility_Resumed_By_User")
    }

    fun addCustomSensitivePackage(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        val appName = FocusShieldManager.getAppNameForPackage(context, packageName)
        val profile = PackageSafetyProfile(packageName, appName, SensitiveCategory.SECURITY_SENSITIVE)
        customSensitiveMap[packageName] = profile

        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet(KEY_CUSTOM_SENSITIVE_PACKAGES, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(packageName)
        prefs.edit().putStringSet(KEY_CUSTOM_SENSITIVE_PACKAGES, currentSet).apply()
    }

    fun removeCustomSensitivePackage(context: Context, packageName: String) {
        customSensitiveMap.remove(packageName)
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet(KEY_CUSTOM_SENSITIVE_PACKAGES, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(packageName)
        prefs.edit().putStringSet(KEY_CUSTOM_SENSITIVE_PACKAGES, currentSet).apply()
    }

    /**
     * Inspects package safety profile
     */
    fun getSafetyProfile(packageName: String): PackageSafetyProfile? {
        return customSensitiveMap[packageName]
            ?: DEFAULT_SENSITIVE_PACKAGES[packageName]
            ?: if (isPackageNameSensitiveByKeyword(packageName)) {
                PackageSafetyProfile(packageName, packageName.substringAfterLast('.'), SensitiveCategory.BANKING)
            } else null
    }

    private fun isPackageNameSensitiveByKeyword(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("paytm") || lower.contains("phonepe") || lower.contains("gpay") ||
                lower.contains("upi") || lower.contains("bank") || lower.contains("wallet") ||
                lower.contains("auth") || lower.contains("password") || lower.contains("crypto") ||
                lower.contains("invest") || lower.contains("card") || lower.contains("money")
    }

    fun isSensitiveApp(packageName: String): Boolean {
        if (!_isSafetyModeEnabled.value) return false
        return getSafetyProfile(packageName) != null
    }

    /**
     * Called by FocusShieldAccessibilityService when a window state change occurs
     */
    fun onPackageForegroundChanged(packageName: String) {
        _activePackageName.value = packageName

        if (_isAccessibilityPausedByUser.value) {
            _isInSensitiveAppMode.value = false
            _activeCategory.value = SensitiveCategory.STUDY_ALLOWED
            return
        }

        val profile = getSafetyProfile(packageName)
        if (_isSafetyModeEnabled.value && profile != null) {
            if (!_isInSensitiveAppMode.value) {
                sensitiveAppOpenedAt = System.currentTimeMillis()
                logSafetyEvent("Sensitive_App_Mode_Activated_${profile.category.name}")
            }
            _isInSensitiveAppMode.value = true
            _activeCategory.value = profile.category
        } else {
            if (_isInSensitiveAppMode.value) {
                sensitiveAppClosedAt = System.currentTimeMillis()
                logSafetyEvent("Sensitive_App_Mode_Deactivated")
            }
            _isInSensitiveAppMode.value = false
            _activeCategory.value = SensitiveCategory.STUDY_ALLOWED
        }
    }

    /**
     * Safety check: Should Nova perform any accessibility interaction/blocking?
     */
    fun isSafeToInteract(packageName: String): Boolean {
        if (_isAccessibilityPausedByUser.value) return false
        if (_isSafetyModeEnabled.value && isSensitiveApp(packageName)) return false
        return true
    }

    /**
     * Safety check: Should Nova block or overlay on this package?
     */
    fun shouldSuppressInterruption(packageName: String): Boolean {
        if (_isAccessibilityPausedByUser.value) return true
        if (_isSafetyModeEnabled.value && isSensitiveApp(packageName)) return true
        return false
    }

    /**
     * Log non-sensitive analytics event safely
     */
    private fun logSafetyEvent(eventType: String) {
        // Safe analytics telemetry - contains NO screen content, NO OTPs, NO credentials
        android.util.Log.d("AccessibilitySafety", "Safety Event: $eventType")
    }

    /**
     * GetAllRegisteredSensitiveApps for UI listing
     */
    fun getAllSensitiveProfiles(): List<PackageSafetyProfile> {
        val list = mutableListOf<PackageSafetyProfile>()
        list.addAll(DEFAULT_SENSITIVE_PACKAGES.values)
        list.addAll(customSensitiveMap.values)
        return list.distinctBy { it.packageName }.sortedBy { it.appName }
    }
}

/**
     * Privacy Filter for Gemini / External APIs
     */
object PrivacyFilter {

    private val CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b")
    private val CVV_PATTERN = Pattern.compile("\\b\\d{3,4}\\b")
    private val OTP_KEYWORD_PATTERN = Pattern.compile("(?i)\\b(otp|pin|passcode|password|cvv|secret|token)\\b[:\\s]*\\d{4,8}")
    private val UPI_ID_PATTERN = Pattern.compile("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}")

    fun sanitizeForGemini(rawText: String?): String {
        if (rawText.isNullOrBlank()) return ""

        var sanitized = rawText
        try {
            sanitized = CARD_PATTERN.matcher(sanitized).replaceAll("[REDACTED_CARD_NUMBER]")
            sanitized = OTP_KEYWORD_PATTERN.matcher(sanitized).replaceAll("[REDACTED_AUTH_CODE]")
            sanitized = UPI_ID_PATTERN.matcher(sanitized).replaceAll("[REDACTED_UPI_ID]")
        } catch (e: Exception) {
            return "[REDACTED_SENSITIVE_CONTENT]"
        }
        return sanitized
    }

    fun shouldSuppressCloudProcessing(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false
        val lower = rawText.lowercase()
        return lower.contains("otp") || lower.contains("enter pin") || lower.contains("cvv") ||
                lower.contains("password") || lower.contains("upi pin") || lower.contains("netbanking") ||
                lower.contains("account number") || lower.contains("credit card")
    }
}
