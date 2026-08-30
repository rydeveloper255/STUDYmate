package com.example.ui.screens.feedback

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.FeedbackCategory
import com.example.data.model.UserFeedbackEntity
import com.example.service.feedback.FeedbackManager
import com.example.service.feedback.ProcessedAttachment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.feedback.FeedbackAttachmentUtility

// Theme Colors
private val DarkBg = Color(0xFF090D16)
private val CardBg = Color(0xFF131B2E)
private val CardBorder = Color(0xFF1E293B)
private val NeonCyan = Color(0xFF06B6D4)
private val CoralPink = Color(0xFFF43F5E)
private val AmberYellow = Color(0xFFF59E0B)
private val EmeraldGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    initialFeature: String = "General",
    initialErrorId: String? = null,
    viewModel: FeedbackViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(initialFeature, initialErrorId) {
        viewModel.setInitialContext(initialFeature, initialErrorId)
        viewModel.loadFeedbackHistory(context)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Activity Result Launchers using ViewModel
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addUris(context, uris)
        }
    }

    val recordingPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.addUris(context, listOf(uri))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Feedback & Bug Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Help us improve StudyMate for everyone",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row Switcher
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkBg,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Report", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = NeonCyan,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My Feedback", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = NeonCyan,
                    unselectedContentColor = TextSecondary
                )
            }

            if (selectedTabIndex == 0) {
                // Submit Form View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Linked Error Banner (if triggered from an app error)
                    if (!uiState.relatedErrorId.isNullOrBlank()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0764)),
                                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD8B4FE))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Linked Application Error",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Error ID: ${uiState.relatedErrorId} (Context pre-filled)",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE9D5FF)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Feedback Category Selection
                    item {
                        Column {
                            Text(
                                text = "Feedback Type",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(FeedbackCategory.values()) { category ->
                                    val isSelected = uiState.selectedCategory == category
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setCategory(category) },
                                        label = {
                                            Text(
                                                text = "${category.iconEmoji} ${category.label}",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan,
                                            selectedLabelColor = Color(0xFF090D16),
                                            containerColor = CardBg,
                                            labelColor = TextPrimary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = CardBorder,
                                            selectedBorderColor = NeonCyan
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. High Priority Checkbox
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, if (uiState.isHighPriority) CoralPink else CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = uiState.isHighPriority,
                                    onCheckedChange = { viewModel.setHighPriority(it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CoralPink,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Preventing app usage?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Check if this issue is blocking you from using StudyMate",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 4. Affected Feature / Screen
                    item {
                        Column {
                            Text(
                                text = "Affected Feature / Screen",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.affectedFeature,
                                onValueChange = { viewModel.setAffectedFeature(it) },
                                placeholder = { Text("e.g. Mock Test, Notes, Nova AI") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBg,
                                    unfocusedContainerColor = CardBg
                                )
                            )
                        }
                    }

                    // 5. Title (Optional)
                    item {
                        Column {
                            Text(
                                text = "Title (Optional)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.titleText,
                                onValueChange = { viewModel.setTitle(it) },
                                placeholder = { Text("Short title for the problem...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBg,
                                    unfocusedContainerColor = CardBg
                                )
                            )
                        }
                    }

                    // 6. Problem Description (Mandatory)
                    item {
                        Column {
                            Text(
                                text = "Problem Description *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.descriptionText,
                                onValueChange = { viewModel.setDescription(it) },
                                placeholder = { Text("Problem ko detail me likhein...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = CardBg,
                                    unfocusedContainerColor = CardBg
                                )
                            )
                        }
                    }

                    // 7. Attachments (Screenshots & Video)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Attachments (Optional)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Screenshot Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardBg,
                                    border = BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { screenshotPickerLauncher.launch("image/*") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Screenshot(s)", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // Screen Recording Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardBg,
                                    border = BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { recordingPickerLauncher.launch("video/*") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = AmberYellow, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Screen Video", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (uiState.isProcessingAttachments) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Processing & validating files...", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            if (!uiState.errorMessage.isNullOrBlank()) {
                                Text(
                                    text = uiState.errorMessage!!,
                                    fontSize = 11.sp,
                                    color = CoralPink,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Selected Attachments Preview
                            if (uiState.processedAttachments.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    items(uiState.processedAttachments) { attachment ->
                                        if (attachment.isVideo) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF1E1B4B),
                                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                                modifier = Modifier.size(width = 140.dp, height = 80.dp)
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                                    Column(modifier = Modifier.align(Alignment.Center)) {
                                                        Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = AmberYellow, modifier = Modifier.size(24.dp))
                                                        Text("Screen Video", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.removeAttachment(attachment) },
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .align(Alignment.TopEnd)
                                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                            ) {
                                                AsyncImage(
                                                    model = attachment.file,
                                                    contentDescription = "Screenshot Preview",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(
                                                    onClick = { viewModel.removeAttachment(attachment) },
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .align(Alignment.TopEnd)
                                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 8. Auto System Info Disclosure
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CardBg.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Auto-captured Diagnostic Context:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📱 Device: ${uiState.deviceModel} | ⚙️ OS: ${uiState.osVersion}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // 9. Submit Button
                    item {
                        Button(
                            onClick = {
                                viewModel.submitFeedback(context)
                            },
                            enabled = !uiState.isSubmitting && uiState.descriptionText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isHighPriority) CoralPink else NeonCyan,
                                contentColor = Color(0xFF090D16)
                            )
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF090D16), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submitting...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Feedback", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            } else {
                // My Feedback History Tab View
                MyFeedbackHistoryView(context = context, viewModel = viewModel)
            }
        }
    }

    // Success Dialog Overlay
    if (uiState.submissionSuccessEntity != null) {
        val entity = uiState.submissionSuccessEntity!!
        Dialog(onDismissRequest = { viewModel.clearSuccessState() }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardBg,
                border = BorderStroke(1.dp, EmeraldGreen),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))

                    Text("✅ Feedback Sent Successfully", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Surface(
                        color = DarkBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = "Feedback ID: ${entity.feedbackId}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "Thank you! Aapka feedback StudyMate team ko bhej diya gaya hai.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                viewModel.clearSuccessState()
                                selectedTabIndex = 1 // Switch to History Tab
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF090D16))
                        ) {
                            Text("View My Feedback", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyFeedbackHistoryView(
    context: android.content.Context,
    viewModel: FeedbackViewModel = viewModel()
) {
    val feedbackList by viewModel.feedbackHistory.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadFeedbackHistory(context)
    }

    if (feedbackList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No feedback submitted yet", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Your submitted feedback history will appear here", fontSize = 12.sp, color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(feedbackList) { item ->
                FeedbackHistoryItemCard(item = item)
            }
        }
    }
}

@Composable
fun FeedbackHistoryItemCard(item: UserFeedbackEntity) {
    val categoryObj = FeedbackCategory.fromString(item.category)
    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(item.createdAtMillis))

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(categoryObj.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = categoryObj.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Status Chip
                val (statusBg, statusFg) = when (item.status) {
                    "RESOLVED" -> EmeraldGreen to Color.Black
                    "REVIEWING" -> Color(0xFFA855F7) to Color.White
                    else -> AmberYellow to Color.Black
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = item.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.description,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ID: ${item.feedbackId}", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Text(dateStr, fontSize = 10.sp, color = TextSecondary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.syncState == "SENT") {
                        Icon(Icons.Default.CloudDone, contentDescription = "Sent", tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sent", fontSize = 10.sp, color = EmeraldGreen)
                    } else {
                        Icon(Icons.Default.CloudOff, contentDescription = "Pending Sync", tint = AmberYellow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pending Sync", fontSize = 10.sp, color = AmberYellow)
                    }
                }
            }
        }
    }
}
