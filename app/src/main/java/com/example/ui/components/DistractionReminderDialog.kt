package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun DistractionReminderDialog(
    appName: String = "App",
    minutesSpent: Int = 25,
    pendingSubject: String = "Physics",
    pendingTopic: String = "Core Practice",
    onStartStudy: (subject: String, topic: String, mins: Int) -> Unit,
    onRemindLater: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GoldenSpark.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .testTag("distraction_reminder_dialog"),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoldenSpark.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = GoldenSpark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Distraction Shield Reminder ⏳",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x20F59E0B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40F59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Boss, aap kaafi der se $appName par ho ($minutesSpent min). Agar study session pending hai to 20-minute $pendingSubject session complete kar sakte ho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onStartStudy(pendingSubject, pendingTopic, 20) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF070B19), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Study Session (20m)", color = Color(0xFF070B19), fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRemindLater,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x30FFFFFF))
                        ) {
                            Icon(Icons.Filled.Schedule, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remind Later", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("Dismiss", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
