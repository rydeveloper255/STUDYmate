package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.components.StudyMateAppCardEmblem
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.AuthScreenState
import com.example.viewmodel.MainViewModel

@Composable
fun AuthScreenHost(
    viewModel: MainViewModel,
    onGuestSignIn: () -> Unit
) {
    val context = LocalContext.current
    val authNavState by viewModel.authNavState.collectAsState()
    val isLoading by viewModel.isAuthLoading.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()
    val successMessage by viewModel.authSuccessMessage.collectAsState()
    val pendingEmail by viewModel.pendingAuthEmail.collectAsState()
    val cooldownSeconds by viewModel.otpCooldownSeconds.collectAsState()

    when (authNavState) {
        AuthScreenState.LOGIN -> {
            LoginScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                successMessage = successMessage,
                onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                onEmailSignIn = { emailOrPhone, pass -> viewModel.signInWithEmailOrPhone(emailOrPhone, pass) },
                onGuestSignIn = onGuestSignIn,
                onNavigateToSignUp = { viewModel.setAuthNavState(AuthScreenState.SIGNUP) },
                onForgotPassword = {
                    viewModel.clearAuthMessages()
                    viewModel.setAuthNavState(AuthScreenState.FORGOT_PASSWORD)
                },
                onDismissError = { viewModel.clearAuthError() }
            )
        }
        AuthScreenState.SIGNUP -> {
            SignUpScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSignUp = { fullName, email, phone, pass, confirmPass ->
                    viewModel.startSignUp(fullName, email, phone, pass, confirmPass)
                },
                onNavigateToLogin = {
                    viewModel.clearAuthMessages()
                    viewModel.setAuthNavState(AuthScreenState.LOGIN)
                },
                onDismissError = { viewModel.clearAuthError() }
            )
        }
        AuthScreenState.OTP_VERIFICATION -> {
            OtpVerificationScreen(
                email = pendingEmail,
                isLoading = isLoading,
                errorMessage = errorMessage,
                successMessage = successMessage,
                cooldownSeconds = cooldownSeconds,
                isPasswordRecovery = false,
                onVerifyOtp = { otp -> viewModel.verifyEmailOtp(otp) },
                onResendOtp = { viewModel.resendEmailOtp(isRecovery = false) },
                onBackToPrevious = {
                    viewModel.clearAuthMessages()
                    viewModel.setAuthNavState(AuthScreenState.SIGNUP)
                },
                onDismissError = { viewModel.clearAuthError() }
            )
        }
        AuthScreenState.FORGOT_PASSWORD -> {
            ForgotPasswordEmailScreen(
                initialEmail = pendingEmail,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSendOtp = { email -> viewModel.startForgotPassword(email) },
                onBackToLogin = {
                    viewModel.clearAuthMessages()
                    viewModel.setAuthNavState(AuthScreenState.LOGIN)
                },
                onDismissError = { viewModel.clearAuthError() }
            )
        }
        AuthScreenState.FORGOT_PASSWORD_OTP -> {
            OtpVerificationScreen(
                email = pendingEmail,
                isLoading = isLoading,
                errorMessage = errorMessage,
                successMessage = successMessage,
                cooldownSeconds = cooldownSeconds,
                isPasswordRecovery = true,
                onVerifyOtp = { otp -> viewModel.verifyForgotPasswordOtp(otp) },
                onResendOtp = { viewModel.resendEmailOtp(isRecovery = true) },
                onBackToPrevious = {
                    viewModel.clearAuthMessages()
                    viewModel.setAuthNavState(AuthScreenState.FORGOT_PASSWORD)
                },
                onDismissError = { viewModel.clearAuthError() }
            )
        }
        AuthScreenState.RESET_PASSWORD -> {
            ResetPasswordScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onResetPassword = { newPass, confirmPass ->
                    viewModel.completePasswordReset(newPass, confirmPass)
                },
                onBackToLogin = {
                    viewModel.clearAuthMessages()
                    viewModel.setAuthNavState(AuthScreenState.LOGIN)
                },
                onDismissError = { viewModel.clearAuthError() }
            )
        }
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String? = null,
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: (email: String, pass: String) -> Unit,
    onEmailSignUp: (email: String, pass: String, name: String, examName: String) -> Unit = { _, _, _, _ -> },
    onGuestSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    onForgotPassword: (email: String) -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var identifierInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var localValidationErr by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun submitLogin() {
        localValidationErr = null
        val cleanIdentifier = identifierInput.trim()
        if (cleanIdentifier.isBlank()) {
            localValidationErr = "Please enter your Email."
            return
        }
        if (!cleanIdentifier.contains("@") && cleanIdentifier.length < 10) {
            localValidationErr = "Please enter a valid email address."
            return
        }
        if (passwordInput.length < 6) {
            localValidationErr = "Password must be at least 6 characters."
            return
        }
        onEmailSignIn(cleanIdentifier, passwordInput)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 24.dp)
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                // Real StudyMate App Emblem Card (Screenshot 1 Reference)
                StudyMateAppCardEmblem(
                    size = 86.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Header Titles
                Text(
                    text = "Welcome to StudyMate",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sign in to continue your journey.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Success Message Feedback
                AnimatedVisibility(
                    visible = !successMessage.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (!successMessage.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
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
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Error Feedback Banner
                val activeError = localValidationErr ?: errorMessage
                AnimatedVisibility(
                    visible = !activeError.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (!activeError.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("login_error_banner"),
                            shape = RoundedCornerShape(12.dp),
                            color = CoralRose.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "Error",
                                    tint = CoralRose,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = activeError,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
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
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Dismiss",
                                        tint = CoralRose,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Email Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Email",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = identifierInput,
                        onValueChange = {
                            identifierInput = it
                            localValidationErr = null
                        },
                        placeholder = {
                            Text(
                                text = "student@university.edu",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.MailOutline,
                                contentDescription = "Email",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF1E2D4D),
                            focusedContainerColor = Color(0xFF111C33),
                            unfocusedContainerColor = Color(0xFF111C33)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Password Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            localValidationErr = null
                        },
                        placeholder = {
                            Text(
                                text = "••••••••",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Password",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            submitLogin()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF1E2D4D),
                            focusedContainerColor = Color(0xFF111C33),
                            unfocusedContainerColor = Color(0xFF111C33)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 3. Forgot Password Link
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        ),
                        modifier = Modifier
                            .springClickable {
                                onForgotPassword(identifierInput)
                            }
                            .padding(vertical = 4.dp)
                            .testTag("forgot_password_link")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Primary Button: "Login ->"
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        submitLogin()
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_login_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF050B14),
                        disabledContainerColor = NeonCyan.copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFF050B14).copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF050B14),
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Login",
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

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Secondary Button: "Create Account"
                Surface(
                    onClick = { if (!isLoading) onNavigateToSignUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("navigate_signup_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF111C33),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223254))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Optional Quick Sign In (Google & Guest)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E2D4D))
                    Text(
                        text = "  or  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E2D4D))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Google",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = { if (!isLoading) onGuestSignIn() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("guest_signin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF94A3B8)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4D))
                    ) {
                        Text(
                            text = "Guest Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Policy Disclaimer
            Text(
                text = "By continuing, you agree to the Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
