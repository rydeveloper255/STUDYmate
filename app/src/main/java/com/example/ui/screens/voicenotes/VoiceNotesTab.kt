package com.example.ui.screens.voicenotes

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VoiceNoteItem
import com.example.data.model.VoiceNoteType
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.VoiceNotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VoiceNotesTab(
    viewModel: VoiceNotesViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceNotes by viewModel.filteredVoiceNotes.collectAsStateWithLifecycle()
    val allNotes by viewModel.allVoiceNotes.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRecordingModalOpen by viewModel.isRecordingModalOpen.collectAsStateWithLifecycle()
    val selectedDetailNote by viewModel.selectedDetailNote.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusNotification.collectAsStateWithLifecycle()

    val isRecording by viewModel.audioRecorder.isRecording.collectAsStateWithLifecycle()
    val isRecordingPaused by viewModel.audioRecorder.isPaused.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.audioRecorder.durationMillis.collectAsStateWithLifecycle()
    val currentAmplitude by viewModel.audioRecorder.currentAmplitude.collectAsStateWithLifecycle()
    val amplitudeHistory by viewModel.audioRecorder.amplitudeHistory.collectAsStateWithLifecycle()

    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val currentPlayingId by viewModel.audioPlayer.currentPlayingId.collectAsStateWithLifecycle()
    val playbackPos by viewModel.audioPlayer.currentPositionMillis.collectAsStateWithLifecycle()
    val playbackTotal by viewModel.audioPlayer.totalDurationMillis.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.audioPlayer.playbackSpeed.collectAsStateWithLifecycle()
    val activeTranscribingId by viewModel.activeTranscribingId.collectAsStateWithLifecycle()

    // Permission launcher for Recording Audio
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openRecordingModal()
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusNotification()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                VoiceNotesHeader(
                    totalNotes = allNotes.size,
                    totalDurationMillis = allNotes.sumOf { it.durationMillis },
                    onBack = onBack,
                    onStartRecord = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.openRecordingModal()
                            viewModel.startRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            // Search Bar & Filter Row
            item {
                VoiceNotesSearchAndFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { viewModel.setSubjectFilter(it) },
                    selectedType = selectedType,
                    onTypeSelected = { viewModel.setTypeFilter(it) }
                )
            }

            // Quick Create Bar
            item {
                QuickRecordPromptCard(
                    onRecordLecture = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.openRecordingModal(type = VoiceNoteType.LECTURE)
                            viewModel.startRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onRecordReminder = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.openRecordingModal(type = VoiceNoteType.QUICK_REMINDER)
                            viewModel.startRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            // Voice Notes List
            if (voiceNotes.isEmpty()) {
                item {
                    EmptyVoiceNotesPlaceholder(
                        searchQuery = searchQuery,
                        onRecordClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.openRecordingModal()
                                viewModel.startRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }
            } else {
                items(voiceNotes, key = { it.id }) { note ->
                    VoiceNoteCard(
                        note = note,
                        isPlaying = isPlaying && currentPlayingId == note.id,
                        playbackPos = if (currentPlayingId == note.id) playbackPos else 0L,
                        playbackTotal = if (currentPlayingId == note.id) playbackTotal else note.durationMillis,
                        playbackSpeed = playbackSpeed,
                        isTranscribing = activeTranscribingId == note.id || note.isTranscribing,
                        onPlayPause = { viewModel.playNote(note) },
                        onSeek = { viewModel.seekPlayback(it) },
                        onCycleSpeed = { viewModel.cyclePlaybackSpeed() },
                        onToggleBookmark = { viewModel.toggleBookmark(note) },
                        onTranscribe = { viewModel.triggerTranscription(note.id) },
                        onDelete = { viewModel.deleteVoiceNote(note) },
                        onClickDetail = { viewModel.openNoteDetail(note) },
                        onAddReminder = { reminder ->
                            viewModel.convertExtractedReminderToNovaReminder(reminder, note.subject)
                        }
                    )
                }
            }
        }

        // Live Audio Recording Bottom Sheet / Modal
        if (isRecordingModalOpen) {
            LiveAudioRecordingModal(
                isRecording = isRecording,
                isPaused = isRecordingPaused,
                durationMillis = recordingDuration,
                currentAmplitude = currentAmplitude,
                amplitudeHistory = amplitudeHistory,
                subject = viewModel.recordingSubject.collectAsStateWithLifecycle().value,
                onSubjectChange = { viewModel.setRecordingSubject(it) },
                noteType = viewModel.recordingType.collectAsStateWithLifecycle().value,
                onTypeChange = { viewModel.setRecordingType(it) },
                customTitle = viewModel.recordingCustomTitle.collectAsStateWithLifecycle().value,
                onTitleChange = { viewModel.setRecordingCustomTitle(it) },
                onPause = { viewModel.pauseRecording() },
                onResume = { viewModel.resumeRecording() },
                onCancel = { viewModel.cancelRecording() },
                onFinish = { viewModel.stopAndSaveRecording(autoTranscribe = true) }
            )
        }

        // Detail Bottom Sheet
        selectedDetailNote?.let { note ->
            VoiceNoteDetailDialog(
                note = note,
                isPlaying = isPlaying && currentPlayingId == note.id,
                playbackPos = if (currentPlayingId == note.id) playbackPos else 0L,
                playbackTotal = if (currentPlayingId == note.id) playbackTotal else note.durationMillis,
                playbackSpeed = playbackSpeed,
                onPlayPause = { viewModel.playNote(note) },
                onSeek = { viewModel.seekPlayback(it) },
                onCycleSpeed = { viewModel.cyclePlaybackSpeed() },
                onToggleBookmark = { viewModel.toggleBookmark(note) },
                onAddReminder = { reminder ->
                    viewModel.convertExtractedReminderToNovaReminder(reminder, note.subject)
                },
                onDismiss = { viewModel.closeNoteDetail() }
            )
        }
    }
}

// =========================================================================
// HEADER & STATS
// =========================================================================

@Composable
private fun VoiceNotesHeader(
    totalNotes: Int,
    totalDurationMillis: Long,
    onBack: (() -> Unit)?,
    onStartRecord: () -> Unit
) {
    val totalMins = (totalDurationMillis / 1000 / 60).toInt()

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonCyan, DeepIndigo))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Voice Notes & Lectures",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Audio recording with AI text transcription",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                Button(
                    onClick = onStartRecord,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = DarkCanvas,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Record",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkCanvas
                        )
                    )
                }
            }

            // Mini Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(icon = "🎙️", label = "Notes", value = "$totalNotes")
                Divider(modifier = Modifier.height(20.dp).width(1.dp), color = GlassBorderDark)
                StatPill(icon = "⏱️", label = "Duration", value = "${totalMins}m")
                Divider(modifier = Modifier.height(20.dp).width(1.dp), color = GlassBorderDark)
                StatPill(icon = "✨", label = "AI Transcribed", value = "Auto")
            }
        }
    }
}

