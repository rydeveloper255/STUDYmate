package com.example.ui.screens.auth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.FocusShieldManager
import com.example.service.focus.PermissionCheckStatus
import com.example.service.focus.PermissionHealthMonitor
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun PermissionSetupScreen(
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Track runtime permission states
    var isMicGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var isCameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var isNotificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var isStorageGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var isUsageAccessGranted by remember {
        mutableStateOf(PermissionHealthMonitor.checkUsageAccess(context) == PermissionCheckStatus.READY)
    }
    var isOverlayGranted by remember {
        mutableStateOf(PermissionHealthMonitor.checkOverlayPermission(context) == PermissionCheckStatus.READY)
    }

    // Refresh permission statuses on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isMicGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                isCameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isNotificationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    isStorageGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                } else {
                    isNotificationGranted = true
                    isStorageGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
                isUsageAccessGranted = PermissionHealthMonitor.checkUsageAccess(context) == PermissionCheckStatus.READY
                isOverlayGranted = PermissionHealthMonitor.checkOverlayPermission(context) == PermissionCheckStatus.READY
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Launchers
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isMicGranted = granted
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isCameraGranted = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
    }

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        isStorageGranted = perms.values.any { it }
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        isMicGranted = perms[Manifest.permission.RECORD_AUDIO] ?: isMicGranted
        isCameraGranted = perms[Manifest.permission.CAMERA] ?: isCameraGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotificationGranted = perms[Manifest.permission.POST_NOTIFICATIONS] ?: isNotificationGranted
            isStorageGranted = perms[Manifest.permission.READ_MEDIA_IMAGES] ?: isStorageGranted
        } else {
            isStorageGranted = perms[Manifest.permission.READ_EXTERNAL_STORAGE] ?: isStorageGranted
        }
    }

    val isProtectionReady = isUsageAccessGranted && isOverlayGranted
    val grantedCount = listOf(isMicGranted, isCameraGranted, isNotificationGranted, isStorageGranted, isProtectionReady).count { it }
    val totalCount = 5

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("permission_setup_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StudyMateBrandLogo(size = 56.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "App Setup & Permissions 🛡️",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Grant permissions below so NOVA Voice Companion, Focus Shield, and Doubt Solver can function seamlessly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Pill Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x2038BDF8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Setup Readiness",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$grantedCount of $totalCount Configured",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (grantedCount == totalCount) EmeraldSuccess else GoldenSpark
                                )
                            }

                            if (grantedCount < 4) {
                                Button(
                                    onClick = {
                                        val reqList = mutableListOf(
                                            Manifest.permission.RECORD_AUDIO,
                                            Manifest.permission.CAMERA
                                        )
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            reqList.add(Manifest.permission.POST_NOTIFICATIONS)
                                            reqList.add(Manifest.permission.READ_MEDIA_IMAGES)
                                        } else {
                                            reqList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                        multiPermissionLauncher.launch(reqList.toTypedArray())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("grant_all_basic_btn")
                                ) {
                                    Text("Grant Basic", color = Color(0xFF070B19), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 1. 🎙️ Microphone Permission Card
            item {
                PermissionItemCard(
                    title = "🎙️ Microphone Access",
                    description = "Enables voice tutoring with NOVA AI companion, speech recognition, and instant doubt asking.",
                    isGranted = isMicGranted,
                    statusText = if (isMicGranted) "Voice Ready" else "Microphone Disabled",
                    buttonText = "Allow Microphone",
                    icon = Icons.Filled.Mic,
                    accentColor = NeonCyan,
                    testTag = "perm_card_mic",
                    onAction = {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }

            // 2. 📷 Camera Permission Card
            item {
                PermissionItemCard(
                    title = "📷 Camera & Vision",
                    description = "Capture questions, textbook diagrams, and notes for instant AI step-by-step solutions.",
                    isGranted = isCameraGranted,
                    statusText = if (isCameraGranted) "Vision Ready" else "Camera Disabled",
                    buttonText = "Allow Camera",
                    icon = Icons.Filled.CameraAlt,
                    accentColor = ElectricViolet,
                    testTag = "perm_card_camera",
                    onAction = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }

            // 3. 🔔 Notification Permission Card
            item {
                PermissionItemCard(
                    title = "🔔 Study Reminders & Alerts",
                    description = "Stay on track with countdown alerts, scheduled study sessions, and focus break reminders.",
                    isGranted = isNotificationGranted,
                    statusText = if (isNotificationGranted) "Notifications Active" else "Notifications Off",
                    buttonText = "Allow Notifications",
                    icon = Icons.Filled.Notifications,
                    accentColor = GoldenSpark,
                    testTag = "perm_card_notifications",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            isNotificationGranted = true
                        }
                    }
                )
            }

            // 4. 📁 Files & Media Permission Card
            item {
                PermissionItemCard(
                    title = "📁 Document & Image Import",
                    description = "Import PDF study materials, formula sheets, and past question papers for AI analysis.",
                    isGranted = isStorageGranted,
                    statusText = if (isStorageGranted) "Storage Ready" else "Media Access Off",
                    buttonText = "Allow Media Access",
                    icon = Icons.Filled.Folder,
                    accentColor = Color(0xFF38BDF8),
                    testTag = "perm_card_storage",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            storageLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                        } else {
                            storageLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                        }
                    }
                )
            }

            // 5. 🎯 Focus Protection System Card (Accessibility-Free)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("perm_card_focus_protection"),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isProtectionReady) Color(0x2010B981) else Color(0x22F59E0B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isProtectionReady) EmeraldSuccess.copy(alpha = 0.5f) else GoldenSpark.copy(alpha = 0.5f)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isProtectionReady) EmeraldSuccess.copy(alpha = 0.2f) else GoldenSpark.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isProtectionReady) Icons.Filled.CheckCircle else Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = if (isProtectionReady) EmeraldSuccess else GoldenSpark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🎯 Focus Protection Engine 2.0",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isProtectionReady) "✓ Ready & Protecting" else "Setup Required (No Accessibility Needed)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isProtectionReady) EmeraldSuccess else GoldenSpark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Standard Android Usage Access detects when you open blocked apps during study sessions. Zero Accessibility service or keystroke monitoring required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sub-permission 1: Usage Access
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isUsageAccessGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (isUsageAccessGranted) EmeraldSuccess else GoldenSpark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("1. Usage Access", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            if (!isUsageAccessGranted) {
                                Button(
                                    onClick = { PermissionHealthMonitor.openUsageAccessSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("✓ Granted", fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub-permission 2: Display Over Other Apps (Overlay)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOverlayGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (isOverlayGranted) EmeraldSuccess else GoldenSpark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("2. Display Over Other Apps", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            if (!isOverlayGranted) {
                                Button(
                                    onClick = { PermissionHealthMonitor.openOverlaySettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSpark, contentColor = Color(0xFF070B19)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("✓ Granted", fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "🛡️ Zero-Interference Banking: UPI & Payment apps (Paytm, PhonePe, GPay) run with zero screen interception.",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Bottom Continue Section
            item {
                Spacer(modifier = Modifier.height(8.dp))

                GlassButton(
                    text = if (grantedCount == totalCount) "Launch StudyMate AI 🚀" else "Complete Setup & Continue",
                    onClick = onCompleteSetup,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isPrimary = true,
                    testTag = "complete_permission_setup_button"
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onCompleteSetup,
                        modifier = Modifier.testTag("skip_permission_setup_btn")
                    ) {
                        Text(
                            text = "Skip for now (configure later in Settings)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    statusText: String,
    buttonText: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onAction: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        fillAlpha = 0.78f
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) EmeraldSuccess.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Filled.Check else icon,
                            contentDescription = null,
                            tint = if (isGranted) EmeraldSuccess else accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGranted) EmeraldSuccess else Color(0xFF94A3B8),
                            fontWeight = if (isGranted) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSuccess.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "✓ Granted",
                            color = EmeraldSuccess,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                lineHeight = 17.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color(0xFF070B19)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
