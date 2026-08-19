package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.NovaMemoryCategory
import com.example.data.model.NovaMemoryItem
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaMemoryCenterTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf<NovaMemoryCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<NovaMemoryItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filteredMemories = remember(memories, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) memories else memories.filter { it.category == selectedCategoryFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
            borderColor = ElectricIndigo.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("NOVA MEMORY CENTER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricIndigo, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Personal Learning Context", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Private on-device contextual knowledge", fontSize = 12.sp, color = TextSecondary)
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkCanvas)
                }
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { selectedCategoryFilter = null },
                label = { Text("All (${memories.size})") }
            )
            NovaMemoryCategory.values().forEach { cat ->
                val count = memories.count { it.category == cat }
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat },
                    label = { Text("${cat.displayName} ($count)") }
                )
            }
        }

        // Memories List
        if (filteredMemories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No memory items found", color = TextSecondary, fontSize = 14.sp)
                    Text("Tap '+ Add' to save study habits, target ranks or notes.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMemories, key = { it.id }) { item ->
                    MemoryItemCard(
                        item = item,
                        onToggle = { isEnabled -> viewModel.toggleMemory(item.id, isEnabled) },
                        onEdit = { editingMemory = item },
                        onDelete = { viewModel.deleteMemory(item.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralPink),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All NOVA Memories", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Add Memory Dialog
    if (showAddDialog) {
        AddEditMemoryDialog(
            memoryToEdit = null,
            onDismiss = { showAddDialog = false },
            onSave = { category, key, value ->
                viewModel.addManualMemory(category, key, value)
                showAddDialog = false
            }
        )
    }

    // Edit Memory Dialog
    if (editingMemory != null) {
        AddEditMemoryDialog(
            memoryToEdit = editingMemory,
            onDismiss = { editingMemory = null },
            onSave = { category, key, value ->
                editingMemory?.let {
                    viewModel.editManualMemory(it.id, category, key, value)
                }
                editingMemory = null
            }
        )
    }

    // Clear Confirm Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Memories?") },
            text = { Text("This will permanently delete all saved personal memory and preferences from NOVA.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMemories()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemoryItemCard(
    item: NovaMemoryItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.category.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${item.source}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.key,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.value,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralPink, modifier = Modifier.size(18.dp))
                }
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
private fun AddEditMemoryDialog(
    memoryToEdit: NovaMemoryItem?,
    onDismiss: () -> Unit,
    onSave: (category: NovaMemoryCategory, key: String, value: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(memoryToEdit?.category ?: NovaMemoryCategory.ACADEMIC) }
    var key by remember { mutableStateOf(memoryToEdit?.key ?: "") }
    var value by remember { mutableStateOf(memoryToEdit?.value ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (memoryToEdit == null) "Add NOVA Memory" else "Edit Memory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text("Category", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NovaMemoryCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Title / Concept Key") },
                    placeholder = { Text("e.g. Weak in Rotational Motion formulas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Details / Preference Note") },
                    placeholder = { Text("e.g. Needs step-by-step torque calculations") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (key.isNotBlank() && value.isNotBlank()) {
                                onSave(selectedCategory, key, value)
                            }
                        },
                        enabled = key.isNotBlank() && value.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Save Memory", fontWeight = FontWeight.Bold, color = DarkCanvas)
                    }
                }
            }
        }
    }
}
