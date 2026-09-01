package com.example.ui.screens.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.service.focus.PermissionCheckStatus
import com.example.service.focus.PermissionHealthMonitor
import com.example.ui.components.springClickable
import com.example.ui.theme.*

enum class FocusSetupStep {
    FOCUS_PERMISSIONS,     // Screenshot 3: Background & Usage Access
    NOTIFICATION_PERMISSION, // Screenshot 4: Stay on Track
    ALL_SET_SUCCESS        // Screenshot 5: You're all set!
}

@Composable
fun PermissionSetupScreen(
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableStateOf(FocusSetupStep.FOCUS_PERMISSIONS) }

    // Hardware back navigation handler
    BackHandler(enabled = currentStep != FocusSetupStep.FOCUS_PERMISSIONS) {
        currentStep = when (currentStep) {
            FocusSetupStep.ALL_SET_SUCCESS -> FocusSetupStep.NOTIFICATION_PERMISSION
            FocusSetupStep.NOTIFICATION_PERMISSION -> FocusSetupStep.FOCUS_PERMISSIONS
            FocusSetupStep.FOCUS_PERMISSIONS -> FocusSetupStep.FOCUS_PERMISSIONS
        }
    }

    // Permission state trackers
    var isBackgroundGranted by remember {
        mutableStateOf(PermissionHealthMonitor.checkBackgroundOptimization(context) == PermissionCheckStatus.READY)
    }
    var isUsageAccessGranted by remember {
        mutableStateOf(PermissionHealthMonitor.checkUsageAccess(context) == PermissionCheckStatus.READY)
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

    // Live refresh when returning from System Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBackgroundGranted = PermissionHealthMonitor.checkBackgroundOptimization(context) == PermissionCheckStatus.READY
                isUsageAccessGranted = PermissionHealthMonitor.checkUsageAccess(context) == PermissionCheckStatus.READY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isNotificationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
        currentStep = FocusSetupStep.ALL_SET_SUCCESS
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .testTag("permission_setup_screen")
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "setup_step_transition"
        ) { step ->
            when (step) {
                FocusSetupStep.FOCUS_PERMISSIONS -> {
                    FocusPermissionsStepView(
                        isBackgroundGranted = isBackgroundGranted,
                        isUsageAccessGranted = isUsageAccessGranted,
                        onRequestBackground = {
                            PermissionHealthMonitor.openBatteryOptimizationSettings(context)
                        },
                        onRequestUsageAccess = {
                            PermissionHealthMonitor.openUsageAccessSettings(context)
                        },
                        onContinue = {
                            currentStep = FocusSetupStep.NOTIFICATION_PERMISSION
                        }
                    )
                }

                FocusSetupStep.NOTIFICATION_PERMISSION -> {
                    NotificationPermissionStepView(
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                isNotificationGranted = true
                                currentStep = FocusSetupStep.ALL_SET_SUCCESS
                            }
                        },
                        onSkip = {
                            currentStep = FocusSetupStep.ALL_SET_SUCCESS
                        }
                    )
                }

                FocusSetupStep.ALL_SET_SUCCESS -> {
                    AllSetSuccessStepView(
                        isBackgroundGranted = isBackgroundGranted,
                        isUsageAccessGranted = isUsageAccessGranted,
                        isNotificationGranted = isNotificationGranted,
                        onContinueToHome = onCompleteSetup
                    )
                }
            }
        }
    }
}

/**
 * Screenshot 3 Reference: "Set Up StudyMate Focus"
 */
