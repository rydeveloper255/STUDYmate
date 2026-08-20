package com.example.ui.screens.focus

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun AccessibilityPrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler(enabled = true) {
        onBack()
    }

    var isAccessibilityGranted by remember {
        mutableStateOf(FocusShieldManager.isAccessibilityServiceEnabled(context))
    }
    val isSafetyModeEnabled by AccessibilitySafetyManager.isSafetyModeEnabled.collectAsState()
    val isAccessibilityPausedByUser by AccessibilitySafetyManager.isAccessibilityPausedByUser.collectAsState()
    val isInSensitiveAppMode by AccessibilitySafetyManager.isInSensitiveAppMode.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = FocusShieldManager.isAccessibilityServiceEnabled(context)
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
                            .testTag("accessibility_privacy_back_btn")
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
                            text = "Accessibility Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "How Nova Uses Accessibility & Privacy Guarantees",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
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

            // 1. Accessibility Status Banner
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isAccessibilityGranted && !isAccessibilityPausedByUser) EmeraldSuccess.copy(alpha = 0.5f) else GoldenSpark.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isAccessibilityGranted && !isAccessibilityPausedByUser) EmeraldSuccess.copy(alpha = 0.2f) else GoldenSpark.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isAccessibilityGranted) Icons.Filled.Shield else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (isAccessibilityGranted && !isAccessibilityPausedByUser) EmeraldSuccess else GoldenSpark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isAccessibilityGranted) "Service Active & Protected" else "Accessibility Disabled",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isAccessibilityPausedByUser) "Temporarily paused by user"
                                    else if (isInSensitiveAppMode) "SENSITIVE_APP_MODE (Passive)"
                                    else if (isAccessibilityGranted) "Safety Mode ON • Minimum Privilege"
                                    else "Tap to enable in Android Settings",
                                    fontSize = 12.sp,
                                    color = if (isInSensitiveAppMode) NeonCyan else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAccessibilityGranted && !isAccessibilityPausedByUser) EmeraldSuccess.copy(alpha = 0.2f) else Color(0xFF334155)
                        ) {
                            Text(
                                text = if (!isAccessibilityGranted) "OFF" else if (isAccessibilityPausedByUser) "PAUSED" else "ACTIVE",
                                color = if (!isAccessibilityGranted) Color(0xFF94A3B8) else if (isAccessibilityPausedByUser) GoldenSpark else EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 2. Clear Notice: Financial Data Protection
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1B4B).copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Zero Financial Data Access Guarantee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Nova's Accessibility Service NEVER accesses, reads, collects, stores, or transmits any financial data. Your passwords, PINs, OTPs, CVVs, card numbers, bank balances, and UPI details remain completely private and untouched.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // 3. How Nova Uses Accessibility Services
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null, tint = GoldenSpark, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "How Nova Uses Accessibility",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PrivacyBulletPoint(
                            title = "1. App Package Identification",
                            description = "Nova checks ONLY the package name of the active foreground app (e.g. com.google.android.youtube) during an active study timer."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PrivacyBulletPoint(
                            title = "2. Focus Shield Protection",
                            description = "If you open a distracting app you explicitly restricted (like Instagram or YouTube) during focus time, Focus Shield shows a soft study reminder."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PrivacyBulletPoint(
                            title = "3. SENSITIVE_APP_MODE",
                            description = "When you open Paytm, PhonePe, Google Pay, SBI, HDFC, or any banking/financial app, Nova enters SENSITIVE_APP_MODE — completely stopping all interaction and overlay logic."
                        )
                    }
                }
            }

            // 4. What Nova NEVER Does
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Block, null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Strict Security Boundaries (What We NEVER Do)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PrivacyBulletPoint(
                            title = "❌ No Reading Credentials or Screen Content",
                            description = "Nova does not inspect or record text inside password fields, payment screens, or OTP messages."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PrivacyBulletPoint(
                            title = "❌ No Financial Automation or Clicks",
                            description = "Nova never auto-clicks 'Pay', enters UPI PINs, approves transactions, or alters bank amounts."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PrivacyBulletPoint(
                            title = "❌ No Screen Recording or Screenshots",
                            description = "No background video recording, canvas capturing, or window recording is ever performed."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PrivacyBulletPoint(
                            title = "❌ No Cloud Transmission of Private Screen Data",
                            description = "Screen text is never sent to Gemini AI, Supabase, Serper, or analytics servers."
                        )
                    }
                }
            }

            // 5. User Controls: Pause & Settings
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "User Privacy Controls",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Accessibility Safety Mode",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Automatically pause Nova inside Paytm, PhonePe, Google Pay & Banking apps.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
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

                        Spacer(modifier = Modifier.height(14.dp))

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
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Android Settings", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PrivacyBulletPoint(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
