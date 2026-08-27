package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun OtpVerificationScreen(
    email: String,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    cooldownSeconds: Int,
    isPasswordRecovery: Boolean = false,
    onVerifyOtp: (otp: String) -> Unit,
    onResendOtp: () -> Unit,
    onBackToPrevious: () -> Unit,
    onDismissError: () -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (ignored: Exception) {}
    }

    val minutes = cooldownSeconds / 60
    val seconds = cooldownSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val isTimerActive = cooldownSeconds > 0
    val timerProgress = (cooldownSeconds.toFloat() / 120f).coerceIn(0f, 1f)

    fun handlePaste() {
        val clipText = clipboardManager.getText()?.text ?: ""
        val digitsOnly = clipText.filter { it.isDigit() }.take(6)
        if (digitsOnly.isNotEmpty()) {
            otpValue = digitsOnly
            if (digitsOnly.length == 6) {
                focusManager.clearFocus()
                onVerifyOtp(digitsOnly)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 24.dp)
            .testTag("otp_verification_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Navigation bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToPrevious,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .testTag("otp_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isPasswordRecovery) "Recovery Verification" else "Email Verification",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logo & Icon
            StudyMateBrandLogo(
                size = 80.dp,
                showTypography = false,
                animated = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isPasswordRecovery) "Reset Password Code" else "Verify Your Email",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Hamne 6-digit verification code aapke email par send kiya hai:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Display Email in Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x330EA5E9),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkEmailRead,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = email.ifBlank { "your.email@domain.com" },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Liquid Glass Card for OTP Input
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 12.dp,
                fillAlpha = 0.75f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success or Error Feedback
                    AnimatedVisibility(
                        visible = !successMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (!successMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldGreen.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = successMessage,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !errorMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoralRose.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = CoralRose,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = errorMessage,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onDismissError,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = CoralRose,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enter 6-Digit Code",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Quick Paste Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x220EA5E9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clickable { handlePaste() }
                                .testTag("paste_otp_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "Paste OTP",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Paste Code",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Custom 6-Box OTP Input System
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Hidden BasicTextField that receives typing input & paste
                        BasicTextField(
                            value = otpValue,
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.filter { it.isDigit() }.take(6)
                                otpValue = digitsOnly
                                if (digitsOnly.length == 6) {
                                    focusManager.clearFocus()
                                    onVerifyOtp(digitsOnly)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (otpValue.length == 6) {
                                        onVerifyOtp(otpValue)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .size(1.dp)
                                .testTag("otp_hidden_input")
                        )

                        // 6 Visible Liquid-Glass Boxes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 6) {
                                val char = otpValue.getOrNull(i)?.toString() ?: ""
                                val isCurrent = i == otpValue.length
                                val isFilled = char.isNotEmpty()

                                val boxBorderBrush = when {
                                    isCurrent -> Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                                    isFilled -> Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.8f), NebulaPurple.copy(alpha = 0.8f)))
                                    else -> Brush.linearGradient(listOf(Color(0x30FFFFFF), Color(0x15FFFFFF)))
                                }

                                val boxBackground = when {
                                    isCurrent -> Color(0x330EA5E9)
                                    isFilled -> Color(0x281E293B)
                                    else -> Color(0x150F172A)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(46.dp, 54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(boxBackground)
                                        .border(
                                            width = if (isCurrent) 1.5.dp else 1.dp,
                                            brush = boxBorderBrush,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            try {
                                                focusRequester.requestFocus()
                                            } catch (ignored: Exception) {}
                                        }
                                        .testTag("otp_box_$i"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Verify Button
                    GlassButton(
                        text = "Verify & Continue",
                        onClick = {
                            focusManager.clearFocus()
                            if (otpValue.length == 6) {
                                onVerifyOtp(otpValue)
                            }
                        },
                        isLoading = isLoading,
                        enabled = otpValue.length == 6 && !isLoading,
                        loadingText = "Verifying code...",
                        icon = Icons.Outlined.VerifiedUser,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verify_otp_button"
                    )

                    // Countdown & Manual Resend Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isTimerActive) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x1A0EA5E9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { timerProgress },
                                        modifier = Modifier.size(16.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp,
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                    Text(
                                        text = "Resend code in $formattedTime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE2E8F0),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x260EA5E9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable(enabled = !isLoading) { onResendOtp() }
                                    .testTag("resend_otp_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Resend OTP",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Resend OTP",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back / Edit Email option
            TextButton(
                onClick = onBackToPrevious,
                modifier = Modifier.testTag("otp_change_email_button")
            ) {
                Text(
                    text = "Entered wrong email? Change email",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
