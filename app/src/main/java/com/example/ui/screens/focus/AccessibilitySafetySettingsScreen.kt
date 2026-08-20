package com.example.ui.screens.focus

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.AccessibilitySafetyManager
import com.example.service.FocusShieldManager
import com.example.service.PackageSafetyProfile
import com.example.service.SensitiveCategory
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun AccessibilitySafetySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler(enabled = true) {
        onBack()
    }

    var isAccessibilityServiceEnabled by remember {
        mutableStateOf(FocusShieldManager.isAccessibilityServiceEnabled(context))
    }
    val isSafetyModeEnabled by AccessibilitySafetyManager.isSafetyModeEnabled.collectAsState()
    val isAccessibilityPausedByUser by AccessibilitySafetyManager.isAccessibilityPausedByUser.collectAsState()
    val isInSensitiveAppMode by AccessibilitySafetyManager.isInSensitiveAppMode.collectAsState()
    val activeCategory by AccessibilitySafetyManager.activeCategory.collectAsState()

    var showPrivacyConsentDialog by remember { mutableStateOf(false) }
    var showAccessibilityPrivacyScreen by remember { mutableStateOf(false) }
    var showAddCustomPackageDialog by remember { mutableStateOf(false) }

    if (showAccessibilityPrivacyScreen) {
        AccessibilityPrivacyScreen(onBack = { showAccessibilityPrivacyScreen = false })
        return
    }
    var customPkgInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<SensitiveCategory?>(null) }

    val allProfiles = remember(selectedCategoryFilter) {
        AccessibilitySafetyManager.getAllSensitiveProfiles().filter {
            selectedCategoryFilter == null || it.category == selectedCategoryFilter
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityServiceEnabled = FocusShieldManager.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B19))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("accessibility_safety_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Accessibility & Safety",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Banking Compatibility & Privacy Rules",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                }

                IconButton(
                    onClick = { showAccessibilityPrivacyScreen = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("privacy_info_dialog_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Privacy Policy",
                        tint = NeonCyan
                    )
                }
            }
        },
        containerColor = Color(0xFF070B19)
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Accessibility & Safety Status Dashboard Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isAccessibilityServiceEnabled && !isAccessibilityPausedByUser) EmeraldSuccess.copy(alpha = 0.5f) else GoldenSpark.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isAccessibilityServiceEnabled && !isAccessibilityPausedByUser) EmeraldSuccess.copy(alpha = 0.2f) else GoldenSpark.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isAccessibilityServiceEnabled) Icons.Filled.Shield else Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = if (isAccessibilityServiceEnabled && !isAccessibilityPausedByUser) EmeraldSuccess else GoldenSpark,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (isAccessibilityServiceEnabled) "Accessibility Service: ON" else "Accessibility: OFF",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (isAccessibilityPausedByUser) "Paused by user"
                                        else if (isInSensitiveAppMode) "Active in ${activeCategory.displayName} Mode"
                                        else if (isAccessibilityServiceEnabled) "Active & Monitoring Distractions"
                                        else "Accessibility service currently unavailable.",
                                        fontSize = 12.sp,
                                        color = if (isInSensitiveAppMode) NeonCyan else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAccessibilityServiceEnabled && !isAccessibilityPausedByUser) EmeraldSuccess.copy(alpha = 0.2f) else Color(0xFF334155)
                            ) {
                                Text(
                                    text = if (!isAccessibilityServiceEnabled) "OFF" else if (isAccessibilityPausedByUser) "PAUSED" else "ACTIVE",
                                    color = if (!isAccessibilityServiceEnabled) Color(0xFF94A3B8) else if (isAccessibilityPausedByUser) GoldenSpark else EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isAccessibilityServiceEnabled) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Android Accessibility Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        AccessibilitySafetyManager.setAccessibilityPausedByUser(context, !isAccessibilityPausedByUser)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAccessibilityPausedByUser) EmeraldSuccess else Color(0xFF334155),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAccessibilityPausedByUser) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAccessibilityPausedByUser) "Resume Service" else "Pause Accessibility",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            })
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                        }
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Safety Mode Control Switch Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.VerifiedUser, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Accessibility Safety Mode",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sensitive apps ke andar Nova interaction automatically limited rahega.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = isSafetyModeEnabled,
                                onCheckedChange = { enabled ->
                                    AccessibilitySafetyManager.setSafetyModeEnabled(context, enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NeonCyan,
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }
                }
            }

            // 3. Banking & Payment Compatibility Warning Banner
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldenSpark.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Security Note",
                            tint = GoldenSpark,
                            modifier = Modifier.size(22.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Banking & Payment App Compatibility",
                                fontWeight = FontWeight.Bold,
                                color = GoldenSpark,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Some banking/payment apps (Paytm, PhonePe, Google Pay, YONO SBI) may limit certain features while an Accessibility Service is active because of their security policies. StudyMate automatically enters SENSITIVE_APP_MODE to remain passive, and you can tap 'Pause Accessibility' anytime if needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 4. Category Filter Chips & Add Custom App
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Protected Sensitive Categories",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )

                        TextButton(onClick = { showAddCustomPackageDialog = true }) {
                            Icon(Icons.Filled.Add, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add App", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedCategoryFilter == null) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCategoryFilter == null) NeonCyan else Color(0xFF334155)),
                            modifier = Modifier.clickable { selectedCategoryFilter = null }
                        ) {
                            Text(
                                text = "All (${AccessibilitySafetyManager.getAllSensitiveProfiles().size})",
                                color = if (selectedCategoryFilter == null) NeonCyan else Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        SensitiveCategory.entries.filter { it != SensitiveCategory.STUDY_ALLOWED }.take(3).forEach { cat ->
                            val count = AccessibilitySafetyManager.getAllSensitiveProfiles().count { it.category == cat }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedCategoryFilter == cat) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCategoryFilter == cat) NeonCyan else Color(0xFF334155)),
                                modifier = Modifier.clickable { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat }
                            ) {
                                Text(
                                    text = "${cat.displayName} ($count)",
                                    color = if (selectedCategoryFilter == cat) NeonCyan else Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Sensitive App Safety Profiles List
            items(allProfiles) { profile ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (profile.category) {
                                            SensitiveCategory.PAYMENTS -> Icons.Filled.AccountBalanceWallet
                                            SensitiveCategory.BANKING -> Icons.Filled.AccountBalance
                                            SensitiveCategory.FINANCIAL -> Icons.Filled.TrendingUp
                                            SensitiveCategory.PASSWORD_MANAGERS -> Icons.Filled.Key
                                            SensitiveCategory.AUTHENTICATION -> Icons.Filled.Lock
                                            else -> Icons.Filled.Security
                                        },
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = profile.appName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${profile.category.displayName} • Passive Mode",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "🛡️ Protected",
                                color = EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Privacy Explanation Modal
    if (showPrivacyConsentDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyConsentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nova Accessibility Privacy Guarantees", color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• Why Nova Needs Accessibility: To optionally detect when restricted distracting apps (YouTube, Instagram) are opened during active study sessions.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Text("• What Nova Accesses: Only the foreground app package name.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Text("• What Nova NEVER Collects: Zero passwords, zero PINs, zero OTPs, zero CVVs, zero card numbers, zero financial screens.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Text("• Sensitive App Safety: When Paytm, PhonePe, Google Pay, or banking apps open, Nova automatically enters SENSITIVE_APP_MODE (zero interaction, zero overlays).", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Text("• Emergency Control: You can pause or disable Accessibility anytime.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyConsentDialog = false }) {
                    Text("I Understand", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add Custom Sensitive Package Dialog
    if (showAddCustomPackageDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomPackageDialog = false },
            title = { Text("Add Custom Sensitive App", color = Color.White, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Enter the package name of the app to protect under SENSITIVE_APP_MODE (e.g., com.example.mybank):", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customPkgInput,
                        onValueChange = { customPkgInput = it },
                        placeholder = { Text("com.example.paymentapp", color = Color(0xFF64748B), fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customPkgInput.isNotBlank()) {
                            AccessibilitySafetyManager.addCustomSensitivePackage(context, customPkgInput.trim())
                            customPkgInput = ""
                        }
                        showAddCustomPackageDialog = false
                    }
                ) {
                    Text("Add App", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomPackageDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
