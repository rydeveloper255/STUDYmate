package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.theme.*

@Composable
fun ForgotPasswordEmailScreen(
    initialEmail: String,
    isLoading: Boolean,
    errorMessage: String?,
    onSendOtp: (email: String) -> Unit,
    onBackToLogin: () -> Unit,
    onDismissError: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var localValidationErr by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        val clean = email.trim().lowercase()
        if (clean.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
            localValidationErr = "Kripya ek valid registered email address enter karein."
            return
        }
        localValidationErr = null
        onSendOtp(clean)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 24.dp)
            .testTag("forgot_password_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .testTag("forgot_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to login",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            StudyMateBrandLogo(
                size = 80.dp,
                showTypography = false,
                animated = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Forgot Password?",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Apna registered email enter karein. Ham aapko password reset OTP bhejenge.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 10.dp,
                fillAlpha = 0.70f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val activeError = localValidationErr ?: errorMessage
                    AnimatedVisibility(
                        visible = !activeError.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (!activeError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoralRose.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = CoralRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = activeError,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            localValidationErr = null
                                            onDismissError()
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = CoralRose,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            localValidationErr = null
                        },
                        label = { Text("Registered Email Address", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("name@example.com", color = Color(0x66FFFFFF)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = NeonCyan) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            submit()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x40FFFFFF),
                            focusedContainerColor = Color(0x1A0F172A),
                            unfocusedContainerColor = Color(0x1A0F172A)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    GlassButton(
                        text = "Send Recovery Code",
                        onClick = {
                            focusManager.clearFocus()
                            submit()
                        },
                        isLoading = isLoading,
                        loadingText = "Sending recovery code...",
                        icon = Icons.Outlined.Send,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "send_recovery_code_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.testTag("back_to_login_button")
            ) {
                Text(
                    text = "← Back to Log In",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ResetPasswordScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onResetPassword: (newPassword: String, confirmPassword: String) -> Unit,
    onBackToLogin: () -> Unit,
    onDismissError: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var localValidationErr by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    // Password strength calculation
    val hasMinLength = newPassword.length >= 8
    val hasDigit = newPassword.any { it.isDigit() }
    val hasUpperOrSpecial = newPassword.any { it.isUpperCase() } || newPassword.any { !it.isLetterOrDigit() }

    val strengthScore = when {
        newPassword.isEmpty() -> 0f
        newPassword.length < 6 -> 0.25f
        hasMinLength && hasDigit && hasUpperOrSpecial -> 1.0f
        hasMinLength && (hasDigit || hasUpperOrSpecial) -> 0.75f
        else -> 0.5f
    }

    val strengthLabel = when {
        newPassword.isEmpty() -> ""
        strengthScore <= 0.25f -> "Weak"
        strengthScore <= 0.5f -> "Fair"
        strengthScore <= 0.75f -> "Good"
        else -> "Strong"
    }

    val animatedStrengthProgress by animateFloatAsState(targetValue = strengthScore, label = "reset_strength_prog")
    val strengthColor by animateColorAsState(
        targetValue = when {
            strengthScore <= 0.25f -> CoralRose
            strengthScore <= 0.5f -> AmberGold
            strengthScore <= 0.75f -> NeonCyan
            else -> EmeraldGreen
        },
        label = "reset_strength_color"
    )

    val isPasswordMatch = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword == confirmPassword
    val isPasswordMismatch = confirmPassword.isNotEmpty() && newPassword != confirmPassword

    fun submit() {
        if (newPassword.length < 6) {
            localValidationErr = "Password kam se kam 6 characters ka hona chahiye (8+ recommended)."
            return
        }
        if (newPassword != confirmPassword) {
            localValidationErr = "Password aur Confirm Password match nahi kar rahe hain."
            return
        }
        localValidationErr = null
        onResetPassword(newPassword, confirmPassword)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 24.dp)
            .testTag("reset_password_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Set New Password",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            StudyMateBrandLogo(
                size = 80.dp,
                showTypography = false,
                animated = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secure Your Account",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Kripya ek naya strong password create karein.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 10.dp,
                fillAlpha = 0.70f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val activeError = localValidationErr ?: errorMessage
                    AnimatedVisibility(
                        visible = !activeError.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (!activeError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoralRose.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = CoralRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = activeError,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            localValidationErr = null
                                            onDismissError()
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = CoralRose,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // New Password Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                localValidationErr = null
                            },
                            label = { Text("New Password (6+ chars)", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = NeonCyan) },
                            trailingIcon = {
                                IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isNewPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_new_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x40FFFFFF),
                                focusedContainerColor = Color(0x1A0F172A),
                                unfocusedContainerColor = Color(0x1A0F172A)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Strength Meter
                        if (newPassword.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Strength: $strengthLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = strengthColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (hasMinLength) "✓ 8+ chars" else "Min 8 chars",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasMinLength) EmeraldGreen else Color(0xFF94A3B8)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0x33FFFFFF))
                                        .testTag("reset_strength_bar")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedStrengthProgress)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(strengthColor)
                                    )
                                }
                            }
                        }
                    }

                    // Confirm New Password Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                localValidationErr = null
                            },
                            label = { Text("Confirm New Password", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Outlined.LockClock, null, tint = NeonCyan) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPasswordMatch) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Matched",
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                submit()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_confirm_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (isPasswordMismatch) CoralRose else NeonCyan,
                                unfocusedBorderColor = if (isPasswordMismatch) CoralRose.copy(alpha = 0.6f) else Color(0x40FFFFFF),
                                focusedContainerColor = Color(0x1A0F172A),
                                unfocusedContainerColor = Color(0x1A0F172A)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        if (confirmPassword.isNotEmpty()) {
                            Text(
                                text = if (isPasswordMatch) "✓ Passwords match" else "Passwords do not match",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPasswordMatch) EmeraldGreen else CoralRose,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .testTag("reset_match_indicator")
                            )
                        }
                    }

                    GlassButton(
                        text = "Update Password",
                        onClick = {
                            focusManager.clearFocus()
                            submit()
                        },
                        isLoading = isLoading,
                        loadingText = "Updating password...",
                        icon = Icons.Outlined.CheckCircle,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "update_password_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
