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
import androidx.compose.ui.graphics.Brush
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
fun SignUpScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignUp: (fullName: String, email: String, phone: String, pass: String, confirmPass: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onDismissError: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var localValidationErr by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Password strength calculation
    val hasMinLength = password.length >= 8
    val hasDigit = password.any { it.isDigit() }
    val hasUpperOrSpecial = password.any { it.isUpperCase() } || password.any { !it.isLetterOrDigit() }

    val strengthScore = when {
        password.isEmpty() -> 0f
        password.length < 6 -> 0.25f
        hasMinLength && hasDigit && hasUpperOrSpecial -> 1.0f
        hasMinLength && (hasDigit || hasUpperOrSpecial) -> 0.75f
        else -> 0.5f
    }

    val strengthLabel = when {
        password.isEmpty() -> ""
        strengthScore <= 0.25f -> "Weak"
        strengthScore <= 0.5f -> "Fair"
        strengthScore <= 0.75f -> "Good"
        else -> "Strong"
    }

    val animatedStrengthProgress by animateFloatAsState(targetValue = strengthScore, label = "strength_prog")
    val strengthColor by animateColorAsState(
        targetValue = when {
            strengthScore <= 0.25f -> CoralRose
            strengthScore <= 0.5f -> AmberGold
            strengthScore <= 0.75f -> NeonCyan
            else -> EmeraldGreen
        },
        label = "strength_color"
    )

    val isPasswordMatch = password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword
    val isPasswordMismatch = confirmPassword.isNotEmpty() && password != confirmPassword

    fun validateAndSubmit() {
        localValidationErr = null
        if (fullName.trim().isBlank() || fullName.trim().length < 2) {
            localValidationErr = "Kripya apna pura naam darj karein (minimum 2 characters)."
            return
        }
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            localValidationErr = "Kripya ek valid email address enter karein (jaise: student@example.com)."
            return
        }
        val cleanPhone = phone.filter { it.isDigit() }
        if (cleanPhone.length < 10) {
            localValidationErr = "Kripya valid 10-digit mobile number darj karein."
            return
        }
        if (password.length < 6) {
            localValidationErr = "Password kam se kam 6 characters ka hona chahiye (8+ recommended)."
            return
        }
        if (password != confirmPassword) {
            localValidationErr = "Password aur Confirm Password match nahi kar rahe hain."
            return
        }
        onSignUp(fullName.trim(), cleanEmail, cleanPhone, password, confirmPassword)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 24.dp)
            .testTag("signup_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Back button and Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .testTag("signup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to login",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand visual
            StudyMateBrandLogo(
                size = 80.dp,
                showTypography = false,
                animated = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Join StudyMate",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Your AI powered study mentor & smart companion",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Glass Container for Form
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 10.dp,
                fillAlpha = 0.70f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Error Notice
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

                    // Full Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            localValidationErr = null
                        },
                        label = { Text("Full Name", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. Rahul Sharma", color = Color(0x66FFFFFF)) },
                        leadingIcon = { Icon(Icons.Outlined.Person, null, tint = NeonCyan) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_full_name_input"),
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

                    // Email Address Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            localValidationErr = null
                        },
                        label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("name@example.com", color = Color(0x66FFFFFF)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = NeonCyan) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input"),
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

                    // Mobile Number Field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 10) {
                                phone = digitsOnly
                                localValidationErr = null
                            }
                        },
                        label = { Text("Mobile Number (10 digits)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. 9876543210", color = Color(0x66FFFFFF)) },
                        leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = NeonCyan) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_phone_input"),
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

                    // Password Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                localValidationErr = null
                            },
                            label = { Text("Password", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = NeonCyan) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_password_input"),
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

                        // Real-time Password Strength Meter
                        if (password.isNotEmpty()) {
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

                                // Strength Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0x33FFFFFF))
                                        .testTag("password_strength_bar")
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

                    // Confirm Password Field
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
                            label = { Text("Confirm Password", color = Color(0xFF94A3B8)) },
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
                                            contentDescription = "Toggle Confirm Password Visibility",
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
                                validateAndSubmit()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_confirm_password_input"),
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

                        // Live Match Status
                        if (confirmPassword.isNotEmpty()) {
                            Text(
                                text = if (isPasswordMatch) "✓ Passwords match" else "Passwords do not match",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPasswordMatch) EmeraldGreen else CoralRose,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .testTag("password_match_indicator")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Create Account Button
                    GlassButton(
                        text = "Create Account",
                        onClick = {
                            focusManager.clearFocus()
                            validateAndSubmit()
                        },
                        isLoading = isLoading,
                        loadingText = "Creating account & sending OTP...",
                        icon = Icons.Outlined.PersonAdd,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "signup_submit_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switch to Login Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag("navigate_login_button")
                ) {
                    Text(
                        text = "Log In",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
