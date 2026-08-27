package com.example.service.focus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Locale

/**
 * Focus Protection Engine 2.0 - Device / OEM Compatibility Layer
 * Detects known aggressive battery manager implementations (Xiaomi/MIUI, Huawei, Oppo, Vivo, Samsung)
 * and provides safe, honest guidance without using unsupported OS-level hacks.
 */
object DeviceCompatibilityLayer {

    private const val TAG = "DeviceCompatLayer"

    enum class OemVendor {
        XIAOMI,
        HUAWEI_HONOR,
        OPPO_REALME_ONEPLUS,
        VIVO_IQOO,
        SAMSUNG,
        GOOGLE_PIXEL,
        STANDARD_AOSP
    }

    data class OemProfile(
        val vendor: OemVendor,
        val manufacturerName: String,
        val hasAggressiveBackgroundRestrictions: Boolean,
        val guidanceMessage: String,
        val settingActionName: String
    )

    fun getDeviceProfile(): OemProfile {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)

        return when {
            manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> {
                OemProfile(
                    vendor = OemVendor.XIAOMI,
                    manufacturerName = "Xiaomi / Redmi / POCO",
                    hasAggressiveBackgroundRestrictions = true,
                    guidanceMessage = "MIUI/HyperOS may pause background timers. Please allow Auto-start & set Battery Saver to 'No restrictions'.",
                    settingActionName = "Open MIUI Autostart Settings"
                )
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                OemProfile(
                    vendor = OemVendor.HUAWEI_HONOR,
                    manufacturerName = "Huawei / Honor",
                    hasAggressiveBackgroundRestrictions = true,
                    guidanceMessage = "EMUI App Launch manager may stop background protection. Set StudyMate launch mode to 'Manage manually'.",
                    settingActionName = "Open App Launch Settings"
                )
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> {
                OemProfile(
                    vendor = OemVendor.OPPO_REALME_ONEPLUS,
                    manufacturerName = "Oppo / Realme / OnePlus",
                    hasAggressiveBackgroundRestrictions = true,
                    guidanceMessage = "ColorOS/OxygenOS may restrict background services. Enable 'Allow background activity' and disable battery optimization.",
                    settingActionName = "Open App Battery Settings"
                )
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                OemProfile(
                    vendor = OemVendor.VIVO_IQOO,
                    manufacturerName = "Vivo / iQOO",
                    hasAggressiveBackgroundRestrictions = true,
                    guidanceMessage = "FuntouchOS/OriginOS restricts background tasks. Enable 'High background power consumption' for uninterrupted sessions.",
                    settingActionName = "Open Background Power Settings"
                )
            }
            manufacturer.contains("samsung") -> {
                OemProfile(
                    vendor = OemVendor.SAMSUNG,
                    manufacturerName = "Samsung",
                    hasAggressiveBackgroundRestrictions = true,
                    guidanceMessage = "OneUI puts background apps to sleep. Ensure StudyMate is in 'Never sleeping apps' or battery is set to 'Unrestricted'.",
                    settingActionName = "Open Battery Optimization"
                )
            }
            manufacturer.contains("google") -> {
                OemProfile(
                    vendor = OemVendor.GOOGLE_PIXEL,
                    manufacturerName = "Google Pixel",
                    hasAggressiveBackgroundRestrictions = false,
                    guidanceMessage = "Pixel running standard Android battery management. Set battery usage to 'Unrestricted' for ideal performance.",
                    settingActionName = "Open App Battery Settings"
                )
            }
            else -> {
                OemProfile(
                    vendor = OemVendor.STANDARD_AOSP,
                    manufacturerName = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                    hasAggressiveBackgroundRestrictions = false,
                    guidanceMessage = "Ensure battery optimization is disabled so StudyMate foreground monitoring remains active.",
                    settingActionName = "Open Battery Optimization"
                )
            }
        }
    }

    /**
     * Attempts to open OEM-specific battery/auto-start manager with safe fallback to standard settings
     */
    fun openOemBackgroundSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        val profile = getDeviceProfile()
        when (profile.vendor) {
            OemVendor.XIAOMI -> {
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                intents.add(Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
                intents.add(Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")))
            }
            OemVendor.HUAWEI_HONOR -> {
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
            }
            OemVendor.OPPO_REALME_ONEPLUS -> {
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.BatteryActivity")))
            }
            OemVendor.VIVO_IQOO -> {
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.MainGuideActivity")))
            }
            OemVendor.SAMSUNG -> {
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity")))
            }
            else -> {
                // Handled in fallback
            }
        }

        // Add standard Android Battery Optimization Request Intent
        try {
            val reqOptIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            intents.add(reqOptIntent)
        } catch (e: Exception) {
            // Ignore
        }

        // Add standard App Details settings fallback
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        intents.add(appDetailsIntent)

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Intent failed: ${e.message}")
            }
        }

        // Final fallback
        return try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            true
        } catch (e: Exception) {
            false
        }
    }
}
