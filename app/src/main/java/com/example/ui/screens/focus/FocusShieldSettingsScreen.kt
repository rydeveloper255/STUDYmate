package com.example.ui.screens.focus

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.FocusShieldManager
import com.example.service.InstalledAppInfo
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FocusShieldSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showAccessibilitySafetyScreen by remember { mutableStateOf(false) }
    var showAccessibilityPrivacyScreen by remember { mutableStateOf(false) }

    if (showAccessibilitySafetyScreen) {
        AccessibilitySafetySettingsScreen(onBack = { showAccessibilitySafetyScreen = false })
        return
    }

    if (showAccessibilityPrivacyScreen) {
        AccessibilityPrivacyScreen(onBack = { showAccessibilityPrivacyScreen = false })
        return
    }

    BackHandler(enabled = true) {
        onBack()
    }

    var isShieldEnabled by remember { mutableStateOf(FocusShieldManager.isShieldEnabled()) }
    var isAccessibilityGranted by remember { mutableStateOf(FocusShieldManager.isAccessibilityServiceEnabled(context)) }
    var isUsageAccessGranted by remember { mutableStateOf(FocusShieldManager.isUsageAccessGranted(context)) }
    var showPermissionExplanationDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = FocusShieldManager.isAccessibilityServiceEnabled(context)
                isUsageAccessGranted = FocusShieldManager.isUsageAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All Apps") }
    var isLoadingApps by remember { mutableStateOf(true) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var restrictedSet by remember { mutableStateOf<Set<String>>(FocusShieldManager.getRestrictedPackages()) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // Load installed apps asynchronously from PackageManager
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = FocusShieldManager.loadInstalledApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
                restrictedSet = FocusShieldManager.getRestrictedPackages()
                isLoadingApps = false
            }
        }
    }

    // Refresh permission statuses periodically when user returns
    LaunchedEffect(Unit) {
        isAccessibilityGranted = FocusShieldManager.isAccessibilityServiceEnabled(context)
        isUsageAccessGranted = FocusShieldManager.isUsageAccessGranted(context)
    }

    val categories = listOf("All Apps", "Social Media", "Streaming", "Shorts & Videos", "Messaging", "Gaming", "Browsing")

    val filteredApps = remember(installedApps, searchQuery, selectedCategory) {
        installedApps.filter { app ->
            val matchesCategory = selectedCategory == "All Apps" || app.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(16.dp)
            .testTag("focus_shield_settings_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF))
                                .testTag("focus_shield_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🛡️ Focus Mode → Blocked Apps",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Select apps to block during focus sessions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isShieldEnabled) EmeraldSuccess.copy(alpha = 0.2f) else CoralRose.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isShieldEnabled) EmeraldSuccess.copy(alpha = 0.5f) else CoralRose.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isShieldEnabled) "ACTIVE" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isShieldEnabled) EmeraldSuccess else CoralRose,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Master Shield Protection Toggle
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.8f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isShieldEnabled)
                                            Brush.linearGradient(listOf(NeonCyan, NebulaPurple))
                                        else
                                            Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF334155)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Focus App Blocking Shield",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isShieldEnabled) "Active during study countdowns" else "Disabled — apps will not be blocked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = isShieldEnabled,
                            onCheckedChange = { enabled ->
                                isShieldEnabled = enabled
                                FocusShieldManager.setShieldFeatureEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF070B19),
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0x30FFFFFF)
                            ),
                            modifier = Modifier.testTag("master_shield_toggle")
                        )
                    }
                }
            }

            // Permission Status & Clear Explanation Card (FEATURE 1 — Accessibility Permission Flow)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isAccessibilityGranted) Color(0x2010B981) else Color(0x25F59E0B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAccessibilityGranted) Color(0x5010B981) else Color(0x60F59E0B)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAccessibilityGranted) Icons.Filled.CheckCircle else Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (isAccessibilityGranted) EmeraldSuccess else GoldenSpark,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "App Blocking",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Status Indicator Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAccessibilityGranted) Color(0x3010B981) else Color(0x30F59E0B)
                            ) {
                                Text(
                                    text = if (isAccessibilityGranted) "✓ Accessibility enabled" else "Accessibility not enabled",
                                    color = if (isAccessibilityGranted) EmeraldSuccess else GoldenSpark,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Accessibility permission is required to block the apps you select during study time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showAccessibilitySafetyScreen = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("open_accessibility_safety_settings_btn")
                        ) {
                            Icon(Icons.Filled.VerifiedUser, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Banking & Payment Safety Settings 🛡️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showAccessibilityPrivacyScreen = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenSpark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("open_accessibility_privacy_btn")
                        ) {
                            Icon(Icons.Filled.Lock, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accessibility Privacy Guarantees 🔒", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (!isAccessibilityGranted) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("enable_accessibility_btn")
                            ) {
                                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Accessibility", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Section
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    fillAlpha = 0.75f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Search apps field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search installed apps...", color = Color(0xFF64748B)) },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = NeonCyan) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, "Clear", tint = Color(0xFF94A3B8))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_apps_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x30FFFFFF),
                                focusedContainerColor = Color(0x15FFFFFF),
                                unfocusedContainerColor = Color(0x15FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Chips
                        ScrollableTabRow(
                            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                            containerColor = Color.Transparent,
                            contentColor = NeonCyan,
                            edgePadding = 0.dp
                        ) {
                            categories.forEach { category ->
                                val isSel = selectedCategory == category
                                Tab(
                                    selected = isSel,
                                    onClick = { selectedCategory = category },
                                    text = {
                                        Text(
                                            text = category,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSel) NeonCyan else Color(0xFF94A3B8)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Bar: Select All, Deselect All, and Save Blocked Apps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val next = restrictedSet.toMutableSet()
                                        next.addAll(filteredApps.map { it.packageName })
                                        restrictedSet = next
                                        hasUnsavedChanges = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x2538BDF8), contentColor = NeonCyan),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("select_all_apps_btn")
                                ) {
                                    Icon(Icons.Filled.SelectAll, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val next = restrictedSet.toMutableSet()
                                        next.removeAll(filteredApps.map { it.packageName }.toSet())
                                        restrictedSet = next
                                        hasUnsavedChanges = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x30FFFFFF)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("deselect_all_apps_btn")
                                ) {
                                    Text("Deselect All", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    FocusShieldManager.saveRestrictedPackages(context, restrictedSet)
                                    hasUnsavedChanges = false
                                    Toast.makeText(context, "✅ Blocked apps updated successfully!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasUnsavedChanges) GoldenSpark else EmeraldSuccess,
                                    contentColor = Color(0xFF070B19)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("save_blocked_apps_btn")
                            ) {
                                Icon(Icons.Filled.Save, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (hasUnsavedChanges) "Save Changes" else "Saved",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // App List Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Installed Apps (${filteredApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val selectedCount = filteredApps.count { restrictedSet.contains(it.packageName) }
                    Text(
                        text = "$selectedCount Blocked",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoralRose,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), fillAlpha = 0.5f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.SearchOff, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No apps match your search.", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                // Apps List
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = restrictedSet.contains(app.packageName)
                    InstalledAppRow(
                        app = app,
                        isChecked = isChecked,
                        onCheckedChange = { checked ->
                            val next = restrictedSet.toMutableSet()
                            if (checked) {
                                next.add(app.packageName)
                            } else {
                                next.remove(app.packageName)
                            }
                            restrictedSet = next
                            hasUnsavedChanges = true
                            // Auto-persist immediately as well
                            FocusShieldManager.setAppRestricted(context, app.packageName, checked)
                        }
                    )
                }
            }

            // Whitelist System Info
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x1538BDF8),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2538BDF8))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Safe & Protected Whitelist",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Essential system utilities (Phone Calls, Emergency Messages, Calculator, Clock, System Settings, and StudyMate AI) are permanently whitelisted for safety and will never be blocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Floating Save Button (if unsaved changes exist)
        if (hasUnsavedChanges) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        FocusShieldManager.saveRestrictedPackages(context, restrictedSet)
                        hasUnsavedChanges = false
                        Toast.makeText(context, "✅ Blocked apps list saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(52.dp)
                        .testTag("floating_save_blocked_apps_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19))
                ) {
                    Icon(Icons.Filled.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save ${restrictedSet.size} Blocked Apps", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }

        // Permission Explanation Modal Dialog
        if (showPermissionExplanationDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionExplanationDialog = false },
                containerColor = Color(0xFF111827),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Why Permissions Are Needed", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "StudyMate AI uses official Android APIs to provide scheduled focus protection without violating user privacy:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x18FFFFFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🛡️ 1. Accessibility Service", color = NeonCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Used only during active focus countdowns to detect when a blocked app is brought to the foreground, allowing StudyMate AI to display your motivational interruption screen.",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x18FFFFFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📊 2. Usage Access (Alternative)", color = GoldenSpark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Used as a secondary Android method to detect foreground apps when Accessibility is unavailable on your device or version.",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2010B981),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "🔒 Privacy Guarantee: No keystrokes, personal chats, or screen contents are ever accessed or stored. Only app package names are checked during focus.",
                                    color = Color(0xFFE2E8F0),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionExplanationDialog = false
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19))
                    ) {
                        Text("Open Settings", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionExplanationDialog = false }) {
                        Text("Close", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@Composable
fun InstalledAppRow(
    app: InstalledAppInfo,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appIconBitmap = remember(app.packageName) {
        FocusShieldManager.getAppIconBitmap(context, app.packageName)
    }

    val categoryIcon = when (app.category.lowercase()) {
        "streaming", "shorts & videos" -> Icons.Filled.VideoLibrary
        "social media" -> Icons.Filled.CameraAlt
        "messaging" -> Icons.Filled.ChatBubble
        "gaming" -> Icons.Filled.SportsEsports
        "browsing" -> Icons.Filled.Public
        else -> Icons.Filled.Apps
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .testTag("app_row_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        fillAlpha = if (isChecked) 0.85f else 0.5f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Real App Icon or category fallback
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isChecked) Color(0x30F43F5E) else Color(0x18FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = if (isChecked) CoralRose else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isChecked) Color(0x30F43F5E) else Color(0x18FFFFFF)
                        ) {
                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isChecked) CoralRose else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = isChecked,
                onCheckedChange = { onCheckedChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = CoralRose,
                    uncheckedColor = Color(0xFF64748B),
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.testTag("checkbox_${app.packageName}")
            )
        }
    }
}
