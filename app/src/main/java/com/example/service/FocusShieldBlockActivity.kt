package com.example.service

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

class FocusShieldBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent?.getStringExtra("BLOCKED_PACKAGE") ?: ""

        setContent {
            StudyMateTheme(darkTheme = true) {
                FocusShieldBlockScreen(
                    blockedPackageName = blockedPackage,
                    onBackToStudy = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("NAVIGATE_TO", "FOCUS")
                        }
                        startActivity(intent)
                        finish()
                    },
                    onEndFocusSession = {
                        FocusShieldManager.endFocusSession()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("ACTION_END_FOCUS_FROM_BLOCK", true)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun FocusShieldBlockScreen(
    blockedPackageName: String = "",
    onBackToStudy: () -> Unit,
    onEndFocusSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subject by FocusShieldManager.currentSubject.collectAsStateWithLifecycle()
    val topic by FocusShieldManager.currentTopic.collectAsStateWithLifecycle()
    val remainingSecs by FocusShieldManager.remainingSeconds.collectAsStateWithLifecycle()

    var showEndConfirmDialog by remember { mutableStateOf(false) }

    val blockedAppName = remember(blockedPackageName) {
        if (blockedPackageName.isBlank()) "Restricted App"
        else FocusShieldManager.getAppNameForPackage(context, blockedPackageName)
    }

    val appIconBitmap = remember(blockedPackageName) {
        if (blockedPackageName.isNotBlank()) {
            FocusShieldManager.getAppIconBitmap(context, blockedPackageName)
        } else null
    }

    val minutes = remainingSecs / 60
    val secs = remainingSecs % 60
    val timeRemainingFormatted = String.format("%02d:%02d remaining", minutes, secs)

    val motivationalQuotes = remember {
        listOf(
            "📚 Your future self will thank you for studying now! 💙",
            "✨ Stay in the zone! Real mastery is built one focused session at a time.",
            "🎯 Distraction is temporary, but the knowledge you build lasts forever.",
            "💪 Small sacrifices today lead to huge results on exam day!",
            "🌟 You've got this! Protect your focus and keep learning."
        )
    }
    val currentQuote = remember { motivationalQuotes.first() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF060914),
                        Color(0xFF0D1527),
                        Color(0xFF191336)
                    )
                )
            )
            .padding(24.dp)
            .systemBarsPadding()
            .testTag("focus_shield_block_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield Glowing Icon with App Icon Badge
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color(0x25F43F5E))
                        .border(2.5.dp, Brush.linearGradient(listOf(CoralRose, NeonCyan)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Focus Shield",
                        tint = CoralRose,
                        modifier = Modifier.size(50.dp)
                    )
                }

                // Blocked app icon badge
                if (appIconBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = appIconBitmap.asImageBitmap(),
                            contentDescription = blockedAppName,
                            modifier = Modifier.size(26.dp).clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CoralRose.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "🛡️ FOCUS SHIELD ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoralRose,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$blockedAppName is Blocked! 🚫",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Motivational Message
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0x2038BDF8),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4038BDF8))
            ) {
                Text(
                    text = currentQuote,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Task Active Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.8f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ONGOING FOCUS SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = subject,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = topic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x3038BDF8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5038BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeRemainingFormatted,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Action Buttons
            GlassButton(
                text = "Back to Study",
                onClick = onBackToStudy,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                isPrimary = true,
                testTag = "block_back_to_study_btn"
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showEndConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("block_end_session_btn"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))
            ) {
                Text(
                    text = "End Focus Session",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showEndConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEndConfirmDialog = false },
                containerColor = Color(0xFF131C2E),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "End Focus Session?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "You still have time remaining on your focus countdown. Are you sure you want to exit early?",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEndConfirmDialog = false
                            onEndFocusSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                    ) {
                        Text("End Session", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndConfirmDialog = false }) {
                        Text("Keep Studying", color = NeonCyan)
                    }
                }
            )
        }
    }
}
