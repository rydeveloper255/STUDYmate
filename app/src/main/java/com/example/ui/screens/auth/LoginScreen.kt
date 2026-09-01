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
    val snackbarHostState = remember { SnackbarHostState() }

    var identifierInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var localValidationErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun submitLogin() {
        localValidationErr = null
        val cleanIdentifier = identifierInput.trim()
        if (cleanIdentifier.isBlank()) {
            localValidationErr = "Kripya Email ya Mobile Number enter karein."
            return
        }
        if (passwordInput.length < 6) {
            localValidationErr = "Password kam se kam 6 characters ka hona chahiye."
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
                Spacer(modifier = Modifier.height(28.dp))

                // Brand Header & Visual
                StudyMateBrandLogo(
                    size = 110.dp,
                    showTypography = true,
                    animated = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Glass Auth Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 12.dp,
                    fillAlpha = 0.75f
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Log in to access your study plans & AI tutor",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        // Success Feedback
                        AnimatedVisibility(
                            visible = !successMessage.isNullOrBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            if (!successMessage.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldGreen.copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
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

                        // Error Banner
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

                        // Email or Phone input field
                        OutlinedTextField(
                            value = identifierInput,
                            onValueChange = {
                                identifierInput = it
                                localValidationErr = null
                            },
                            label = { Text("Email Address / Mobile", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("name@example.com", color = Color(0x66FFFFFF)) },
                            leadingIcon = { Icon(Icons.Outlined.Person, null, tint = NeonCyan) },
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
                                unfocusedBorderColor = Color(0x40FFFFFF),
                                focusedContainerColor = Color(0x1A0F172A),
                                unfocusedContainerColor = Color(0x1A0F172A)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Password input field
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                localValidationErr = null
                            },
                            label = { Text("Password", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = NeonCyan) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle Password",
                                        tint = Color(0xFF94A3B8)
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
                                unfocusedBorderColor = Color(0x40FFFFFF),
                                focusedContainerColor = Color(0x1A0F172A),
                                unfocusedContainerColor = Color(0x1A0F172A)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Forgot password option
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = {
                                    onForgotPassword(identifierInput)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.testTag("forgot_password_link")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Primary Log In Button
                        GlassButton(
                            text = "Log In",
                            onClick = {
                                focusManager.clearFocus()
                                submitLogin()
                            },
                            icon = Icons.Outlined.Login,
                            isPrimary = true,
                            isLoading = isLoading,
                            loadingText = "Authenticating...",
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "submit_login_button"
                        )

                        // Divider with text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x33FFFFFF))
                            Text(
                                text = "  or continue with  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x33FFFFFF))
                        }

                        // Google Sign In
                        GlassButton(
                            text = "Continue with Google",
                            onClick = onGoogleSignIn,
                            icon = Icons.Filled.AccountCircle,
                            isPrimary = false,
                            isLoading = isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "google_signin_button"
                        )

                        // Guest Mode
                        OutlinedButton(
                            onClick = { if (!isLoading) onGuestSignIn() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("guest_signin_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF94A3B8),
                                disabledContentColor = Color(0x6694A3B8)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Text(
                                text = "Continue as Guest Scholar",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Up link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                    TextButton(
                        onClick = onNavigateToSignUp,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.testTag("navigate_signup_button")
                    ) {
                        Text(
                            text = "Sign Up",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Policy Disclaimer
            Text(
                text = "By continuing, you agree to the Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
