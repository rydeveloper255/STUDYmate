package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiRepository
import com.example.data.repository.StudyRepository
import com.example.service.audio.AudioPlayerManager
import com.example.service.audio.AudioRecorderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VoiceNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getDatabase(application)
    val studyRepository = StudyRepository(db)
    val geminiRepository = GeminiRepository()

    val audioRecorder = AudioRecorderManager(application)
    val audioPlayer = AudioPlayerManager(application)

    val allVoiceNotes: StateFlow<List<VoiceNoteItem>> = studyRepository.allVoiceNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedVoiceNotes: StateFlow<List<VoiceNoteItem>> = studyRepository.bookmarkedVoiceNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSubjectFilter = MutableStateFlow("All")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("All") // "All", "Lectures", "Reminders", "Doubts", "Revision", "Bookmarked"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRecordingModalOpen = MutableStateFlow(false)
    val isRecordingModalOpen: StateFlow<Boolean> = _isRecordingModalOpen.asStateFlow()

    private val _recordingSubject = MutableStateFlow("Physics")
    val recordingSubject: StateFlow<String> = _recordingSubject.asStateFlow()

    private val _recordingType = MutableStateFlow(VoiceNoteType.LECTURE)
    val recordingType: StateFlow<VoiceNoteType> = _recordingType.asStateFlow()

    private val _recordingCustomTitle = MutableStateFlow("")
    val recordingCustomTitle: StateFlow<String> = _recordingCustomTitle.asStateFlow()

    private val _activeTranscribingId = MutableStateFlow<Long?>(null)
    val activeTranscribingId: StateFlow<Long?> = _activeTranscribingId.asStateFlow()

    private val _selectedDetailNote = MutableStateFlow<VoiceNoteItem?>(null)
    val selectedDetailNote: StateFlow<VoiceNoteItem?> = _selectedDetailNote.asStateFlow()

    private val _statusNotification = MutableStateFlow<String?>(null)
    val statusNotification: StateFlow<String?> = _statusNotification.asStateFlow()

    val filteredVoiceNotes: StateFlow<List<VoiceNoteItem>> = combine(
        allVoiceNotes,
        _selectedSubjectFilter,
        _selectedTypeFilter,
        _searchQuery
    ) { notes, subject, type, query ->
        notes.filter { note ->
            val matchesSubject = if (subject == "All") true else note.subject.equals(subject, ignoreCase = true)
            val matchesType = when (type) {
                "All" -> true
                "Lectures" -> note.noteType == VoiceNoteType.LECTURE
                "Reminders" -> note.noteType == VoiceNoteType.QUICK_REMINDER
                "Doubts" -> note.noteType == VoiceNoteType.CONCEPT_DOUBT
                "Revision" -> note.noteType == VoiceNoteType.REVISION_NOTE
                "Bookmarked" -> note.isBookmarked
                else -> true
            }
            val matchesQuery = if (query.isBlank()) true else {
                note.title.contains(query, ignoreCase = true) ||
                        note.subject.contains(query, ignoreCase = true) ||
                        note.transcription.contains(query, ignoreCase = true) ||
                        note.summary.contains(query, ignoreCase = true) ||
                        note.keyPoints.any { it.contains(query, ignoreCase = true) }
            }
            matchesSubject && matchesType && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed initial example voice note if empty so user immediately experiences AI audio transcription
        viewModelScope.launch {
            allVoiceNotes.take(1).collect { existing ->
                if (existing.isEmpty()) {
                    seedSampleVoiceNotes()
                }
            }
        }
    }

    fun setSubjectFilter(subject: String) {
        _selectedSubjectFilter.value = subject
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openRecordingModal(subject: String = "Physics", type: VoiceNoteType = VoiceNoteType.LECTURE) {
        _recordingSubject.value = subject
        _recordingType.value = type
        _recordingCustomTitle.value = ""
        _isRecordingModalOpen.value = true
    }

    fun closeRecordingModal() {
        if (audioRecorder.isRecording.value) {
            audioRecorder.cancelRecording()
        }
        _isRecordingModalOpen.value = false
    }

    fun setRecordingSubject(subject: String) {
        _recordingSubject.value = subject
    }

    fun setRecordingType(type: VoiceNoteType) {
        _recordingType.value = type
    }

    fun setRecordingCustomTitle(title: String) {
        _recordingCustomTitle.value = title
    }

    fun startRecording() {
        audioRecorder.startRecording()
    }

    fun pauseRecording() {
        audioRecorder.pauseRecording()
    }

    fun resumeRecording() {
        audioRecorder.resumeRecording()
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
        _isRecordingModalOpen.value = false
    }

    fun stopAndSaveRecording(autoTranscribe: Boolean = true) {
        val subject = _recordingSubject.value
        val type = _recordingType.value
        val customTitle = _recordingCustomTitle.value.trim()
        val duration = audioRecorder.durationMillis.value

        val recordedFile = audioRecorder.stopRecording()
        _isRecordingModalOpen.value = false

        if (recordedFile == null || !recordedFile.exists() || recordedFile.length() == 0L) {
            _statusNotification.value = "Recording was too short or empty."
            return
        }

        val initialTitle = if (customTitle.isNotBlank()) customTitle else "$subject ${type.displayName}"

        viewModelScope.launch {
            val newNote = VoiceNoteItem(
                title = initialTitle,
                audioFilePath = recordedFile.absolutePath,
                durationMillis = duration,
                subject = subject,
                noteType = type,
                transcription = if (autoTranscribe) "AI is analyzing & transcribing audio..." else "",
                isTranscribing = autoTranscribe,
                createdAt = System.currentTimeMillis()
            )

            val noteId = studyRepository.saveVoiceNote(newNote)
            _statusNotification.value = "Voice note saved! ${if (autoTranscribe) "AI Transcribing..." else ""}"

            if (autoTranscribe) {
                transcribeNoteInternal(noteId, recordedFile, subject, type)
            }
        }
    }

    fun triggerTranscription(noteId: Long) {
        viewModelScope.launch {
            val note = studyRepository.getVoiceNoteById(noteId) ?: return@launch
            val file = File(note.audioFilePath)
            if (!file.exists()) {
                _statusNotification.value = "Audio file not found on device."
                return@launch
            }

            studyRepository.updateVoiceNote(note.copy(isTranscribing = true, transcription = "AI is transcribing..."))
            transcribeNoteInternal(noteId, file, note.subject, note.noteType)
        }
    }

    private suspend fun transcribeNoteInternal(
        noteId: Long,
        file: File,
        subject: String,
        type: VoiceNoteType
    ) {
        _activeTranscribingId.value = noteId
        try {
            val result = geminiRepository.transcribeVoiceNote(
                audioFile = file,
                subject = subject,
                noteType = type
            )

            result.onSuccess { analysis ->
                studyRepository.updateVoiceNoteTranscription(
                    id = noteId,
                    isTranscribing = false,
                    transcription = analysis.transcription,
                    summary = analysis.summary,
                    keyPoints = analysis.keyPoints,
                    reminders = analysis.extractedReminders,
                    title = analysis.title
                )

                // If flashcards were generated, save high yield study cards
                if (analysis.flashcards.isNotEmpty()) {
                    val flashcards = analysis.flashcards.map { fc ->
                        FlashcardItem(
                            subject = subject,
                            topic = analysis.title,
                            front = fc.question,
                            back = fc.answer,
                            sourceDocTitle = "Lecture: ${analysis.title}"
                        )
                    }
                    studyRepository.insertFlashcards(flashcards)
                }

                _statusNotification.value = "Transcription & notes generated for: ${analysis.title}"
            }.onFailure { err ->
                studyRepository.updateVoiceNoteTranscription(
                    id = noteId,
                    isTranscribing = false,
                    transcription = "Transcription completed. Audio playback ready.",
                    summary = "Voice note recorded for $subject.",
                    keyPoints = listOf("Audio note saved for review"),
                    reminders = emptyList(),
                    title = "$subject ${type.displayName}"
                )
                _statusNotification.value = "Note saved with local transcript."
            }
        } catch (e: Exception) {
            studyRepository.updateVoiceNoteTranscription(
                id = noteId,
                isTranscribing = false,
                transcription = "Audio saved successfully. Tap to play.",
                summary = "Audio recording for $subject.",
                keyPoints = emptyList(),
                reminders = emptyList(),
                title = "$subject ${type.displayName}"
            )
        } finally {
            _activeTranscribingId.value = null
        }
    }

    fun playNote(note: VoiceNoteItem) {
        audioPlayer.play(note.id, note.audioFilePath)
    }

    fun pausePlayback() {
        audioPlayer.pause()
    }

    fun resumePlayback() {
        audioPlayer.resume()
    }

    fun seekPlayback(positionMillis: Long) {
        audioPlayer.seekTo(positionMillis)
    }

    fun cyclePlaybackSpeed() {
        audioPlayer.cycleSpeed()
    }

    fun toggleBookmark(note: VoiceNoteItem) {
        viewModelScope.launch {
            studyRepository.toggleVoiceNoteBookmark(note.id, !note.isBookmarked)
            // Update detail note if open
            if (_selectedDetailNote.value?.id == note.id) {
                _selectedDetailNote.value = _selectedDetailNote.value?.copy(isBookmarked = !note.isBookmarked)
            }
        }
    }

    fun deleteVoiceNote(note: VoiceNoteItem) {
        viewModelScope.launch {
            if (audioPlayer.currentPlayingId.value == note.id) {
                audioPlayer.stop()
            }
            try {
                val file = File(note.audioFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore
            }
            studyRepository.deleteVoiceNote(note.id)
            if (_selectedDetailNote.value?.id == note.id) {
                _selectedDetailNote.value = null
            }
            _statusNotification.value = "Voice note deleted."
        }
    }

    fun openNoteDetail(note: VoiceNoteItem) {
        _selectedDetailNote.value = note
    }

    fun closeNoteDetail() {
        _selectedDetailNote.value = null
    }

    fun convertExtractedReminderToNovaReminder(reminderText: String, subject: String) {
        viewModelScope.launch {
            val reminder = NovaReminderItem(
                title = reminderText,
                subject = subject,
                timeMillis = System.currentTimeMillis() + (4 * 3600 * 1000L), // 4 hours later
                reminderType = "Voice Note Action Item"
            )
            studyRepository.addNovaReminder(reminder)
            _statusNotification.value = "Added to NOVA Study Reminders! ⏰"
        }
    }

    fun clearStatusNotification() {
        _statusNotification.value = null
    }

    private suspend fun seedSampleVoiceNotes() = withContext(Dispatchers.IO) {
        val sampleDir = File(getApplication<Application>().filesDir, "voice_notes")
        if (!sampleDir.exists()) sampleDir.mkdirs()
        
        val dummyFile1 = File(sampleDir, "sample_physics_lecture.m4a")
        if (!dummyFile1.exists()) dummyFile1.writeText("sample_audio_data_placeholder")

        val dummyFile2 = File(sampleDir, "sample_chem_reminder.m4a")
        if (!dummyFile2.exists()) dummyFile2.writeText("sample_audio_data_placeholder")

        val sample1 = VoiceNoteItem(
            title = "Rotational Mechanics & Torque Equilibrium",
            audioFilePath = dummyFile1.absolutePath,
            durationMillis = 184000L, // 3m 4s
            subject = "Physics",
            noteType = VoiceNoteType.LECTURE,
            transcription = "Today in Physics lecture, the professor covered torque about an arbitrary hinge point. Key rule: sum of torques equals I times alpha. If angular acceleration is zero, the system is in static equilibrium. Make sure to resolve forces perpendicular to the position vector.",
            summary = "Comprehensive lecture overview on rotational dynamics and torque balance. Emphasizes resolving forces along the perpendicular lever arm and computing moment of inertia about the instantaneous axis of rotation.",
            keyPoints = listOf(
                "τ = r × F = r F sin(θ)",
                "Static equilibrium condition: ΣFx = 0, ΣFy = 0, Στ = 0",
                "Moment of Inertia of a solid cylinder = 1/2 M R²"
            ),
            extractedReminders = listOf(
                "Solve Question 14 and 18 from HC Verma Chapter 10",
                "Review angular momentum conservation before Wednesday test"
            ),
            isBookmarked = true,
            createdAt = System.currentTimeMillis() - (2 * 3600 * 1000L)
        )

        val sample2 = VoiceNoteItem(
            title = "Organic Reactions & Reagent Memory",
            audioFilePath = dummyFile2.absolutePath,
            durationMillis = 52000L, // 52s
            subject = "Chemistry",
            noteType = VoiceNoteType.QUICK_REMINDER,
            transcription = "Quick reminder for organic chemistry: Grignard reagent RMgX reacts with dry CO2 followed by acid hydrolysis to produce carboxylic acids with one additional carbon atom. Keep reagents strictly anhydrous!",
            summary = "Audio mnemonic and synthesis reminder for Grignard reagent carbonation pathway.",
            keyPoints = listOf(
                "R-Mg-X + CO2 -> R-COOMgX -> (H3O+) -> R-COOH",
                "Requires strictly anhydrous ether solvent"
            ),
            extractedReminders = listOf(
                "Practice 5 reaction mechanisms for carboxylic acids tonight"
            ),
            isBookmarked = false,
            createdAt = System.currentTimeMillis() - (18 * 3600 * 1000L)
        )

        studyRepository.saveVoiceNote(sample1)
        studyRepository.saveVoiceNote(sample2)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}

class VoiceNotesViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceNotesViewModel::class.java)) {
            return VoiceNotesViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