@Composable
private fun StatPill(icon: String, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

// =========================================================================
// SEARCH & FILTERS
// =========================================================================

@Composable
private fun VoiceNotesSearchAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val subjects = listOf("All", "Physics", "Chemistry", "Mathematics", "Biology", "General")
    val types = listOf("All", "Lectures", "Reminders", "Doubts", "Revision", "Bookmarked")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search voice notes, transcripts, or summaries...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = NeonCyan, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated.copy(alpha = 0.6f),
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassBorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Type Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(types) { type ->
                val isSelected = selectedType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(type) },
                    label = {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DarkCanvas else TextPrimary
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        containerColor = DarkSurfaceElevated.copy(alpha = 0.5f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = GlassBorderDark,
                        selectedBorderColor = NeonCyan
                    )
                )
            }
        }

        // Subject Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(subjects) { subject ->
                val isSelected = selectedSubject == subject
                FilterChip(
                    selected = isSelected,
                    onClick = { onSubjectSelected(subject) },
                    label = {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DarkCanvas else TextSecondary
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreen,
                        containerColor = DarkSurfaceElevated.copy(alpha = 0.3f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = GlassBorderDark,
                        selectedBorderColor = EmeraldGreen
                    )
                )
            }
        }
    }
}

// =========================================================================
// QUICK ACTION PROMPT CARD
// =========================================================================

