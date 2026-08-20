package com.example.data.repository

import android.util.Log
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.data.remote.supabase.SupabaseSyncService
import com.example.service.intelligence.ExamIntelligenceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository responsible for managing the active ExamContext across the entire application.
 * Ensures Planner, Mock Tests, Revision, Nova, and Analytics consume a single source of exam truth.
 */
class ExamContextRepository(
    private val database: StudyMateDatabase,
    private val intelligenceService: ExamIntelligenceService,
    private val syncService: SupabaseSyncService? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "ExamContextRepository"

    private val userDao = database.userDao()
    private val examObjectiveDao = database.examObjectiveDao()

    private val _activeExamContext = MutableStateFlow(ExamContext.defaultContext())
    val activeExamContext: StateFlow<ExamContext> = _activeExamContext.asStateFlow()

    private val _discoveryState = MutableStateFlow<ExamDiscoveryState>(ExamDiscoveryState.Idle)
    val discoveryState: StateFlow<ExamDiscoveryState> = _discoveryState.asStateFlow()

    init {
        // Observe user profile changes to sync active ExamContext
        scope.launch {
            userDao.getUserProfile().filterNotNull().collect { profile ->
                val currentExamName = profile.examName
                val currentExamId = sanitizeExamId(currentExamName)
                if (_activeExamContext.value.examId != currentExamId) {
                    loadExamContext(
                        examId = currentExamId,
                        examName = currentExamName,
                        category = "Selected Exam"
                    )
                }
            }
        }
    }

    /**
     * Loads or discovers ExamContext for the specified exam.
     */
    suspend fun loadExamContext(
        examId: String,
        examName: String,
        category: String = "Competitive Exams",
        forceRefresh: Boolean = false
    ): ExamContext = withContext(Dispatchers.IO) {
        val safeId = if (examId.isNotBlank()) examId else sanitizeExamId(examName)
        _discoveryState.value = ExamDiscoveryState.Loading("Preparing your exam information...")

        try {
            val context = intelligenceService.resolveExamContext(
                examId = safeId,
                examName = examName,
                category = category,
                forceRefresh = forceRefresh,
                onProgressUpdate = { stepMsg ->
                    _discoveryState.value = ExamDiscoveryState.Loading(stepMsg)
                }
            )

            _activeExamContext.value = context
            _discoveryState.value = ExamDiscoveryState.Success(context)
            Log.d(TAG, "Successfully loaded active ExamContext for: $safeId (${context.subjects.size} subjects)")
            context
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading ExamContext for $examName", e)
            val fallback = ExamContext.defaultContext().copy(
                examId = safeId,
                examName = examName,
                category = category
            )
            _activeExamContext.value = fallback
            _discoveryState.value = ExamDiscoveryState.Error(e.message ?: "Failed loading exam structure")
            fallback
        }
    }

    /**
     * Confirms changing user's active exam.
     * Preserves all completed sessions, past mock results, and historic plans.
     */
    suspend fun confirmExamChange(
        newExamId: String,
        newExamName: String,
        category: String = "Competitive Exams",
        targetDateMillis: Long = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000
    ) = withContext(Dispatchers.IO) {
        val safeId = sanitizeExamId(newExamName.ifBlank { newExamId })

        // 1. Update User Profile
        val user = userDao.getUserProfileOnce()
        if (user != null) {
            val updatedProfile = user.copy(
                examName = newExamName,
                examDateMillis = targetDateMillis
            )
            userDao.insertOrUpdateUserProfile(updatedProfile)
            syncService?.syncUserProfile(updatedProfile)
        }

        // 2. Set new Active Exam Objective
        val objective = ExamObjective(
            id = System.currentTimeMillis(),
            examName = newExamName,
            category = category,
            examDateMillis = targetDateMillis,
            targetScoreOrRank = "Top 500 Rank / 99%ile",
            status = "ACTIVE"
        )
        val id = examObjectiveDao.insertObjective(objective)
        examObjectiveDao.setActiveObjective(id)

        // 3. Load & Resolve new ExamContext
        loadExamContext(
            examId = safeId,
            examName = newExamName,
            category = category,
            forceRefresh = false
        )
    }

    private fun sanitizeExamId(raw: String): String {
        return raw.lowercase().replace("[^a-z0-9]".toRegex(), "_").trim('_').take(40).ifBlank { "railway_rrb_ntpc" }
    }
}
