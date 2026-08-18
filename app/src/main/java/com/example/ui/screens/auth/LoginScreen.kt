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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StudyMateBrandLogo
import com.example.ui.components.springClickable
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: (email: String, pass: String) -> Unit,
    onGuestSignIn: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    // Display Snackbar and Toast feedback whenever a sign-in error occurs
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
        }
    }

    // Verify and listen to Firebase Auth currentUser changes in LoginScreen lifecycle
    DisposableEffect(Unit) {
        val auth = try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                com.google.firebase.auth.FirebaseAuth.getInstance()
            } else null
        } catch (e: Exception) {
            null
        }

        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                android.util.Log.d("LoginScreen", "FirebaseAuth StateListener detected currentUser: ${user.uid} (${user.email})")
            }
        }

        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(24.dp)
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Header & Visual
            StudyMateBrandLogo(
                size = 140.dp,
                showTypography = true,
                animated = true
            )

            // Glass Auth Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                elevation = 12.dp,
                fillAlpha = 0.75f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign in to elevate your learning",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Personalized revision, AI tutor & smart planner",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Human-readable Error Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (errorMessage != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .testTag("login_error_banner"),
                                shape = RoundedCornerShape(14.dp),
                                color = CoralRose.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = "Error",
                                        tint = CoralRose,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = errorMessage,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onDismissError,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss",
                                            tint = CoralRose,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Primary: Continue with Google
                    GlassButton(
                        text = "Continue with Google",
                        onClick = onGoogleSignIn,
                        icon = Icons.Filled.AccountCircle,
                        isPrimary = true,
                        isLoading = isLoading,
                        loadingText = "Authenticating with Google...",
                        testTag = "google_signin_button"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary: Continue with Email
                    GlassButton(
                        text = "Continue with Email",
                        onClick = { if (!isLoading) showEmailDialog = true },
                        icon = Icons.Outlined.Email,
                        isPrimary = false,
                        testTag = "email_signin_button"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tertiary: Continue as Guest
                    OutlinedButton(
                        onClick = { if (!isLoading) onGuestSignIn() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guest_signin_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF94A3B8),
                            disabledContentColor = Color(0x6694A3B8)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Text(
                            text = "Continue as Guest",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ℹ️ Guest progress is stored locally on this device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Policy Disclaimer
            Text(
                text = "By continuing, you agree to the Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Snackbar Host for Floating Alerts
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .testTag("login_snackbar_host"),
            snackbar = { data ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRose.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = CoralRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = data.visuals.message,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (data.visuals.actionLabel != null) {
                            TextButton(onClick = { data.dismiss() }) {
                                Text(
                                    text = data.visuals.actionLabel ?: "OK",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        )

        // Email / Password Modal Dialog
        if (showEmailDialog) {
            AlertDialog(
                onDismissRequest = { showEmailDialog = false },
                containerColor = Color(0xFF131C2E),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Email Authentication",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Enter your email & password to sign in or create an account.",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Outlined.Email, null, tint = NeonCyan) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x40FFFFFF)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = NeonCyan) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x40FFFFFF)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmailDialog = false
                            onEmailSignIn(emailInput, passwordInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF070B19)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_email_auth")
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmailDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}