@Composable
private fun FocusPermissionsStepView(
    isBackgroundGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onRequestBackground: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onContinue: () -> Unit
) {
    val canProceed = isUsageAccessGranted || isBackgroundGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { /* Menu */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                Text(
                    text = "StudyMate",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = NeonCyan
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E2D4D),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Title
            Text(
                text = "Set Up StudyMate Focus",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = NeonCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Two quick settings help Focus Mode work reliably to prevent distractions.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Background Access Card
            PermissionRequirementCard(
                icon = Icons.Filled.BatterySaver,
                title = "Background Access",
                description = "Allows StudyMate to reliably monitor your study sessions and maintain Focus Mode even when the screen is off or you switch apps briefly.",
                isEnabled = isBackgroundGranted,
                buttonText = if (isBackgroundGranted) "Settings" else "Allow Access",
                isPrimaryAction = !isBackgroundGranted,
                showArrow = !isBackgroundGranted,
                showGear = isBackgroundGranted,
                onAction = onRequestBackground,
                testTag = "perm_card_background"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Usage Access Card
            PermissionRequirementCard(
                icon = Icons.Filled.PhonelinkLock,
                title = "Usage Access",
                description = "Required to detect when distracting apps are opened so StudyMate can gently block them and redirect your attention back to your studies.",
                isEnabled = isUsageAccessGranted,
                buttonText = if (isUsageAccessGranted) "Enabled" else "Enable Usage Access",
                isPrimaryAction = false,
                showExternalIcon = !isUsageAccessGranted,
                onAction = onRequestUsageAccess,
                testTag = "perm_card_usage_access"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bottom Action CTA
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("continue_to_dashboard_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canProceed) NeonCyan else Color(0xFF162036),
                    contentColor = if (canProceed) Color(0xFF050B14) else Color(0xFF64748B),
                    disabledContainerColor = Color(0xFF162036),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (canProceed) "Continue" else "Continue to Dashboard",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (canProceed) Icons.AutoMirrored.Filled.ArrowForward else Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (canProceed) "Settings can be adjusted anytime in Focus preferences." else "Please enable required permissions to continue.",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Individual Permission Requirement Card with exact badge and styling
 */
@Composable
private fun PermissionRequirementCard(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    buttonText: String,
    isPrimaryAction: Boolean = false,
    showArrow: Boolean = false,
    showGear: Boolean = false,
    showExternalIcon: Boolean = false,
    onAction: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111C33),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Icon + Status Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Box (Teal background)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0E2A38),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF164E63)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Status Pill Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) Color(0xFF0D281E) else Color(0xFF2D1217),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isEnabled) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFFDC2626).copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) Color(0xFF34D399) else Color(0xFFF87171))
                        )
                        Text(
                            text = if (isEnabled) "Enabled" else "Not enabled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isEnabled) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Description
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF94A3B8)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            if (isPrimaryAction) {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF050B14)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        if (showArrow) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B273E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3B5D))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                        if (showExternalIcon) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showGear) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screenshot 4 Reference: "Stay on Track" (Notification Permission)
 */
@Composable
private fun NotificationPermissionStepView(
    onRequestNotification: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bell_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Glowing Orb Visual with Ringing Bell
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        spotColor = NeonCyan.copy(alpha = 0.5f * pulseGlow)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF122C4D),
                                Color(0xFF0C1933),
                                Color(0xFF060B18)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.8f * pulseGlow),
                                Color(0x30FFFFFF)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring arc
                Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.15f),
                        radius = size.minDimension / 2f
                    )
                }

                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = "Notifications",
                    tint = NeonCyan,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Title
            Text(
                text = "Stay on Track",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "StudyMate uses notifications to remind you of study sessions and keep you focused.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF94A3B8)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onRequestNotification,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("allow_notifications_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF050B14)
                )
            ) {
                Text(
                    text = "Allow Notifications",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("skip_notifications_btn")
            ) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}

/**
 * Screenshot 5 Reference: "You're all set!" (Final Success Screen)
 */
@Composable
private fun AllSetSuccessStepView(
    isBackgroundGranted: Boolean,
    isUsageAccessGranted: Boolean,
    isNotificationGranted: Boolean,
    onContinueToHome: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "check_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // Glowing Concentric Circle with Checkmark
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = CircleShape,
                        spotColor = NeonCyan.copy(alpha = 0.6f * pulseGlow)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5BF),
                                Color(0xFF00B4D8),
                                Color(0xFF0A1E38)
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.9f * pulseGlow),
                                NeonCyan.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color(0xFF050B14),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "You're all set!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = NeonCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "StudyMate Focus is ready.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    color = Color(0xFF94A3B8)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Checklist Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF111C33),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    VerifiedPermissionItem(
                        text = "Background access"
                    )

                    VerifiedPermissionItem(
                        text = "Usage access"
                    )

                    if (isNotificationGranted) {
                        VerifiedPermissionItem(
                            text = "Study notifications"
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onContinueToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("continue_to_studymate_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF050B14)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Continue to StudyMate",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerifiedPermissionItem(
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = NeonCyan.copy(alpha = 0.15f),
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Color.White
            )
        )
    }
}
