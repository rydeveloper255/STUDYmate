package com.example.ui.components.test

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Reusable Smart Orientation Gate for CBT Exams, Practice Drills, PYQ, and Mock Tests.
 * Ensures the user's phone is rotated and locked to Landscape mode before starting
 * any CBT test, preventing false crashes, layout glitches, or early timer depletion.
 */
@Composable
fun TestOrientationGate(
    testTitle: String,
    totalQuestions: Int,
    durationMinutes: Int,
    examName: String = "",
    subjectName: String = "",
    onOrientationConfirmed: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp > configuration.screenHeightDp

    var isRotationLocked by remember { mutableStateOf(isSystemRotationLocked(context)) }
    var hasRequestedRotation by remember { mutableStateOf(false) }
    var isPreparingTest by remember { mutableStateOf(false) }

    // Check rotation lock status on resume (e.g. after returning from Display Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isRotationLocked = isSystemRotationLocked(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept Back Press: Cancel test launch and restore normal portrait/unspecified orientation
    BackHandler(enabled = true) {
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } catch (_: Exception) {}
        onCancel()
    }

    // When Landscape is verified by the device configuration:
    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            isPreparingTest = true
            // Lock screen firmly to landscape mode for active CBT exam
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } catch (_: Exception) {}
            delay(350L) // Brief smooth layout settle
            onOrientationConfirmed()
        }
    }

    // Rotating Phone Infinite Animation
    val infiniteTransition = rememberInfiniteTransition(label = "rotation_pulse")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2600
                0f at 0
                0f at 600
                90f at 1600
                90f at 2200
                90f at 2600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "phone_rotate"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("test_orientation_gate"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background gradient orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.12f * glowAlpha), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricViolet.copy(alpha = 0.10f * glowAlpha), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.7f),
                    radius = size.width * 0.5f
                )
            )
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    try {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } catch (_: Exception) {}
                    onCancel()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x1AFFFFFF), CircleShape)
                    .testTag("gate_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cancel & Return",
                    tint = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x1838BDF8),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(EmeraldSuccess, CircleShape)
                    )
                    Text(
                        text = "CBT Test Environment",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                }
            }
        }

        // Main Center Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (isPreparingTest) {
                // Preparing / Transitioning State
                CircularProgressIndicator(
                    color = NeonCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Preparing your test…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Configuring landscape CBT workspace & timer…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            } else {
                // Animated Phone Graphic Container
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(ringScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glowing rotation guide track
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    NeonCyan.copy(alpha = 0.6f * glowAlpha),
                                    ElectricViolet.copy(alpha = 0.8f * glowAlpha),
                                    EmeraldSuccess.copy(alpha = 0.4f * glowAlpha),
                                    NeonCyan.copy(alpha = 0.6f * glowAlpha)
                                )
                            ),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Rotating Phone Mockup
                    Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 110.dp)
                            .rotate(animatedAngle)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    listOf(NeonCyan, ElectricViolet)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Phone screen notch & UI representation
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Speaker / Notch
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(3.dp)
                                    .background(Color(0x66FFFFFF), RoundedCornerShape(2.dp))
                            )

                            // Center screen graphic
                            Icon(
                                imageVector = Icons.Filled.ScreenRotation,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )

                            // Bottom Navigation Indicator
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.5.dp)
                                    .background(Color(0x44FFFFFF), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }

                // Heading & Instructions
                Text(
                    text = "Rotate Your Screen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Please rotate your phone to landscape mode to start the test.\nLandscape mode is required for a better CBT test experience.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Test Information Summary Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x15FFFFFF),
                    border = BorderStroke(1.dp, Color(0x20FFFFFF)),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Questions",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$totalQuestions MCQs",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color(0x22FFFFFF))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Duration",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$durationMinutes Mins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        if (examName.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color(0x22FFFFFF))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Target",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = examName.take(12),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet
                                )
                            }
                        }
                    }
                }

                // Primary Rotate Button
                Button(
                    onClick = {
                        hasRequestedRotation = true
                        isRotationLocked = isSystemRotationLocked(context)
                        try {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(52.dp)
                        .testTag("rotate_screen_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF0F172A)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rotate Your Screen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Rotation Lock Warning & Setting Guide
                if (isRotationLocked || (hasRequestedRotation && !isLandscape)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberWarning.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Screen rotation is locked",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AmberWarning
                                )
                            }

                            Text(
                                text = "Please turn off Screen Rotation Lock in your device settings to continue to the test.",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )

                            OutlinedButton(
                                onClick = { openRotationSettings(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("open_rotation_settings_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AmberWarning),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AmberWarning
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open Rotation Settings",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Checks whether Android system auto-rotation is locked.
 */
fun isSystemRotationLocked(context: Context): Boolean {
    return try {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            1
        ) == 0
    } catch (_: Exception) {
        false
    }
}

/**
 * Opens system Auto-Rotate or Display settings.
 */
fun openRotationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_AUTO_ROTATE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

/**
 * Finds the parent Activity from a given Context.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