@Composable
private fun QuickRecordPromptCard(
    onRecordLecture: () -> Unit,
    onRecordReminder: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = NeonCyan.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎙️ Instant Audio Capture",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Record classroom lecture or a fast study reminder",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRecordReminder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("⏰ Reminder", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onRecordLecture,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🎓 Lecture", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// VOICE NOTE CARD
// =========================================================================

@Composable
private fun VoiceNoteCard(
    note: VoiceNoteItem,
    isPlaying: Boolean,
    playbackPos: Long,
    playbackTotal: Long,
    playbackSpeed: Float,
    isTranscribing: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleBookmark: () -> Unit,
    onTranscribe: () -> Unit,
    onDelete: () -> Unit,
    onClickDetail: () -> Unit,
    onAddReminder: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(note.createdAt) { dateFormat.format(Date(note.createdAt)) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .springClickable { onClickDetail() },
        borderColor = if (isPlaying) NeonCyan else GlassBorderDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Top Row: Type & Subject Badge, Date, Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DeepIndigo.copy(alpha = 0.25f))
                            .border(1.dp, DeepIndigo.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${note.noteType.icon} ${note.noteType.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Subject Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = note.subject,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Bookmark",
                            tint = if (note.isBookmarked) AmberGold else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Audio Player Bar
            AudioPlayerBar(
                isPlaying = isPlaying,
                currentPos = playbackPos,
                totalDuration = if (playbackTotal > 0) playbackTotal else note.durationMillis,
                speed = playbackSpeed,
                onPlayPause = onPlayPause,
                onSeek = onSeek,
                onCycleSpeed = onCycleSpeed
            )

            // Transcribing Shimmer / Summary Preview
            if (isTranscribing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                    Text(
                        text = "✨ NOVA AI is transcribing audio & generating notes...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else if (note.summary.isNotBlank()) {
                // Summary Preview Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "✨", fontSize = 12.sp)
                        Text(
                            text = "AI Summary",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                    }

                    Text(
                        text = note.summary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Tags & Highlights
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.extractedReminders.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmberGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⏰ ${note.extractedReminders.size} Action Items",
                                    color = AmberGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (note.keyPoints.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "💡 ${note.keyPoints.size} Key Points",
                                    color = EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClickDetail,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "View Full Notes & Transcript →",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (note.summary.isBlank() && !isTranscribing) {
                        IconButton(
                            onClick = onTranscribe,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Transcribe with AI",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// AUDIO PLAYER BAR COMPONENT
// =========================================================================

@Composable
private fun AudioPlayerBar(
    isPlaying: Boolean,
    currentPos: Long,
    totalDuration: Long,
    speed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit
) {
    val curSecs = (currentPos / 1000).toInt()
    val totSecs = (totalDuration / 1000).toInt().coerceAtLeast(1)
    val curText = "${curSecs / 60}:${String.format(Locale.getDefault(), "%02d", curSecs % 60)}"
    val totText = "${totSecs / 60}:${String.format(Locale.getDefault(), "%02d", totSecs % 60)}"
    val progress = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Play / Pause Circle Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isPlaying) EmeraldGreen else NeonCyan)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = DarkCanvas,
                modifier = Modifier.size(22.dp)
            )
        }

        // Progress Slider & Times
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    val newPos = (newProgress * totalDuration).toLong()
                    onSeek(newPos)
                },
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = GlassBorderDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = curText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = totText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                )
            }
        }

        // Speed Chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { onCycleSpeed() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${speed}x",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// =========================================================================
// LIVE AUDIO RECORDING MODAL
// =========================================================================

@Composable
private fun LiveAudioRecordingModal(
    isRecording: Boolean,
    isPaused: Boolean,
    durationMillis: Long,
    currentAmplitude: Float,
    amplitudeHistory: List<Float>,
    subject: String,
    onSubjectChange: (String) -> Unit,
    noteType: VoiceNoteType,
    onTypeChange: (VoiceNoteType) -> Unit,
    customTitle: String,
    onTitleChange: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    val totalSecs = (durationMillis / 1000).toInt()
    val formattedTime = "${String.format(Locale.getDefault(), "%02d", totalSecs / 60)}:${String.format(Locale.getDefault(), "%02d", totalSecs % 60)}"

    Dialog(onDismissRequest = { /* Prevent accidental cancel while recording */ }) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            borderColor = NeonCyan
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (!isPaused && isRecording) Color.Red else AmberGold)
                        )
                        Text(
                            text = if (isPaused) "Recording Paused" else "Recording in Progress...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isPaused) AmberGold else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = TextSecondary
                        )
                    }
                }

                // Type & Subject Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recording Type & Subject",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )

                    // Note Type Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(VoiceNoteType.values()) { type ->
                            val isSelected = noteType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DeepIndigo else DarkSurfaceElevated)
                                    .border(1.dp, if (isSelected) ElectricViolet else GlassBorderDark, RoundedCornerShape(8.dp))
                                    .clickable { onTypeChange(type) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${type.icon} ${type.displayName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }

                    // Subject Chips
                    val subjects = listOf("Physics", "Chemistry", "Mathematics", "Biology", "English", "General")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { sub ->
                            val isSelected = subject == sub
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else DarkSurfaceElevated)
                                    .border(1.dp, if (isSelected) NeonCyan else GlassBorderDark, RoundedCornerShape(8.dp))
                                    .clickable { onSubjectChange(sub) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) DarkCanvas else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                // Optional Custom Title
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = onTitleChange,
                    placeholder = { Text("Note Title (Optional, e.g. Thermodynamics Part 1)", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated.copy(alpha = 0.5f),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Large Glowing Mic & Live Audio Waveform
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated.copy(alpha = 0.6f))
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Waveform Canvas
                        LiveAudioWaveformCanvas(
                            amplitudeHistory = amplitudeHistory,
                            isRecording = isRecording && !isPaused,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 16.dp)
                        )

                        // Duration Text
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) AmberGold else NeonCyan,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                // Controls: Cancel, Pause/Resume, Finish & Transcribe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trash / Cancel Button
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(1.dp, GlassBorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Trash Recording",
                            tint = TextSecondary
                        )
                    }

                    // Pause / Resume Button
                    IconButton(
                        onClick = {
                            if (isPaused) onResume() else onPause()
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) AmberGold else DarkSurfaceElevated)
                            .border(1.dp, if (isPaused) AmberGold else GlassBorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = if (isPaused) DarkCanvas else TextPrimary
                        )
                    }

                    // Finish & Transcribe
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.shadow(12.dp, RoundedCornerShape(14.dp), spotColor = NeonCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = DarkCanvas,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Transcribe ✨",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkCanvas
                            )
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// LIVE AUDIO WAVEFORM CANVAS
// =========================================================================

