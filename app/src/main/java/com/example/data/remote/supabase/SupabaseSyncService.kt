package com.example.data.remote.supabase

import android.util.Log
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

sealed class SupabaseSyncStatus {
    object Idle : SupabaseSyncStatus()
    object Syncing : SupabaseSyncStatus()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SupabaseSyncStatus()
    data class Error(val message: String, val throwable: Throwable? = null) : SupabaseSyncStatus()
}

/**
 * Offline-First Bi-directional Synchronization Engine with Supabase.
 * Strictly adheres to guidelines:
 * - Room is the local source of truth for instant UI responsiveness.
 * - Changes are synced asynchronously in the background.
 * - If offline or unconfigured, functions return gracefully without crashes.
 * - Avoids duplicate records using Postgres upsert with on_conflict.
 */
class SupabaseSyncService(
    private val client: SupabaseClient,
    private val authManager: SupabaseAuthManager,
    private val database: StudyMateDatabase
) {
    private val TAG = "SupabaseSyncService"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<SupabaseSyncStatus>(SupabaseSyncStatus.Idle)
    val syncStatus: StateFlow<SupabaseSyncStatus> = _syncStatus.asStateFlow()

    private val userId: String get() = authManager.getStoredUserId()
    private val token: String? get() = authManager.getAccessToken()

    private val todayDateString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // --- 1. PROFILES & USER SETTINGS ---

    fun syncUserProfile(user: UserProfile, notifPrefs: NotificationPreference? = null) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                _syncStatus.value = SupabaseSyncStatus.Syncing
                val uid = if (user.uid.isNotBlank()) user.uid else userId
                authManager.associateFirebaseOrLocalUser(uid, user.email)

                val profileJson = JSONObject().apply {
                    put("id", uid)
                    put("email", user.email)
                    put("name", user.name)
                    put("photo_url", user.photoUrl ?: JSONObject.NULL)
                    put("grade", user.grade)
                    put("education_level", user.educationLevel)
                    put("language_preference", user.languagePreference)
                    put("exam_category", user.examCategory)
                    put("exam_name", user.examName)
                    put("exam_date_millis", user.examDateMillis)
                    put("target_score", user.targetScore)
                    put("goal", user.goal)
                    put("subjects", JSONArray(user.subjects))
                    put("high_priority_subjects", JSONArray(user.highPrioritySubjects))
                    put("medium_priority_subjects", JSONArray(user.mediumPrioritySubjects))
                    put("low_priority_subjects", JSONArray(user.lowPrioritySubjects))
                    put("strong_subjects", JSONArray(user.strongSubjects))
                    put("weak_subjects", JSONArray(user.weakSubjects))
                    put("weak_topics", JSONArray(user.weakTopics))
                    put("preparation_level", user.preparationLevel)
                    put("daily_target_minutes", user.dailyTargetMinutes)
                    put("available_study_hours", user.availableStudyHours.toDouble())
                    put("preferred_study_start_time", user.preferredStudyStartTime)
                    put("preferred_study_end_time", user.preferredStudyEndTime)
                    put("preferred_study_days", JSONArray(user.preferredStudyDays))
                    put("break_duration_minutes", user.breakDurationMinutes)
                    put("preferred_study_time", user.preferredStudyTime)
                    put("morning_night_preference", user.morningNightPreference)
                    put("revision_frequency", user.revisionFrequency)
                    put("mock_test_frequency", user.mockTestFrequency)
                    put("daily_study_goal", user.dailyStudyGoal)
                    put("short_term_goal", user.shortTermGoal)
                    put("long_term_goal", user.longTermGoal)
                    put("notifications_enabled", user.notificationsEnabled)
                    put("xp", user.xp)
                    put("level", user.level)
                    put("streak_days", user.streakDays)
                    put("total_focus_minutes", user.totalFocusMinutes)
                    put("total_questions_solved", user.totalQuestionsSolved)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()))
                }

                val res = client.from("profiles").upsert(
                    jsonBody = profileJson.toString(),
                    onConflict = "id",
                    accessToken = token
                )

                if (notifPrefs != null) {
                    syncUserSettings(user, notifPrefs)
                }

                if (res.isSuccess) {
                    _syncStatus.value = SupabaseSyncStatus.Success("Profile synced to cloud")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing user profile to Supabase: ${e.message}")
                _syncStatus.value = SupabaseSyncStatus.Error(e.message ?: "Sync error", e)
            }
        }
    }

    suspend fun syncUserSettings(user: UserProfile, notifPrefs: NotificationPreference) = withContext(Dispatchers.IO) {
        if (!client.isReady()) return@withContext
        try {
            val settingsJson = JSONObject().apply {
                put("user_id", userId)
                put("theme", "dark")
                put("tts_speed", 1.0)
                put("tts_voice", "en-US-Standard-C")
                put("sound_enabled", true)
                put("vibration_enabled", true)
                put("notification_preferences", JSONObject().apply {
                    put("master_enabled", notifPrefs.masterEnabled)
                    put("study_reminders", notifPrefs.studyReminders)
                    put("exam_countdown_alerts", notifPrefs.examCountdownAlerts)
                    put("daily_goal_reminders", notifPrefs.dailyGoalReminders)
                    put("missed_study_reminders", notifPrefs.missedStudyReminders)
                    put("break_reminders", notifPrefs.breakReminders)
                    put("focus_reminders", notifPrefs.focusReminders)
                    put("streak_alerts", notifPrefs.streakAlerts)
                    put("motivational_quotes", notifPrefs.motivationalQuotes)
                    put("weekly_report", notifPrefs.weeklyReport)
                    put("reminder_time", "${notifPrefs.reminderHour}:${notifPrefs.reminderMinute}")
                    put("quiet_start", "${notifPrefs.quietStartHour}:${notifPrefs.quietStartMinute}")
                    put("quiet_end", "${notifPrefs.quietEndHour}:${notifPrefs.quietEndMinute}")
                })
                put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()))
            }

            client.from("user_settings").upsert(
                jsonBody = settingsJson.toString(),
                onConflict = "user_id",
                accessToken = token
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing user_settings", e)
        }
    }

    // --- 2. EXAMS & OBJECTIVES & TOPIC MASTERY ---

    fun syncExamObjective(objective: ExamObjective) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val examJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", objective.id)
                    put("name", objective.examName)
                    put("category", objective.category)
                    put("target_score", objective.targetScoreOrRank)
                    put("target_date_millis", objective.examDateMillis)
                    put("target_weekly_hours", objective.targetWeeklyStudyHours.toDouble())
                    put("total_syllabus_topics", objective.totalSyllabusTopicsCount)
                    put("completed_syllabus_topics", objective.completedSyllabusTopicsCount)
                    put("priority_subjects", JSONArray(objective.prioritySubjects))
                    put("status", objective.status)
                }

                client.from("exams").upsert(
                    jsonBody = examJson.toString(),
                    onConflict = "user_id,name",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing exam objective", e)
            }
        }
    }

    fun syncTopicMastery(mastery: TopicMastery) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val masteryJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", mastery.id)
                    put("subject", mastery.subject)
                    put("topic", mastery.topic)
                    put("mastery_score", mastery.masteryScore)
                    put("accuracy_percent", mastery.accuracyPercent.toDouble())
                    put("total_attempted", mastery.totalQuestionsAttempted)
                    put("correct_count", mastery.correctQuestionsCount)
                    put("incorrect_count", mastery.incorrectQuestionsCount)
                    put("easy_solved", mastery.easySolved)
                    put("med_solved", mastery.medSolved)
                    put("hard_solved", mastery.hardSolved)
                    put("retention_decay_rate", mastery.retentionDecayRate.toDouble())
                    put("mastery_level", mastery.masteryLevel)
                    put("weak_spots", JSONArray(mastery.weakSpots))
                    put("last_tested_at", mastery.lastTestedMillis)
                    put("recommended_review_at", mastery.recommendedReviewDateMillis)
                }

                client.from("topic_progress").upsert(
                    jsonBody = masteryJson.toString(),
                    onConflict = "user_id,subject,topic",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing topic progress", e)
            }
        }
    }

    // --- 3. STUDY PLANNER & TASKS ---

    fun syncStudyPlanItem(item: StudyPlanItem) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val taskJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", item.id)
                    put("subject", item.subject)
                    put("chapter", item.chapter)
                    put("topic", item.topic)
                    put("target_minutes", item.targetMinutes)
                    put("is_completed", item.isCompleted)
                    put("scheduled_date_millis", item.scheduledDateMillis)
                    put("priority", item.priority.name)
                    put("notes", item.notes)
                }

                client.from("study_tasks").upsert(
                    jsonBody = taskJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing study task", e)
            }
        }
    }

    fun deleteStudyPlanItem(localId: Long) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                client.from("study_tasks").delete(
                    queryParams = mapOf("user_id" to "eq.$userId", "local_id" to "eq.$localId"),
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting study task in cloud", e)
            }
        }
    }

    // --- 4. STUDY SESSIONS & FOCUS SESSIONS ---

    fun syncStudySessionHistory(session: StudentSessionHistory) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val sessionJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", session.id)
                    put("session_type", session.sessionType)
                    put("subject", session.subject)
                    put("topic", session.topic)
                    put("duration_minutes", session.durationMinutes)
                    put("actual_minutes_spent", session.actualMinutesSpent)
                    put("xp_earned", session.xpEarned)
                    put("accuracy_percent", session.accuracyPercent?.toDouble() ?: JSONObject.NULL)
                    put("questions_attempted", session.questionsAttempted)
                    put("productivity_rating", session.productivityRating)
                    put("notes_summary", session.notesSummary)
                    put("timestamp", session.timestamp)
                }

                client.from("study_sessions").upsert(
                    jsonBody = sessionJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing study session", e)
            }
        }
    }

    fun syncFocusSession(focus: FocusSession) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val focusJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", focus.id)
                    put("subject", focus.subject)
                    put("topic", focus.topic)
                    put("duration_minutes", focus.durationMinutes)
                    put("actual_minutes_spent", focus.actualMinutesSpent)
                    put("xp_earned", focus.xpEarned)
                    put("is_completed", focus.isCompleted)
                    put("timestamp", focus.timestamp)
                }

                client.from("focus_sessions").upsert(
                    jsonBody = focusJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing focus session", e)
            }
        }
    }

    // --- 5. TESTS & MOCK ATTEMPTS & MISTAKES ---

    fun syncMockTestAttempt(attempt: MockTestAttempt) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val attemptJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", attempt.id)
                    put("title", attempt.title)
                    put("subject", attempt.subject)
                    put("exam_name", attempt.examName)
                    put("topic", attempt.topic)
                    put("difficulty", attempt.difficulty)
                    put("score", attempt.score)
                    put("total_questions", attempt.totalQuestions)
                    put("correct_count", attempt.correctCount)
                    put("incorrect_count", attempt.incorrectCount)
                    put("skipped_count", attempt.skippedCount)
                    put("accuracy_percent", attempt.accuracyPercent.toDouble())
                    put("time_spent_seconds", attempt.timeSpentSeconds)
                    put("weak_topics", JSONArray(attempt.weakTopics))
                    put("strong_topics", JSONArray(attempt.strongTopics))
                    put("ai_recommendation", attempt.aiRecommendation)
                    put("marking_scheme", attempt.markingScheme)
                    put("timestamp", attempt.timestamp)
                }

                client.from("test_attempts").upsert(
                    jsonBody = attemptJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing test attempt", e)
            }
        }
    }

    fun deleteMockTestAttempt(localId: Long) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                client.from("test_attempts").delete(
                    queryParams = mapOf("user_id" to "eq.$userId", "local_id" to "eq.$localId"),
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting test attempt from cloud", e)
            }
        }
    }

    fun syncMistakeItem(mistake: MistakeItem) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val mistakeJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", mistake.id)
                    put("question_text", mistake.questionText)
                    put("student_answer", mistake.studentAnswer)
                    put("correct_answer", mistake.correctAnswer)
                    put("subject", mistake.subject)
                    put("topic", mistake.topic)
                    put("mistake_category", mistake.mistakeCategory)
                    put("explanation", mistake.explanation)
                    put("is_mastered", mistake.isMastered)
                    put("timestamp", mistake.timestamp)
                }

                client.from("question_attempts").upsert(
                    jsonBody = mistakeJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing mistake item", e)
            }
        }
    }

    fun syncMistake(mistake: MistakeItem) = syncMistakeItem(mistake)

    // --- 6. REVISION ITEMS (FLASHCARDS) ---

    fun syncFlashcard(card: FlashcardItem) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val cardJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", card.id)
                    put("subject", card.subject)
                    put("topic", card.topic)
                    put("front", card.front)
                    put("back", card.back)
                    put("hint", card.hint)
                    put("difficulty", card.difficulty)
                    put("status", card.status.name)
                    put("confidence", card.confidence)
                    put("review_count", card.reviewCount)
                    put("last_reviewed", card.lastReviewed)
                    put("interval_days", card.intervalDays)
                    put("ease_factor", card.easeFactor.toDouble())
                    put("repetitions", card.repetitions)
                    put("next_review_date", card.nextReviewDate)
                    put("source_doc_title", card.sourceDocTitle)
                    put("created_at", card.createdAt)
                }

                client.from("revision_items").upsert(
                    jsonBody = cardJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing flashcard", e)
            }
        }
    }

    fun deleteFlashcard(localId: Long) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                client.from("revision_items").delete(
                    queryParams = mapOf("user_id" to "eq.$userId", "local_id" to "eq.$localId"),
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting revision item in cloud", e)
            }
        }
    }

    // --- 7. NOTES & ATTACHMENTS (WITH SUPABASE STORAGE) ---

    fun syncSmartNote(note: SmartNoteItem) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val noteJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", note.id)
                    put("title", note.title)
                    put("subject", note.subject)
                    put("topic", note.topic)
                    put("content_markdown", note.contentMarkdown)
                    put("key_points", JSONArray(note.keyPoints))
                    put("formulas", JSONArray(note.formulas))
                    put("important_facts", JSONArray(note.importantFacts))
                    put("source_url", note.sourceUrl)
                    put("source_title", note.sourceTitle)
                    put("is_bookmarked", note.isBookmarked)
                    put("is_revised", note.isRevised)
                    put("revision_category", note.revisionCategory.name)
                    put("created_at", note.createdAt)
                }

                client.from("notes").upsert(
                    jsonBody = noteJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing smart note", e)
            }
        }
    }

    fun deleteSmartNote(localId: Long) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                client.from("notes").delete(
                    queryParams = mapOf("user_id" to "eq.$userId", "local_id" to "eq.$localId"),
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting note from cloud", e)
            }
        }
    }

    suspend fun uploadNoteAttachment(
        noteId: Long,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        if (!client.isReady()) return@withContext SupabaseResult.Error("Supabase not configured")

        val path = "users/$userId/notes/${noteId}_${System.currentTimeMillis()}_$fileName"
        val res = client.uploadFile(
            bucket = "note_attachments",
            path = path,
            mimeType = mimeType,
            fileBytes = fileBytes,
            accessToken = token
        )

        if (res is SupabaseResult.Success) {
            val publicUrl = res.data
            // Register in note_attachments table
            val attachmentRecord = JSONObject().apply {
                put("user_id", userId)
                put("note_local_id", noteId)
                put("file_name", fileName)
                put("file_type", mimeType)
                put("file_url", publicUrl)
                put("file_size_bytes", fileBytes.size)
            }
            client.from("note_attachments").insert(attachmentRecord.toString(), accessToken = token)
        }

        res
    }

    // --- 8. NOVA MEMORY & CONVERSATIONS ---

    fun syncNovaMemory(memory: NovaMemoryItem) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val memJson = JSONObject().apply {
                    put("user_id", userId)
                    put("local_id", memory.id)
                    put("memory_key", memory.key)
                    put("content", memory.value)
                    put("category", memory.category.name)
                    put("source_context", memory.source)
                    put("is_active", memory.isEnabled)
                    put("created_at", memory.timestamp)
                }

                client.from("nova_memory").upsert(
                    jsonBody = memJson.toString(),
                    onConflict = "user_id,local_id",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing nova memory", e)
            }
        }
    }

    fun deleteNovaMemory(localId: Long) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                client.from("nova_memory").delete(
                    queryParams = mapOf("user_id" to "eq.$userId", "local_id" to "eq.$localId"),
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting nova memory in cloud", e)
            }
        }
    }

    fun recordNovaChatMessage(sender: String, messageText: String, reasoning: String? = null) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val msgJson = JSONObject().apply {
                    put("user_id", userId)
                    put("sender", sender)
                    put("text", messageText)
                    put("reasoning_content", reasoning ?: JSONObject.NULL)
                    put("created_at", System.currentTimeMillis())
                }

                client.from("nova_messages").insert(msgJson.toString(), accessToken = token, returnRepresentation = false)
            } catch (e: Exception) {
                Log.w(TAG, "Error logging nova message", e)
            }
        }
    }

    // --- 9. INSIGHTS & RECOMMENDATIONS ---

    fun syncIntelligenceSnapshot(snapshot: IntelligenceSnapshot) {
        if (!client.isReady()) return
        syncScope.launch {
            try {
                val insightJson = JSONObject().apply {
                    put("user_id", userId)
                    put("timestamp", snapshot.timestamp)
                    put("exam_days_remaining", snapshot.examDaysRemaining)
                    put("overall_mastery_score", snapshot.overallMasteryScore)
                    put("syllabus_completion_percent", snapshot.syllabusCompletionPercent)
                    put("readiness_index", snapshot.readinessIndex.toDouble())
                    put("top_recommended_action", snapshot.topRecommendedActionTitle)
                    put("top_recommended_subject", snapshot.topRecommendedSubject)
                    put("pacing_status", snapshot.pacingStatus)
                    put("insights_summary", snapshot.insightsSummary)
                    put("weak_topics_count", snapshot.weakTopicsCount)
                    put("mastered_topics_count", snapshot.masteredTopicsCount)
                }

                client.from("student_insights").insert(insightJson.toString(), accessToken = token, returnRepresentation = false)
            } catch (e: Exception) {
                Log.w(TAG, "Error logging student insights", e)
            }
        }
    }

    // --- 10. APP USAGE SUMMARY (PRIVACY-PRESERVING) ---

    fun syncAppUsageDaily(usageData: List<Map<String, Any?>>) {
        if (!client.isReady() || usageData.isEmpty()) return
        syncScope.launch {
            try {
                val arr = JSONArray()
                usageData.forEach { map ->
                    val obj = JSONObject(map).apply {
                        put("user_id", userId)
                        put("date_string", todayDateString)
                    }
                    arr.put(obj)
                }

                client.from("app_usage_daily").upsert(
                    jsonBody = arr.toString(),
                    onConflict = "user_id,package_name,date_string",
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing app usage summary", e)
            }
        }
    }

    // --- 11. FULL CLOUD RESTORE / SYNC WHEN LOGGING IN ---

    suspend fun fullCloudRestore(): Boolean = withContext(Dispatchers.IO) {
        if (!client.isReady()) return@withContext false
        try {
            _syncStatus.value = SupabaseSyncStatus.Syncing

            // 1. Fetch Profile
            val profileRes = client.from("profiles").select(mapOf("id" to "eq.$userId"), accessToken = token)
            if (profileRes is SupabaseResult.Success) {
                val array = JSONArray(profileRes.data)
                if (array.length() > 0) {
                    val p = array.getJSONObject(0)
                    val local = database.userDao().getUserProfileOnce()
                    val restored = (local ?: UserProfile(id = "current_user")).copy(
                        name = p.optString("name", local?.name ?: "Student"),
                        email = p.optString("email", local?.email ?: ""),
                        grade = p.optString("grade", local?.grade ?: "Class 12"),
                        examName = p.optString("exam_name", local?.examName ?: "JEE"),
                        examDateMillis = p.optLong("exam_date_millis", local?.examDateMillis ?: System.currentTimeMillis()),
                        targetScore = p.optString("target_score", local?.targetScore ?: "Top 500 AIR"),
                        xp = p.optInt("xp", local?.xp ?: 350),
                        level = p.optInt("level", local?.level ?: 2),
                        streakDays = p.optInt("streak_days", local?.streakDays ?: 4),
                        totalFocusMinutes = p.optInt("total_focus_minutes", local?.totalFocusMinutes ?: 135),
                        isOnboardingCompleted = true
                    )
                    database.userDao().insertOrUpdateUserProfile(restored)
                }
            }

            // 2. Fetch Flashcards / Revision Items
            val revRes = client.from("revision_items").select(mapOf("user_id" to "eq.$userId"), accessToken = token)
            if (revRes is SupabaseResult.Success) {
                val array = JSONArray(revRes.data)
                val cards = mutableListOf<FlashcardItem>()
                for (i in 0 until array.length()) {
                    val c = array.getJSONObject(i)
                    cards.add(
                        FlashcardItem(
                            id = c.optLong("local_id", 0L),
                            subject = c.optString("subject", "Physics"),
                            topic = c.optString("topic", "General"),
                            front = c.optString("front", ""),
                            back = c.optString("back", ""),
                            hint = c.optString("hint", ""),
                            difficulty = c.optString("difficulty", "Medium"),
                            status = try { RevisionCategory.valueOf(c.optString("status", "PRACTICE_SOON")) } catch (e: Exception) { RevisionCategory.PRACTICE_SOON },
                            confidence = c.optInt("confidence", 3),
                            intervalDays = c.optInt("interval_days", 1),
                            easeFactor = c.optDouble("ease_factor", 2.5).toFloat(),
                            repetitions = c.optInt("repetitions", 0),
                            nextReviewDate = c.optLong("next_review_date", System.currentTimeMillis()),
                            createdAt = c.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
                if (cards.isNotEmpty()) {
                    database.flashcardDao().insertFlashcards(cards)
                }
            }

            // 3. Fetch Smart Notes
            val notesRes = client.from("notes").select(mapOf("user_id" to "eq.$userId"), accessToken = token)
            if (notesRes is SupabaseResult.Success) {
                val array = JSONArray(notesRes.data)
                for (i in 0 until array.length()) {
                    val n = array.getJSONObject(i)
                    val noteItem = SmartNoteItem(
                        id = n.optLong("local_id", 0L),
                        title = n.optString("title", "Smart Note"),
                        subject = n.optString("subject", "Physics"),
                        topic = n.optString("topic", "General"),
                        contentMarkdown = n.optString("content_markdown", ""),
                        isBookmarked = n.optBoolean("is_bookmarked", false),
                        isRevised = n.optBoolean("is_revised", false),
                        createdAt = n.optLong("created_at", System.currentTimeMillis())
                    )
                    database.smartNoteDao().insertSmartNote(noteItem)
                }
            }

            _syncStatus.value = SupabaseSyncStatus.Success("Full cloud sync completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in fullCloudRestore", e)
            _syncStatus.value = SupabaseSyncStatus.Error(e.message ?: "Cloud restore error", e)
            false
        }
    }
}
