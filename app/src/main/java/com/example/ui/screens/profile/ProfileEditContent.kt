package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.PersistenceStatusIndicator
import com.example.ui.theme.*

private val AVATAR_PRESETS = listOf(
    "👨‍🎓", "👩‍🎓", "🧑‍🔬", "👩‍💻", "👨‍🏫", "🦉", "🚀", "⚡", "🎯", "🧠", "💡", "🌟"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditContent(
    user: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onCancel: () -> Unit,
    onOpenExamSwitcher: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var displayName by remember(user) { mutableStateOf(user?.name ?: "") }
    var selectedAvatar by remember(user) { mutableStateOf(user?.photoUrl ?: "👨‍🎓") }
    var selectedLanguage by remember(user) { mutableStateOf(user?.languagePreference ?: "English") }
    var dailyStudyHours by remember(user) { mutableFloatStateOf(user?.availableStudyHours ?: 4.0f) }
    var targetScoreText by remember(user) { mutableStateOf(user?.targetScore ?: "Top 500 AIR / 99%ile") }
    var isSaving by remember { mutableStateOf(false) }

    val activePersistenceStatus by com.example.data.persistence.PersistenceMonitor.activeStatus.collectAsState()

    val availableLanguages = listOf("English", "Hindi", "Hinglish", "Marathi", "Tamil", "Telugu", "Bengali")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("profile_edit_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (displayName.trim().isEmpty()) {
                                Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSaving = true
                            val updated = (user ?: UserProfile()).copy(
                                name = displayName.trim(),
                                photoUrl = selectedAvatar,
                                languagePreference = selectedLanguage,
                                availableStudyHours = dailyStudyHours,
                                targetScore = targetScoreText
                            )
                            onSave(updated)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (isSaving) "Saving..." else "Save Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090D16)
                )
            )
        },
        containerColor = Color(0xFF090D16)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Save status indicator
            PersistenceStatusIndicator(
                status = activePersistenceStatus,
                testTagPrefix = "profile_edit"
            )

            // Avatar Preview & Selector Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.6f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricViolet, NeonCyan)
                                )
                            )
                            .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedAvatar.length <= 4) selectedAvatar else "👨‍🎓",
                            fontSize = 42.sp
                        )
                    }

                    Text(
                        text = "Choose Avatar Preset",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(AVATAR_PRESETS) { emoji ->
                            val isSelected = selectedAvatar == emoji
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) ElectricViolet.copy(alpha = 0.4f)
                                        else Color(0xFF1E293B)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatar = emoji }
                                    .testTag("avatar_option_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }

            // Display Name Input Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.6f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("Enter your full name") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = NeonCyan)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("display_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Email display (read-only in this card, editable in Security)
                    OutlinedTextField(
                        value = user?.email?.takeIf { it.isNotBlank() } ?: "Guest User",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Account Email") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                        },
                        trailingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = "Managed in Security", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.White.copy(alpha = 0.7f),
                            disabledBorderColor = Color.White.copy(alpha = 0.15f),
                            disabledLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Target Exam & Study Target Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.6f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Exam & Academic Targets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Target Exam Selector trigger
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenExamSwitcher?.invoke() }
                            .testTag("profile_edit_exam_card"),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.School, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Selected Exam", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text(
                                        user?.examName ?: "UPSC CSE Preliminary",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text("Change >", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Daily Study Target Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Daily Study Goal",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                "${String.format(java.util.Locale.US, "%.1f", dailyStudyHours)} hrs/day",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                        Slider(
                            value = dailyStudyHours,
                            onValueChange = { dailyStudyHours = it },
                            valueRange = 1.0f..12.0f,
                            steps = 21,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldSuccess,
                                activeTrackColor = EmeraldSuccess
                            ),
                            modifier = Modifier.testTag("study_hours_slider")
                        )
                    }

                    // Target Score Text
                    OutlinedTextField(
                        value = targetScoreText,
                        onValueChange = { targetScoreText = it },
                        label = { Text("Target Score Goal") },
                        placeholder = { Text("e.g. 99%ile, Top 500 AIR, 180+ marks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = ElectricViolet,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Preferred Language Selector Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.6f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Preferred Language",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Used by AI Tutor, daily briefing & practice questions",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableLanguages) { lang ->
                            val isSelected = selectedLanguage.equals(lang, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLanguage = lang },
                                label = { Text(lang, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White.copy(alpha = 0.8f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("lang_chip_$lang")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