@Composable
private fun LiveAudioWaveformCanvas(
    amplitudeHistory: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = 36
        val barWidth = width / (barCount * 1.6f)
        val spacing = barWidth * 0.6f

        val points = if (amplitudeHistory.isNotEmpty()) {
            val takeCount = amplitudeHistory.takeLast(barCount)
            if (takeCount.size < barCount) {
                List(barCount - takeCount.size) { 0.1f } + takeCount
            } else {
                takeCount
            }
        } else {
            List(barCount) { 0.1f }
        }

        for (i in 0 until barCount) {
            val amp = points.getOrElse(i) { 0.1f }
            val barHeight = (height * amp).coerceIn(4f, height)
            val startX = i * (barWidth + spacing)
            val topY = (height - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(NeonCyan, DeepIndigo, EmeraldGreen)
                ),
                topLeft = androidx.compose.ui.geometry.Offset(startX, topY),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}

// =========================================================================
// VOICE NOTE DETAIL DIALOG / BOTTOM SHEET
// =========================================================================

@Composable
private fun VoiceNoteDetailDialog(
    note: VoiceNoteItem,
    isPlaying: Boolean,
    playbackPos: Long,
    playbackTotal: Long,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleBookmark: () -> Unit,
    onAddReminder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedDetailTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp),
            borderColor = NeonCyan
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DeepIndigo.copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${note.noteType.icon} ${note.subject}",
                                color = ElectricViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (note.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Bookmark",
                                tint = if (note.isBookmarked) AmberGold else TextSecondary
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Title
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                // Audio Player Card
                AudioPlayerBar(
                    isPlaying = isPlaying,
                    currentPos = playbackPos,
                    totalDuration = if (playbackTotal > 0) playbackTotal else note.durationMillis,
                    speed = playbackSpeed,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onCycleSpeed = onCycleSpeed
                )

                // Section Tabs
                val tabTitles = listOf("✨ Summary", "📝 Transcript", "⏰ Reminders (${note.extractedReminders.size})")
                TabRow(
                    selectedTabIndex = selectedDetailTab,
                    containerColor = DarkSurfaceElevated,
                    contentColor = NeonCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedDetailTab == index,
                            onClick = { selectedDetailTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedDetailTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedDetailTab == index) NeonCyan else TextSecondary
                                )
                            }
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    when (selectedDetailTab) {
                        0 -> {
                            // Summary & Key Points
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        text = "AI Overview",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.summary.ifBlank { "No summary available. Tap re-transcribe to process with AI." },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }

                                if (note.keyPoints.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "💡 Key Formulas & Concepts",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldGreen
                                            )
                                        )
                                    }

                                    items(note.keyPoints) { point ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.04f))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = "•", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = point,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextPrimary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Full Verbatim Transcript
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Full Audio Transcription",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                        )

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Transcription", note.transcription)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Transcript copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.transcription}")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Share Voice Note"))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.transcription.ifBlank { "No transcript generated yet." },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            lineHeight = 22.sp
                                        )
                                    )
                                }
                            }
                        }
                        2 -> {
                            // Extracted Action Items & Reminders
                            if (note.extractedReminders.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No action items or reminders detected in this recording.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(note.extractedReminders) { reminder ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, GlassBorderDark, RoundedCornerShape(10.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = "⏰", fontSize = 16.sp)
                                                Text(
                                                    text = reminder,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = TextPrimary,
                                                        fontSize = 13.sp
                                                    )
                                                )
                                            }

                                            Button(
                                                onClick = { onAddReminder(reminder) },
                                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "+ Add",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = DarkCanvas,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// EMPTY STATE PLACEHOLDER
// =========================================================================

@Composable
private fun EmptyVoiceNotesPlaceholder(
    searchQuery: String,
    onRecordClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DeepIndigo.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MicNone,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = if (searchQuery.isNotBlank()) "No notes match '$searchQuery'" else "No voice notes recorded yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Text(
                text = "Record classroom lectures or speak quick reminders. NOVA AI will automatically transcribe and summarize them into actionable study notes.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = onRecordClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = DarkCanvas,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Record First Voice Note",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkCanvas
                    )
                )
            }
        }
    }
}
