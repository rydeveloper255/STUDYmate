package com.example.service.intelligence

import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * STEP 60 — SMART REVISION & KNOWLEDGE RETENTION ENGINE 2.0
 * Provides intelligent, measurable spaced revision connected to study, practice, and exam context.
 */
class SmartRevisionService(
    private val database: StudyMateDatabase
) {
    private val revisionDao = database.revisionDao()

    val allRevisionItems: Flow<List<RevisionItemEntity>> = revisionDao.getAllRevisionItems()
    val activeRevisionItems: Flow<List<RevisionItemEntity>> = revisionDao.getActiveRevisionItems()

    val dueRevisionItems: Flow<List<RevisionItemEntity>> = activeRevisionItems.map { list ->
        val now = System.currentTimeMillis()
        list.filter { item ->
            item.status == RevisionItemStatus.DUE.name ||
            (item.status == RevisionItemStatus.PENDING.name && item.scheduledAt <= now) ||
            (item.status == RevisionItemStatus.SNOOZED.name && item.scheduledAt <= now)
        }.sortedWith(
            compareBy<RevisionItemEntity> {
                when (it.priority) {
                    "URGENT" -> 1
                    "HIGH" -> 2
                    "MEDIUM" -> 3
                    else -> 4
                }
            }.thenBy { it.scheduledAt }
        )
    }

    val allRevisionSessions: Flow<List<RevisionSessionEntity>> = revisionDao.getAllRevisionSessions()

    val revisionRetentionStats: Flow<RevisionRetentionStats> = allRevisionSessions.map { sessions ->
        val completed = sessions.filter { it.status == "COMPLETED" }
        val totalSessions = sessions.size
        val totalTimeMinutes = sessions.sumOf { it.timeSpentSeconds } / 60
        val itemsReviewed = sessions.sumOf { it.itemsPlanned }
        val itemsCompleted = sessions.sumOf { it.itemsCompleted }
        val completionRate = if (totalSessions > 0) (completed.size.toFloat() / totalSessions) * 100f else 0f
        
        val scoredSessions = completed.filter { it.totalQuestions > 0 }
        val avgAccuracy = if (scoredSessions.isNotEmpty()) {
            scoredSessions.map { (it.scoreEarned.toFloat() / it.totalQuestions) * 100f }.average().toFloat()
        } else {
            0f
        }

        RevisionRetentionStats(
            totalSessions = totalSessions,
            totalTimeMinutes = totalTimeMinutes,
            itemsReviewed = itemsReviewed,
            itemsCompleted = itemsCompleted,
            completionRatePercent = completionRate,
            averageAccuracy = avgAccuracy
        )
    }

    /**
     * Compute deterministic revision priority and reason based on actual data signals.
     */
    fun calculatePriorityAndReason(
        topic: String,
        subject: String,
        practiceAccuracy: Float,
        mistakeCount: Int,
        lastReviewedAt: Long,
        daysToExam: Int,
        userPriorityOverride: String? = null
    ): Pair<String, String> {
        val now = System.currentTimeMillis()
        val daysSinceReviewed = if (lastReviewedAt > 0) {
            TimeUnit.MILLISECONDS.toDays(now - lastReviewedAt).toInt()
        } else {
            999
        }

        var score = 0
        val reasons = mutableListOf<String>()

        if (mistakeCount >= 2 || (practiceAccuracy in 0.1f..0.59f)) {
            score += 35
            reasons.add("Is topic par additional practice useful ho sakti hai (Accuracy ${(practiceAccuracy * 100).toInt()}%)")
        } else if (mistakeCount == 1) {
            score += 15
            reasons.add("1 recent practice mistake logged")
        }

        if (daysToExam in 1..15) {
            score += 30
            reasons.add("Exam in $daysToExam days")
        } else if (daysToExam in 16..30) {
            score += 15
            reasons.add("Exam approaching")
        }

        if (daysSinceReviewed >= 7) {
            score += 25
            reasons.add("Spaced repetition due ($daysSinceReviewed days ago)")
        } else if (daysSinceReviewed in 3..6) {
            score += 15
        }

        val calculatedPriority = when {
            userPriorityOverride != null && userPriorityOverride.isNotBlank() -> userPriorityOverride
            score >= 50 -> "URGENT"
            score >= 30 -> "HIGH"
            score >= 15 -> "MEDIUM"
            else -> "LOW"
        }

        val reasonText = if (reasons.isNotEmpty()) {
            reasons.joinToString(" • ")
        } else {
            "Scheduled periodic retention review"
        }

        return Pair(calculatedPriority, reasonText)
    }

    /**
     * Creates or updates a revision item safely without duplicates.
     */
    suspend fun addOrUpdateRevisionItem(
        subject: String,
        topic: String,
        examId: String = "",
        subjectId: String = "",
        topicId: String = "",
        resourceId: String? = null,
        resourceTitle: String? = null,
        sourceType: RevisionSourceType = RevisionSourceType.MANUAL,
        preferredMethod: RevisionMethodType = RevisionMethodType.QUICK_REVIEW,
        notes: String = "",
        activeRecallPrompt: String = "",
        scheduledAt: Long = System.currentTimeMillis(),
        daysToExam: Int = 30
    ): RevisionItemEntity = withContext(Dispatchers.IO) {
        val existing = revisionDao.getRevisionItemByTopic(subject, topic)
        val now = System.currentTimeMillis()

        // Fetch mistake info if any
        val mistakes = database.mistakeDao().getAllMistakesSync()
        val topicMistakes = mistakes.filter { it.topic.equals(topic, ignoreCase = true) && !it.isMastered }
        val mistakeCount = topicMistakes.size

        val (priority, reason) = calculatePriorityAndReason(
            topic = topic,
            subject = subject,
            practiceAccuracy = existing?.practiceAccuracy ?: 0.7f,
            mistakeCount = mistakeCount,
            lastReviewedAt = existing?.lastReviewedAt ?: 0L,
            daysToExam = daysToExam,
            userPriorityOverride = existing?.priority
        )

        val prompt = if (activeRecallPrompt.isNotBlank()) {
            activeRecallPrompt
        } else {
            "Explain the core concept and key rules of $topic in your own words."
        }

        val itemToSave = if (existing != null) {
            existing.copy(
                resourceId = resourceId ?: existing.resourceId,
                resourceTitle = resourceTitle ?: existing.resourceTitle,
                priority = priority,
                priorityReason = reason,
                mistakeCount = mistakeCount,
                scheduledAt = if (existing.status == RevisionItemStatus.COMPLETED.name) scheduledAt else existing.scheduledAt,
                status = if (existing.status == RevisionItemStatus.COMPLETED.name) RevisionItemStatus.DUE.name else existing.status,
                notes = if (notes.isNotBlank()) notes else existing.notes,
                preferredMethod = preferredMethod.name,
                updatedAt = now
            )
        } else {
            RevisionItemEntity(
                userId = "current_user",
                examId = examId,
                subjectId = subjectId,
                subject = subject,
                topicId = topicId,
                topic = topic,
                resourceId = resourceId,
                resourceTitle = resourceTitle,
                sourceType = sourceType.name,
                status = RevisionItemStatus.DUE.name,
                priority = priority,
                priorityReason = reason,
                scheduledAt = scheduledAt,
                mistakeCount = mistakeCount,
                activeRecallPrompt = prompt,
                notes = notes,
                preferredMethod = preferredMethod.name,
                createdAt = now,
                updatedAt = now
            )
        }

        revisionDao.insertRevisionItem(itemToSave)
        itemToSave
    }

    /**
     * Check if a proposed revision session conflicts with scheduled study blocks.
     */
    suspend fun checkScheduleConflicts(
        proposedTimeMillis: Long,
        durationMinutes: Int = 30,
        topic: String
    ): List<RevisionConflict> = withContext(Dispatchers.IO) {
        val conflicts = mutableListOf<RevisionConflict>()
        val startWindow = proposedTimeMillis
        val endWindow = proposedTimeMillis + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())

        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val proposedTimeStr = sdf.format(Date(proposedTimeMillis))

        // Check StudyPlanItems
        val planItems = database.studyPlanDao().getAllPlanItemsSync()
        for (plan in planItems) {
            if (!plan.isCompleted) {
                val planStart = plan.scheduledDateMillis
                val planEnd = planStart + TimeUnit.MINUTES.toMillis(plan.targetMinutes.toLong())
                if ((startWindow in planStart..planEnd) || (endWindow in planStart..planEnd)) {
                    conflicts.add(
                        RevisionConflict(
                            conflictTime = proposedTimeStr,
                            existingTitle = "${plan.subject} - ${plan.topic}",
                            revisionTopic = topic,
                            proposedScheduledAt = proposedTimeMillis
                        )
                    )
                }
            }
        }
        conflicts.distinctBy { it.existingTitle }
    }

    /**
     * Postpone / Snooze revision item safely without duplicating records.
     */
    suspend fun snoozeRevisionItem(
        revisionItemId: String,
        snoozeOption: SnoozeOption
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val newScheduledAt = when (snoozeOption) {
            SnoozeOption.LATER_TODAY -> now + TimeUnit.HOURS.toMillis(4)
            SnoozeOption.TOMORROW -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
                calendar.timeInMillis
            }
            is SnoozeOption.CUSTOM_DATE -> snoozeOption.dateMillis
        }

        revisionDao.snoozeRevisionItem(revisionItemId, newScheduledAt, now)
    }

    /**
     * Mark revision complete and schedule next spaced repetition interval.
     */
    suspend fun completeRevision(
        revisionItemId: String,
        scoreEarned: Int = 0,
        totalQuestions: Int = 0,
        methodUsed: RevisionMethodType = RevisionMethodType.QUICK_REVIEW,
        timeSpentSeconds: Int = 300,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val item = revisionDao.getRevisionItemById(revisionItemId) ?: return@withContext
        val now = System.currentTimeMillis()

        // Spaced Repetition interval ladder: 1 -> 3 -> 7 -> 14 -> 30 days
        val currentInterval = item.intervalDays
        val accuracy = if (totalQuestions > 0) scoreEarned.toFloat() / totalQuestions else 0.8f

        val nextIntervalDays = when {
            accuracy >= 0.8f -> when (currentInterval) {
                1 -> 3
                3 -> 7
                7 -> 14
                14 -> 30
                else -> (currentInterval * 2).coerceAtMost(45)
            }
            accuracy >= 0.6f -> (currentInterval + 2).coerceAtMost(21)
            else -> 1 // Reset on low score
        }

        val nextScheduledAt = now + TimeUnit.DAYS.toMillis(nextIntervalDays.toLong())

        revisionDao.completeRevisionItem(
            id = revisionItemId,
            newIntervalDays = nextIntervalDays,
            nextScheduledAt = nextScheduledAt,
            now = now
        )

        // Log Revision Session Entity
        val session = RevisionSessionEntity(
            userId = item.userId,
            examId = item.examId,
            subject = item.subject,
            topic = item.topic,
            revisionItemId = revisionItemId,
            startedAt = now - (timeSpentSeconds * 1000L),
            completedAt = now,
            itemsPlanned = 1,
            itemsCompleted = 1,
            timeSpentSeconds = timeSpentSeconds,
            status = "COMPLETED",
            method = methodUsed.name,
            scoreEarned = scoreEarned,
            totalQuestions = totalQuestions,
            notes = notes,
            createdAt = now
        )
        revisionDao.insertRevisionSession(session)

        // Update Topic Mastery in DB if present
        val topicMastery = database.topicMasteryDao().getTopicMasteryOnce(item.subject, item.topic)
        if (topicMastery != null) {
            val updatedScore = if (totalQuestions > 0) {
                ((topicMastery.masteryScore * 0.6f) + (accuracy * 100f * 0.4f)).toInt().coerceIn(0, 100)
            } else {
                (topicMastery.masteryScore + 5).coerceAtMost(100)
            }
            val updatedMastery = topicMastery.copy(
                masteryScore = updatedScore,
                masteryState = if (updatedScore >= 80) "MASTERED" else if (updatedScore >= 60) "INTERMEDIATE" else "WEAK",
                lastTestedMillis = now
            )
            database.topicMasteryDao().insertOrUpdateTopicMastery(updatedMastery)
        }
    }

    /**
     * Delete or remove revision item.
     */
    suspend fun removeRevisionItem(revisionItemId: String) = withContext(Dispatchers.IO) {
        revisionDao.deleteRevisionItem(revisionItemId)
    }

    /**
     * Update revision item details (priority, notes, preferred method).
     */
    suspend fun updateRevisionItem(item: RevisionItemEntity) = withContext(Dispatchers.IO) {
        revisionDao.updateRevisionItem(item.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Generates a source-grounded, privacy-safe Nova conversational response for revision queries.
     */
    suspend fun getNovaRevisionAnswer(
        userQuery: String,
        currentExam: String
    ): NovaRevisionResponse = withContext(Dispatchers.IO) {
        val lower = userQuery.trim().lowercase()
        val now = System.currentTimeMillis()

        // Get actual due items from DAO
        val mistakes: List<MistakeItem> = database.mistakeDao().getAllMistakesSync()
        val activeMistakes = mistakes.filter { !it.isMastered }

        // Fetch user preferences & profile
        val user = database.userDao().getUserProfileOnce()
        val exam = if (currentExam.isNotBlank()) currentExam else (user?.examName ?: "Competitive Exam")

        // 1. Mistake-focused query: "Jo questions galat hue the unko revise karao"
        if (lower.contains("galat") || lower.contains("mistake") || lower.contains("incorrect")) {
            val mistakeTopics: List<String> = activeMistakes.map { it.topic }.distinct().take(3)
            return@withContext if (activeMistakes.isNotEmpty()) {
                val topicListStr = mistakeTopics.joinToString(", ")
                val actions = listOf(
                    NovaContextualAction(
                        label = "🎯 Revise Mistakes ($topicListStr)",
                        actionType = NovaActionType.START_SMART_PRACTICE,
                        payload = "{\"mode\":\"MISTAKE_CORRECTION\"}",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📚 Open Revision Hub",
                        actionType = NovaActionType.OPEN_REVISION_HUB
                    )
                )
                NovaRevisionResponse(
                    answerText = "Aapke practice sessions me **${activeMistakes.size} mistakes** logged hain, mainly in **$topicListStr**.\n\nIn topics par additional practice useful ho sakti hai. Chalo mistake revision quiz start karein?",
                    actions = actions
                )
            } else {
                NovaRevisionResponse(
                    answerText = "Bahut badiya! Abhi koi unmastered practice mistakes pending nahi hain. Regular revision ya naya topic practice kar sakte ho.",
                    actions = listOf(
                        NovaContextualAction(
                            label = "📚 Open Revision Hub",
                            actionType = NovaActionType.OPEN_REVISION_HUB,
                            isPrimary = true
                        )
                    )
                )
            }
        }

        // 2. Postpone / Snooze query: "Kal ki revision postpone kar do"
        if (lower.contains("postpone") || lower.contains("snooze") || lower.contains("aage badha do")) {
            return@withContext NovaRevisionResponse(
                answerText = "Main scheduled revision ko postpone kar sakti hu. Kya aap isse **Tomorrow 9 AM** ya **Later Today** ke liye schedule karna chahte hain?",
                actions = listOf(
                    NovaContextualAction(
                        label = "⏰ Tomorrow 9 AM",
                        actionType = NovaActionType.OPEN_REVISION_HUB,
                        payload = "{\"action\":\"snooze_tomorrow\"}",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📅 Later Today",
                        actionType = NovaActionType.OPEN_REVISION_HUB,
                        payload = "{\"action\":\"snooze_today\"}"
                    )
                )
            )
        }

        // 3. Revision Progress / Retention query: "Meri revision progress batao"
        if (lower.contains("progress") || lower.contains("retention") || lower.contains("kaisa chal raha")) {
            return@withContext NovaRevisionResponse(
                answerText = "📊 **Revision & Retention Activity Summary:**\n\n- Target Exam: **$exam**\n- Daily Target: **30 mins/day**\n- Performance Metrics: Based on actual practice accuracy and spaced repetition intervals.\n\nRevision Hub me aap due topics aur upcoming scheduled reviews dekh sakte hain.",
                actions = listOf(
                    NovaContextualAction(
                        label = "🚀 Open Revision Hub",
                        actionType = NovaActionType.OPEN_REVISION_HUB,
                        isPrimary = true
                    )
                )
            )
        }

        // 4. Specific topic mention (e.g., "Percentage revise karao", "Algebra revise")
        val commonTopics = listOf("Percentage", "Algebra", "Profit and Loss", "Number System", "Ratio and Proportion", "Puzzles", "Coding Decoding", "Indian Polity", "History", "Current Affairs", "Physics", "Chemistry")
        val matchedTopic = commonTopics.firstOrNull { lower.contains(it.lowercase()) }

        if (matchedTopic != null) {
            val topicMistakes = activeMistakes.count { it.topic.equals(matchedTopic, ignoreCase = true) }
            val rationale = if (topicMistakes > 0) {
                "Is topic me $topicMistakes active practice mistakes logged hain. Is par additional practice useful ho sakti hai."
            } else {
                "Is topic ki conceptual recall aur 5 quick practice questions recommend kiye gaye hain."
            }

            val actions = listOf(
                NovaContextualAction(
                    label = "⚡ Quick Review ($matchedTopic)",
                    actionType = NovaActionType.START_SMART_REVISION,
                    payload = matchedTopic,
                    isPrimary = true
                ),
                NovaContextualAction(
                    label = "⏱️ Start Focus Session",
                    actionType = NovaActionType.START_FOCUS,
                    payload = "$matchedTopic|30"
                ),
                NovaContextualAction(
                    label = "✍️ Practice Questions",
                    actionType = NovaActionType.START_SMART_PRACTICE,
                    payload = "{\"topic\":\"$matchedTopic\"}"
                )
            )

            return@withContext NovaRevisionResponse(
                answerText = "**$matchedTopic Revision Plan:**\n\n$rationale\n\nPehle quick concept review karein ya direct practice questions se start karein?",
                actions = actions
            )
        }

        // 5. Default today's revision query: "Nova, aaj kya revise karna hai?" / "Meri revision list dikhao"
        NovaRevisionResponse(
            answerText = "📚 **Today's Revision Plan:**\n\nAapke schedule aur practice history ke hisab se revision queue ready hai.\n\nJab aap ready ho, pehle due topics se start karein?",
            actions = listOf(
                NovaContextualAction(
                    label = "🔄 Open Revision Hub",
                    actionType = NovaActionType.OPEN_REVISION_HUB,
                    isPrimary = true
                ),
                NovaContextualAction(
                    label = "🎯 Revise Mistakes",
                    actionType = NovaActionType.START_SMART_PRACTICE,
                    payload = "{\"mode\":\"MISTAKE_CORRECTION\"}"
                )
            )
        )
    }
}

data class NovaRevisionResponse(
    val answerText: String,
    val actions: List<NovaContextualAction> = emptyList()
)

sealed class SnoozeOption {
    object LATER_TODAY : SnoozeOption()
    object TOMORROW : SnoozeOption()
    data class CUSTOM_DATE(val dateMillis: Long) : SnoozeOption()
}
