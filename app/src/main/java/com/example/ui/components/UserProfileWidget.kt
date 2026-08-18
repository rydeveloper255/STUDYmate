package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.theme.*

@Composable
fun UserProfileWidget(
    user: UserProfile?,
    onSignOut: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()
    val isGoogleUser = user?.isGuest == false && !user?.email.isNullOrBlank()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_user_profile_widget"),
        shape = RoundedCornerShape(22.dp),
        elevation = 8.dp,
        fillAlpha = 0.72f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: User Avatar & Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with Google verification badge ring
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(NeonCyan, ElectricViolet, NebulaPurple, NeonCyan)
                                )
                            )
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user?.photoUrl,
                                    contentDescription = "User Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .testTag("user_avatar_image")
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "User Avatar",
                                    tint = if (isDark) NeonCyan else Color(0xFF0284C7),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    // Small Verified Badge Dot
                    if (isGoogleUser) {
                        Surface(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd),
                            shape = CircleShape,
                            color = EmeraldSuccess,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isDark) Color(0xFF070B19) else Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // Name, Email, and Google Verification Pill
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = user?.name?.ifBlank { "Student Scholar" } ?: "Student Scholar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!user?.email.isNullOrBlank()) {
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Verification Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isGoogleUser) EmeraldSuccess.copy(alpha = 0.16f) else Color(0x20FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            0.75.dp,
                            if (isGoogleUser) EmeraldSuccess.copy(alpha = 0.5f) else Color(0x30FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isGoogleUser) Icons.Outlined.VerifiedUser else Icons.Filled.Shield,
                                contentDescription = null,
                                tint = if (isGoogleUser) EmeraldSuccess else GoldenSpark,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isGoogleUser) "Google Verified" else "Guest Mode",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isGoogleUser) EmeraldSuccess else GoldenSpark,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Actions (Settings & Sign Out)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Edit / Settings icon button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x2038BDF8) else Color(0x150284C7))
                        .border(1.dp, if (isDark) Color(0x4038BDF8) else Color(0x300284C7), CircleShape)
                        .testTag("widget_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile",
                        tint = if (isDark) NeonCyan else Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Sign Out icon button
                IconButton(
                    onClick = { showSignOutConfirm = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x25F43F5E) else Color(0x15F43F5E))
                        .border(1.dp, CoralRose.copy(alpha = 0.45f), CircleShape)
                        .testTag("widget_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = CoralRose,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Sign Out Confirmation Modal
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = if (isDark) Color(0xFF131C2E) else Color.White,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Sign Out?",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out from ${user?.email ?: "this account"}? You will return to the sign-in screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(
                        text = "Cancel",
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        )
    }
}
