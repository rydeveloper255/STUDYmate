package com.example.ui.screens.focus

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FocusShieldApp
import com.example.service.FocusShieldManager
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun FocusShieldSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isShieldEnabled by remember { mutableStateOf(FocusShieldManager.isShieldEnabled()) }
    var isAccessibilityGranted by remember { mutableStateOf(FocusShieldManager.isAccessibilityServiceEnabled(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All Apps") }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    // Re-check accessibility on refresh
    val allApps = remember(searchQuery, selectedCategory, isShieldEnabled) {
        val apps = FocusShieldManager.getAllApps()
        apps.filter { app ->
            val matchesCategory = selectedCategory == "All Apps" || app.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val categories = listOf("All Apps", "Social Media", "Streaming", "Messaging", "Gaming", "Browsing")

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
            contentPadding = PaddingValues(bottom = 80.dp)
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
                            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🛡️ Focus Shield",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Block distracting apps during study sessions",
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

            // Master Protection Toggle Card
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
                                    text = "Focus Shield Protection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isShieldEnabled) "Restricted apps will be blocked during focus timer" else "Shield is disabled",
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

            // Accessibility Status Banner
            item {
                val isServiceActive = FocusShieldManager.isAccessibilityServiceEnabled(context)
                isAccessibilityGranted = isServiceActive

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isServiceActive) Color(0x2010B981) else Color(0x25F59E0B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isServiceActive) Color(0x5010B981) else Color(0x60F59E0B)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isServiceActive) Icons.Filled.VerifiedUser else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (isServiceActive) EmeraldSuccess else GoldenSpark,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isServiceActive) "Accessibility Service Granted" else "Accessibility Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isServiceActive)
                                "Focus Shield is actively monitoring window changes to automatically block restricted app launches during focus sessions."
                            else
                                "To detect when restricted apps are launched, Android requires enabling the Focus Shield Accessibility Service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 18.sp
                        )

                        if (!isServiceActive) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback settings
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_accessibility_btn")
                            ) {
                                Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant Accessibility Permission in Settings", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Search & Category Filters Card
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
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search app name or package...", color = Color(0xFF64748B)) },
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
                                .testTag("search_app_input"),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Bulk Actions & Custom Add
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val pkgs = allApps.map { it.packageName }
                                        FocusShieldManager.setAllAppsRestricted(context, pkgs, true)
                                        searchQuery = searchQuery + " " // force compose trigger
                                        searchQuery = searchQuery.trim()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4038BDF8)),
                                    modifier = Modifier.height(34.dp).testTag("restrict_all_btn")
                                ) {
                                    Text("Restrict All", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val pkgs = allApps.map { it.packageName }
                                        FocusShieldManager.setAllAppsRestricted(context, pkgs, false)
                                        searchQuery = searchQuery + " "
                                        searchQuery = searchQuery.trim()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x30FFFFFF)),
                                    modifier = Modifier.height(34.dp).testTag("unrestrict_all_btn")
                                ) {
                                    Text("Clear All", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }

                            TextButton(
                                onClick = { showAddCustomDialog = true },
                                modifier = Modifier.height(34.dp).testTag("add_custom_app_btn")
                            ) {
                                Icon(Icons.Filled.Add, null, tint = GoldenSpark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom App", color = GoldenSpark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        text = "Apps (${allApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val restrictedCount = allApps.count { FocusShieldManager.isAppRestricted(it.packageName) }
                    Text(
                        text = "$restrictedCount Restricted",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // App Rows
            items(allApps, key = { it.packageName }) { app ->
                val isRestricted = FocusShieldManager.isAppRestricted(app.packageName)
                var localState by remember(app.packageName, isRestricted) { mutableStateOf(isRestricted) }

                AppShieldItemRow(
                    app = app,
                    isRestricted = localState,
                    onToggle = { newRestricted ->
                        localState = newRestricted
                        FocusShieldManager.setAppRestricted(context, app.packageName, newRestricted)
                    }
                )
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
                                text = "Protected Essential Whitelist",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Essential system tools (Phone Dialing, System Messages, Calculator, Alarm Clock, Device Settings, and StudyMate AI) are strictly whitelisted and can never be blocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Add Custom App Dialog
        if (showAddCustomDialog) {
            var customName by remember { mutableStateOf("") }
            var customPkg by remember { mutableStateOf("") }
            var customCat by remember { mutableStateOf("Social Media") }

            AlertDialog(
                onDismissRequest = { showAddCustomDialog = false },
                containerColor = Color(0xFF131C2E),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Apps, null, tint = GoldenSpark, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Custom App Restriction", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter the app name and package name (e.g. com.game.app) to add it to your Focus Shield restrictions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("App Name (e.g., Mobile Game)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        OutlinedTextField(
                            value = customPkg,
                            onValueChange = { customPkg = it },
                            label = { Text("Package Name (e.g., com.example.game)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customPkg.isNotBlank()) {
                                FocusShieldManager.addCustomApp(context, customName, customPkg, customCat)
                                showAddCustomDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Add Restriction", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@Composable
fun AppShieldItemRow(
    app: FocusShieldApp,
    isRestricted: Boolean,
    onToggle: (Boolean) -> Unit
) {
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
            .clickable { onToggle(!isRestricted) }
            .testTag("app_row_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        fillAlpha = if (isRestricted) 0.85f else 0.5f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isRestricted) Color(0x30F43F5E) else Color(0x20FFFFFF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = if (isRestricted) CoralRose else Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x20FFFFFF)
                        ) {
                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
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

            Switch(
                checked = isRestricted,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF070B19),
                    checkedTrackColor = CoralRose,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0x20FFFFFF)
                )
            )
        }
    }
}
