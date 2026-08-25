package com.example.ui.screens.focus

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FocusPolicy
import com.example.data.model.FocusPreset
import com.example.data.model.FocusProtectionStatus
import com.example.service.FocusShieldManager
import com.example.service.InstalledAppInfo
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusShieldSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(enabled = true) {
        onBack()
    }

    var isShieldEnabled by remember { mutableStateOf(FocusShieldManager.isShieldEnabled()) }
    var isUsageAccessGranted by remember { mutableStateOf(FocusShieldManager.isUsageAccessGranted(context)) }

    val policy by FocusShieldManager.focusPolicy.collectAsStateWithLifecycle()
    val protectionStatus by FocusShieldManager.protectionStatus.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isUsageAccessGranted = FocusShieldManager.isUsageAccessGranted(context)
                FocusShieldManager.updateProtectionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Presets, 1: Apps, 2: Websites & Content, 3: Essential Apps
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppCategory by remember { mutableStateOf("All Apps") }
    var isLoadingApps by remember { mutableStateOf(true) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var newWebsiteInput by remember { mutableStateOf("") }
    var showExplanationDialog by remember { mutableStateOf(false) }

    // Load installed apps asynchronously from PackageManager
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = FocusShieldManager.loadInstalledApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
                isLoadingApps = false
            }
        }
    }

    val categories = listOf("All Apps", "Social Media", "Streaming", "Shorts & Videos", "Messaging", "Gaming", "Browsing")

    val filteredApps = remember(installedApps, searchQuery, selectedAppCategory, policy.blockedPackages) {
        installedApps.filter { app ->
            val matchesCategory = selectedAppCategory == "All Apps" || app.category.equals(selectedAppCategory, ignoreCase = true)
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
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x20FFFFFF))
                        .testTag("back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Distraction Shield 2.0",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Policy-based study protection (UPI & Banking safe)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Switch(
                    checked = isShieldEnabled,
                    onCheckedChange = { enabled ->
                        isShieldEnabled = enabled
                        FocusShieldManager.setShieldFeatureEnabled(context, enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0F172A),
                        checkedTrackColor = NeonCyan,
                        uncheckedTrackColor = Color(0x30FFFFFF)
                    ),
                    modifier = Modifier.testTag("master_shield_switch")
                )
            }

            // Honest Status Indicator Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                fillAlpha = 0.35f
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (protectionStatus) {
                                FocusProtectionStatus.PROTECTION_ACTIVE -> EmeraldSuccess.copy(alpha = 0.2f)
                                FocusProtectionStatus.GENTLE_FOCUS -> GoldenSpark.copy(alpha = 0.2f)
                                FocusProtectionStatus.NEEDS_ATTENTION -> CoralPink.copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = protectionStatus.icon, fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = protectionStatus.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (protectionStatus) {
                                    FocusProtectionStatus.PROTECTION_ACTIVE -> EmeraldSuccess
                                    FocusProtectionStatus.GENTLE_FOCUS -> GoldenSpark
                                    FocusProtectionStatus.NEEDS_ATTENTION -> CoralPink
                                }
                            )
                            Text(
                                text = protectionStatus.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        if (!isUsageAccessGranted) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricViolet,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("grant_usage_btn")
                            ) {
                                Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0x15FFFFFF))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ UPI & Banking Protection: ALWAYS ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )

                        Text(
                            text = "Learn How",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricViolet,
                            modifier = Modifier.clickable { showExplanationDialog = true }
                        )
                    }
                }
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Presets", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Apps (${policy.blockedPackages.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Websites & Content", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Essential Apps", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> PresetsTab(
                    currentPreset = policy.activePreset,
                    onSelectPreset = { preset -> FocusShieldManager.applyPreset(context, preset) }
                )
                1 -> AppsTab(
                    installedApps = filteredApps,
                    isLoading = isLoadingApps,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    categories = categories,
                    selectedCategory = selectedAppCategory,
                    onSelectCategory = { selectedAppCategory = it },
                    blockedPackages = policy.blockedPackages,
                    onToggleApp = { pkg, block -> FocusShieldManager.setAppRestricted(context, pkg, block) },
                    onSelectAll = { pkgs -> FocusShieldManager.selectAllApps(context, pkgs) },
                    onDeselectAll = { pkgs -> FocusShieldManager.deselectAllApps(context, pkgs) }
                )
                2 -> WebsitesAndContentTab(
                    policy = policy,
                    onToggleCategory = { id, enabled -> FocusShieldManager.toggleWebsiteCategory(context, id, enabled) },
                    onAddWebsite = { domain -> FocusShieldManager.addBlockedWebsite(context, domain) },
                    onRemoveWebsite = { domain -> FocusShieldManager.removeBlockedWebsite(context, domain) },
                    onToggleShorts = { blocked -> FocusShieldManager.setShortsBlocking(context, blocked) },
                    onToggleReels = { blocked -> FocusShieldManager.setReelsBlocking(context, blocked) },
                    onToggleStudyMode = { enabled -> FocusShieldManager.setStudyModeContentFilter(context, enabled) }
                )
                3 -> EssentialAppsTab()
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text("Zero-Interference Guarantee", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("StudyMate Distraction Shield 2.0 is built on non-invasive Android principles:")
                    Text("• Zero Accessibility Service requirement by default.")
                    Text("• Hardcoded payment & UPI app whitelist (Google Pay, PhonePe, Paytm, BHIM, Banking apps).")
                    Text("• Authentic background monitoring using Android UsageStatsManager.")
                    Text("• Student always retains immediate manual unlock capability.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showExplanationDialog = false }) {
                    Text("Got It", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun PresetsTab(
    currentPreset: FocusPreset,
    onSelectPreset: (FocusPreset) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Select a Preset Policy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        val presets = listOf(
            FocusPreset.DEEP_STUDY,
            FocusPreset.MOCK_TEST,
            FocusPreset.RESEARCH,
            FocusPreset.LIGHT_FOCUS,
            FocusPreset.CUSTOM
        )

        items(presets) { preset ->
            val isSelected = currentPreset == preset
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPreset(preset) }
                    .testTag("preset_card_${preset.name}"),
                shape = RoundedCornerShape(16.dp),
                borderColor = if (isSelected) NeonCyan else Color(0x20FFFFFF),
                fillAlpha = if (isSelected) 0.35f else 0.15f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x15FFFFFF),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = preset.badgeIcon, fontSize = 22.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonCyan else Color.White
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectPreset(preset) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = NeonCyan,
                            unselectedColor = Color(0x50FFFFFF)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AppsTab(
    installedApps: List<InstalledAppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    blockedPackages: Set<String>,
    onToggleApp: (String, Boolean) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onDeselectAll: (List<String>) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("app_search_input"),
            placeholder = { Text("Search apps...", color = Color(0x60FFFFFF)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x30FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        // Category Chips Row
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = NeonCyan,
            edgePadding = 0.dp,
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) NeonCyan else Color(0x15FFFFFF),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clickable { onSelectCategory(cat) }
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Action Buttons: Select All / Deselect All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${installedApps.size} apps displayed",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onSelectAll(installedApps.map { it.packageName }) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Block All", color = CoralPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { onDeselectAll(installedApps.map { it.packageName }) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Allow All", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    val isBlocked = blockedPackages.contains(app.packageName)
                    val isEssential = app.isEssential

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isEssential) { onToggleApp(app.packageName, !isBlocked) },
                        shape = RoundedCornerShape(12.dp),
                        borderColor = if (isBlocked) CoralPink.copy(alpha = 0.5f) else Color(0x15FFFFFF),
                        fillAlpha = if (isBlocked) 0.25f else 0.1f
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(app.packageName) {
                                FocusShieldManager.getAppIconBitmap(context, app.packageName)
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x20FFFFFF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(app.appName.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isEssential) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldSuccess.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "WHITELISTED",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = EmeraldSuccess,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${app.category} • ${app.packageName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            }

                            if (isEssential) {
                                Icon(Icons.Filled.Lock, contentDescription = "Protected", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            } else {
                                Switch(
                                    checked = isBlocked,
                                    onCheckedChange = { onToggleApp(app.packageName, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CoralPink,
                                        uncheckedTrackColor = Color(0x30FFFFFF)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebsitesAndContentTab(
    policy: FocusPolicy,
    onToggleCategory: (String, Boolean) -> Unit,
    onAddWebsite: (String) -> Unit,
    onRemoveWebsite: (String) -> Unit,
    onToggleShorts: (Boolean) -> Unit,
    onToggleReels: (Boolean) -> Unit,
    onToggleStudyMode: (Boolean) -> Unit
) {
    var websiteInput by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Content Feature Toggles
        item {
            Text(
                text = "Short-Form Distraction Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                fillAlpha = 0.2f
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "▶️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block YouTube Shorts", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Redirects away from Shorts feed loop", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = policy.blockYouTubeShorts,
                            onCheckedChange = onToggleShorts,
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonCyan)
                        )
                    }

                    HorizontalDivider(color = Color(0x15FFFFFF))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📸", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Instagram Reels", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Restricts infinite scroll video tab", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = policy.blockInstagramReels,
                            onCheckedChange = onToggleReels,
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonCyan)
                        )
                    }

                    HorizontalDivider(color = Color(0x15FFFFFF))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎓", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Study-Only Content Mode", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Prioritizes educational portals, Wikipedia & Mock tests", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = policy.studyModeContentFilter,
                            onCheckedChange = onToggleStudyMode,
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonCyan)
                        )
                    }
                }
            }
        }

        // Predefined Website Categories
        item {
            Text(
                text = "Predefined Website Categories",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(FocusShieldManager.PREDEFINED_WEBSITE_CATEGORIES) { cat ->
            val isBlocked = policy.activeWebsiteCategoryIds.contains(cat.id)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                fillAlpha = if (isBlocked) 0.25f else 0.1f,
                borderColor = if (isBlocked) CoralPink.copy(alpha = 0.4f) else Color(0x15FFFFFF)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = cat.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.name, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text(cat.description, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isBlocked,
                        onCheckedChange = { onToggleCategory(cat.id, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralPink,
                            uncheckedTrackColor = Color(0x30FFFFFF)
                        )
                    )
                }
            }
        }

        // Custom Domain Blocking
        item {
            Text(
                text = "Custom Blocked Domains",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = websiteInput,
                    onValueChange = { websiteInput = it },
                    placeholder = { Text("e.g. reddit.com", color = Color(0x60FFFFFF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x30FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (websiteInput.isNotBlank()) {
                            onAddWebsite(websiteInput)
                            websiteInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0F172A)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(policy.blockedWebsites.toList()) { domain ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                fillAlpha = 0.15f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(domain, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onRemoveWebsite(domain) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = CoralPink, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EssentialAppsTab() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                borderColor = EmeraldSuccess.copy(alpha = 0.4f),
                fillAlpha = 0.2f
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Permanent Protection Whitelist",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To guarantee zero disruption to essential student emergencies and banking operations, the following categories are permanently protected from being blocked by Focus Mode:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        val whitelistCategories = listOf(
            Triple("💳 Payments & UPI Apps", "Google Pay, PhonePe, Paytm, BHIM UPI, MobiKwik, Cred, Amazon Pay", "Transactions and fee payments never interrupted"),
            Triple("🏦 Net Banking & Savings", "SBI YONO, HDFC Mobile, ICICI iMobile, Axis, Kotak, PNB, Canara", "Emergency fund transfers and balance checks"),
            Triple("📞 Phone & Emergency Comms", "Phone, Contacts, System Dialer, Emergency SOS", "Incoming emergency calls and communications"),
            Triple("⏰ System Utility & Alarms", "Clock, Alarm, Calculator, Calendar, StudyMate App", "Essential exam tools and timekeeping"),
            Triple("🔑 2FA & Password Managers", "Bitwarden, 1Password, Google Authenticator, Microsoft Auth", "Secure login credentials for exam portals")
        )

        items(whitelistCategories) { (title, examples, desc) ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                fillAlpha = 0.15f
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = examples, color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = desc, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                }
            }
        }
    }
}
