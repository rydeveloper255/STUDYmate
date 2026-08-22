package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppNavTab
import com.example.data.model.NovaActionType
import com.example.data.model.NovaContextualAction
import com.example.viewmodel.NovaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaFloatingAssistant(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: ((AppNavTab) -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val appContext by viewModel.appContext.collectAsState()
    val answerMsg by viewModel.homeWidgetAnswer.collectAsState()
    val widgetState by viewModel.homeWidgetDisplayState.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var queryText by remember { mutableStateOf("") }

    // If on active mock test screen, hide floating assistant to respect exam integrity
    if (appContext.isTestActive) {
        return
    }

    Box(modifier = modifier) {
        // Floating Pill Button
        FloatingActionButton(
            onClick = { showSheet = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ask NOVA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Assistant Bottom Sheet
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                ) {
                    // Header & Context badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NOVA Quick Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Screen Context Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = appContext.topic ?: appContext.subject ?: appContext.screenName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            placeholder = { Text("Ask doubt about ${appContext.topic ?: "this"}...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (queryText.isNotBlank()) {
                                        keyboardController?.hide()
                                        viewModel.submitHomeWidgetQuery(queryText, context, onNavigateToTab)
                                    }
                                }
                            )
                        )

                        IconButton(
                            onClick = {
                                if (queryText.isNotBlank()) {
                                    keyboardController?.hide()
                                    viewModel.submitHomeWidgetQuery(queryText, context, onNavigateToTab)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Send")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contextual Suggestions Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                val q = "Explain key formula for ${appContext.topic ?: "this"}"
                                queryText = q
                                viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                            },
                            label = { Text("💡 Key Formula", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = {
                                val q = "Make 5 questions for ${appContext.topic ?: "this"}"
                                queryText = q
                                viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                            },
                            label = { Text("✍️ Practice 5", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = {
                                val q = "Summarize ${appContext.topic ?: "this"}"
                                queryText = q
                                viewModel.submitHomeWidgetQuery(q, context, onNavigateToTab)
                            },
                            label = { Text("📝 Summary", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    // Answer Box (if active)
                    if (widgetState == com.example.data.model.HomeWidgetDisplayState.EXPANDED && answerMsg != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = answerMsg!!.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.executeContextualAction(
                                                NovaContextualAction(
                                                    label = "Open NOVA",
                                                    actionType = NovaActionType.OPEN_FULL_NOVA
                                                ),
                                                context,
                                                onNavigateToTab
                                            )
                                            showSheet = false
                                        }
                                    ) {
                                        Text("Open Full NOVA →")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
