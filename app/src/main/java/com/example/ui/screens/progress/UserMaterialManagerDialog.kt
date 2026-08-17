package com.example.ui.screens.progress

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserQuestionMaterial
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMaterialManagerDialog(
    materials: List<UserQuestionMaterial>,
    defaultSubject: String = "Physics",
    onDismiss: () -> Unit,
    onSaveMaterial: (title: String, exam: String, subject: String, topic: String, rawText: String) -> Unit,
    onDeleteMaterial: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var exam by remember { mutableStateOf("JEE Main & Advanced") }
    var subject by remember { mutableStateOf(defaultSubject) }
    var topic by remember { mutableStateOf("General") }
    var rawText by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(if (materials.isEmpty()) 0 else 1) } // 0: Add New, 1: View List

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp)
                .testTag("user_material_manager_dialog"),
            shape = RoundedCornerShape(24.dp),
            fillAlpha = 0.95f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricViolet.copy(alpha = 0.25f)
                        ) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Custom Question Bank",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Add personal practice notes & test papers",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x15FFFFFF))
                        .padding(4.dp)
                ) {
                    TabPill(
                        text = "➕ Add Notes / Questions",
                        isSelected = activeTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTab = 0 }
                    )
                    TabPill(
                        text = "📁 Saved Materials (${materials.size})",
                        isSelected = activeTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTab = 1 }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeTab == 0) {
                    // Add Material Form
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Document / Unit Title (e.g. Optics Class Test 2024)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricViolet,
                                    unfocusedBorderColor = Color(0x30FFFFFF),
                                    focusedLabelColor = ElectricViolet,
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = { subject = it },
                                    label = { Text("Subject") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricViolet,
                                        unfocusedBorderColor = Color(0x30FFFFFF),
                                        focusedLabelColor = ElectricViolet,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = topic,
                                    onValueChange = { topic = it },
                                    label = { Text("Topic") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricViolet,
                                        unfocusedBorderColor = Color(0x30FFFFFF),
                                        focusedLabelColor = ElectricViolet,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = exam,
                                onValueChange = { exam = it },
                                label = { Text("Target Exam (e.g. JEE, NEET, CBSE)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricViolet,
                                    unfocusedBorderColor = Color(0x30FFFFFF),
                                    focusedLabelColor = ElectricViolet,
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = rawText,
                                onValueChange = { rawText = it },
                                label = { Text("Paste Questions / Practice Content") },
                                minLines = 5,
                                maxLines = 10,
                                placeholder = {
                                    Text(
                                        "Paste questions, MCQs with options, or formula notes here.\nExample:\nQ1. What is the unit of electric flux?\nA) N/C\nB) N m^2 / C\nC) V m\nD) Both B and C",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricViolet,
                                    unfocusedBorderColor = Color(0x30FFFFFF),
                                    focusedLabelColor = ElectricViolet,
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlassButton(
                        text = "💾 Save to Question Bank",
                        onClick = {
                            if (title.isNotBlank() && rawText.isNotBlank()) {
                                onSaveMaterial(
                                    title.trim(),
                                    exam.trim().ifBlank { "General" },
                                    subject.trim().ifBlank { "Physics" },
                                    topic.trim().ifBlank { "General" },
                                    rawText.trim()
                                )
                                title = ""
                                rawText = ""
                                activeTab = 1
                            }
                        },
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_user_material_btn"
                    )
                } else {
                    // Materials List
                    if (materials.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No custom materials added yet",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                TextButton(onClick = { activeTab = 0 }) {
                                    Text("➕ Add your first question set", color = ElectricViolet)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(materials, key = { it.id }) { mat ->
                                MaterialItemCard(
                                    material = mat,
                                    onDelete = { onDeleteMaterial(mat.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp)),
        color = if (isSelected) ElectricViolet else Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MaterialItemCard(
    material: UserQuestionMaterial,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x15FFFFFF),
        border = BorderStroke(1.dp, Color(0x20FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${material.exam} • ${material.subject} • ${material.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${material.questionCount} Questions/Sections identified",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoralRose.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Material",
                    tint = CoralRose,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
