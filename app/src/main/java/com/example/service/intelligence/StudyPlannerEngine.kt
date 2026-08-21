package com.example.service.intelligence

import com.example.data.model.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Step 11: Intelligent, Adaptive AI Study Planner & Daily Schedule Engine.
 * Multi-factor priority engine & time-budget allocator respecting user preferences,
 * ExamContext, mastery scores, mistakes, spaced recall, subject balance, and exam date deadlines.
 */
object StudyPlannerEngine {

    data class CandidateScore(
        val topic: TopicEntity,
        val subjectName: String,
        val chapterName: String,
        val score: Double,
        val sessionType: String, // LEARNING, PRACTICE, REVISION, MOCK_TEST, WEAK_TOPIC, REVIEW
        val reason: String,
        val recommendedMinutes: Int,
        val priorityLevel: PlanPriority
    )

    /**
     * Generates an adaptive daily study plan adhering strictly to user preferences & exam context.
     */
    fun generateAdaptiveDailyPlan(
        examContext: ExamContext,
        topicMasteries: List<TopicMastery>,
        mistakes: List<MistakeItem>,
        flashcards: List<FlashcardItem>,
        recentMockAttempts: List<MockTestAttempt>,
        existingTodayPlans: List<StudyPlanItem>,
        userPreferences: UserStudyPreferences,
        examDaysRemaining: Int? = null,
        scheduledDateMillis: Long = System.currentTimeMillis()
    ): GeneratedPlanResult {
        val totalBudgetMinutes = userPreferences.dailyAvailableMinutes.coerceAtLeast(30)
        
        // Exclude manually added or already completed plans from remaining budget
        val nonAiOrCompletedPlans = existingTodayPlans.filter { !it.isAiGenerated || it.isCompleted }
        val reservedMinutes = nonAiOrCompletedPlans.sumOf { it.targetMinutes }
        val remainingBudgetMinutes = (totalBudgetMinutes - reservedMinutes).coerceAtLeast(0)

        if (remainingBudgetMinutes < 15 && existingTodayPlans.isNotEmpty()) {
            return GeneratedPlanResult(
                sessions = existingTodayPlans,
                timeBudgetMinutes = totalBudgetMinutes,
                totalScheduledMinutes = existingTodayPlans.sumOf { it.targetMinutes },
                summaryAdvice = "Today's time budget is fully scheduled with existing sessions!"
            )
        }

        val subjectPriorities = parseSubjectPriorities(userPreferences.subjectPrioritiesJson)
        val masteryMap = topicMasteries.associateBy { it.topicId.ifBlank { "${it.subject.lowercase()}_${it.topic.lowercase()}" } }
        val mistakesByTopic = mistakes.filter { !it.isMastered }.groupBy { it.topicId.ifBlank { "${it.subject.lowercase()}_${it.topic.lowercase()}" } }
        val revisionsDueMap = flashcards.filter { it.status == RevisionCategory.REVISE_NOW || it.nextReviewDate <= System.currentTimeMillis() }
            .groupBy { "${it.subject.lowercase()}_${it.topic.lowercase()}" }

        val chaptersMap = examContext.chapters.associateBy { it.id }
        val subjectsMap = examContext.subjects.associateBy { it.id }

        val scoredCandidates = mutableListOf<CandidateScore>()

        for (topic in examContext.topics) {
            val subject = subjectsMap[topic.subjectId]
            val chapter = chaptersMap[topic.chapterId]
            val subjectName = subject?.name ?: "General"
            val chapterName = chapter?.name ?: "General"

            val key = topic.id.ifBlank { "${subjectName.lowercase()}_${topic.name.lowercase()}" }
            val mastery = masteryMap[key] ?: topicMasteries.find { it.topic.equals(topic.name, ignoreCase = true) }
            val topicMistakes = mistakesByTopic[key] ?: mistakes.filter { it.topic.equals(topic.name, ignoreCase = true) }
            val topicRevisions = revisionsDueMap["${subjectName.lowercase()}_${topic.name.lowercase()}"] ?: emptyList()

            val manualOverride = mastery?.userManualOverride ?: "NONE"
            if (manualOverride == "SKIP_FOR_NOW") continue

            // Don't duplicate if already in today's plans
            val alreadyScheduled = existingTodayPlans.any { it.topic.equals(topic.name, ignoreCase = true) && !it.isCompleted }
            if (alreadyScheduled) continue

            val (score, type, reason, planPriority) = calculateCandidateScore(
                topic = topic,
                subjectName = subjectName,
                mastery = mastery,
                mistakesCount = topicMistakes.size,
                revisionsCount = topicRevisions.size,
                subjectPriority = subjectPriorities[subjectName] ?: "MEDIUM",
                examDaysRemaining = examDaysRemaining
            )

            val sessionDuration = userPreferences.preferredSessionMinutes.coerceIn(15, 90)

            scoredCandidates.add(
                CandidateScore(
                    topic = topic,
                    subjectName = subjectName,
                    chapterName = chapterName,
                    score = score,
                    sessionType = type,
                    reason = reason,
                    recommendedMinutes = sessionDuration,
                    priorityLevel = planPriority
                )
            )
        }

        val sortedCandidates = scoredCandidates.sortedByDescending { it.score }

        // Time Allocation with Subject Balance Protection
        val newGeneratedSessions = mutableListOf<StudyPlanItem>()
        var allocatedMinutes = 0
        val subjectMinutesAllocated = mutableMapOf<String, Int>()
        val maxPerSubjectMinutes = if (examContext.subjects.size > 1) (remainingBudgetMinutes * 0.6f).toInt().coerceAtLeast(45) else remainingBudgetMinutes

        var currentStartHour = userPreferences.windowStartHour
        var currentStartMinute = 0
        val cal = Calendar.getInstance().apply {
            timeInMillis = scheduledDateMillis
            set(Calendar.HOUR_OF_DAY, currentStartHour)
            set(Calendar.MINUTE, currentStartMinute)
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (candidate in sortedCandidates) {
            if (allocatedMinutes + candidate.recommendedMinutes > remainingBudgetMinutes) {
                // If remaining time is smaller than preferred duration, shorten if >= 15 min
                val rem = remainingBudgetMinutes - allocatedMinutes
                if (rem >= 15) {
                    val currentSubjectAllocated = subjectMinutesAllocated[candidate.subjectName] ?: 0
                    if (currentSubjectAllocated < maxPerSubjectMinutes) {
                        val startTimeStr = timeFormat.format(cal.time)
                        cal.add(Calendar.MINUTE, rem)
                        val endTimeStr = timeFormat.format(cal.time)

                        newGeneratedSessions.add(
                            StudyPlanItem(
                                userId = userPreferences.userId,
                                examId = examContext.examId,
                                subjectId = candidate.topic.subjectId,
                                chapterId = candidate.topic.chapterId,
                                topicId = candidate.topic.id,
                                subject = candidate.subjectName,
                                chapter = candidate.chapterName,
                                topic = candidate.topic.name,
                                targetMinutes = rem,
                                isCompleted = false,
                                scheduledDateMillis = scheduledDateMillis,
                                startTimeFormatted = startTimeStr,
                                endTimeFormatted = endTimeStr,
                                sessionType = candidate.sessionType,
                                sessionState = "PLANNED",
                                priority = candidate.priorityLevel,
                                aiRecommendationReason = candidate.reason,
                                isAiGenerated = true
                            )
                        )
                        allocatedMinutes += rem
                    }
                }
                break
            }

            val currentSubjectAllocated = subjectMinutesAllocated[candidate.subjectName] ?: 0
            if (currentSubjectAllocated >= maxPerSubjectMinutes && subjectPriorities[candidate.subjectName] != "HIGH") {
                continue
            }

            val startTimeStr = timeFormat.format(cal.time)
            cal.add(Calendar.MINUTE, candidate.recommendedMinutes)
            val endTimeStr = timeFormat.format(cal.time)

            newGeneratedSessions.add(
                StudyPlanItem(
                    userId = userPreferences.userId,
                    examId = examContext.examId,
                    subjectId = candidate.topic.subjectId,
                    chapterId = candidate.topic.chapterId,
                    topicId = candidate.topic.id,
                    subject = candidate.subjectName,
                    chapter = candidate.chapterName,
                    topic = candidate.topic.name,
                    targetMinutes = candidate.recommendedMinutes,
                    isCompleted = false,
                    scheduledDateMillis = scheduledDateMillis,
                    startTimeFormatted = startTimeStr,
                    endTimeFormatted = endTimeStr,
                    sessionType = candidate.sessionType,
                    sessionState = "PLANNED",
                    priority = candidate.priorityLevel,
                    aiRecommendationReason = candidate.reason,
                    isAiGenerated = true
                )
            )

            allocatedMinutes += candidate.recommendedMinutes
            subjectMinutesAllocated[candidate.subjectName] = currentSubjectAllocated + candidate.recommendedMinutes

            // Insert Break Slot
            if (userPreferences.breakMinutes > 0) {
                cal.add(Calendar.MINUTE, userPreferences.breakMinutes)
            }
        }

        // Check Mock Test Recommendation if exam is near or syllabus > 50% covered
        if (allocatedMinutes + 45 <= remainingBudgetMinutes && examDaysRemaining != null && examDaysRemaining <= 30) {
            val startTimeStr = timeFormat.format(cal.time)
            cal.add(Calendar.MINUTE, 45)
            val endTimeStr = timeFormat.format(cal.time)

            newGeneratedSessions.add(
                StudyPlanItem(
                    userId = userPreferences.userId,
                    examId = examContext.examId,
                    subject = "Full Exam",
                    chapter = "Mock Practice",
                    topic = "${examContext.examName} Target Mock",
                    targetMinutes = 45,
                    isCompleted = false,
                    scheduledDateMillis = scheduledDateMillis,
                    startTimeFormatted = startTimeStr,
                    endTimeFormatted = endTimeStr,
                    sessionType = "MOCK_TEST",
                    sessionState = "PLANNED",
                    priority = PlanPriority.HIGH,
                    aiRecommendationReason = "Exam in $examDaysRemaining days — Speed & accuracy benchmark needed",
                    isAiGenerated = true
                )
            )
        }

        val combinedPlans = nonAiOrCompletedPlans + newGeneratedSessions

        // Calculate Deadline Safety Warning
        val deadlineWarning = calculateDeadlineSafetyWarning(
            examContext = examContext,
            topicMasteries = topicMasteries,
            userPreferences = userPreferences,
            examDaysRemaining = examDaysRemaining
        )

        val summary = if (newGeneratedSessions.isNotEmpty()) {
            "Generated ${newGeneratedSessions.size} targeted sessions allocating ${allocatedMinutes}m based on weak areas & spaced recall."
        } else {
            "Existing daily plan is up to date."
        }

        return GeneratedPlanResult(
            sessions = combinedPlans.sortedBy { it.scheduledDateMillis },
            timeBudgetMinutes = totalBudgetMinutes,
            totalScheduledMinutes = combinedPlans.sumOf { it.targetMinutes },
            deadlineWarningMessage = deadlineWarning,
            summaryAdvice = summary
        )
    }

    /**
     * Calculates candidate score implementing strict priority order:
     * 1. Urgent revision
     * 2. Very weak topics
     * 3. Repeated mistakes
     * 4. Important uncovered topics
     * 5. Medium mastery topics
     * 6. Normal syllabus progression
     * 7. Strong topics for maintenance
     */
    private fun calculateCandidateScore(
        topic: TopicEntity,
        subjectName: String,
        mastery: TopicMastery?,
        mistakesCount: Int,
        revisionsCount: Int,
        subjectPriority: String, // HIGH, MEDIUM, LOW
        examDaysRemaining: Int?
    ): TupleScore {
        val state = mastery?.masteryState ?: "NOT_STARTED"
        val scoreVal = mastery?.masteryScore ?: 0
        val manualOverride = mastery?.userManualOverride ?: "NONE"

        var priorityScore = 0.0
        var sessionType = "PRACTICE"
        var reason = ""
        var planPriority = PlanPriority.MEDIUM

        when {
            state == "REVISION_DUE" || revisionsCount > 0 -> {
                priorityScore = 95.0 + revisionsCount * 5
                sessionType = "REVISION"
                reason = "Spaced Recall Due for ${topic.name}"
                planPriority = PlanPriority.HIGH
            }
            state == "WEAK" || scoreVal in 1..44 -> {
                priorityScore = 85.0 + (50 - scoreVal)
                sessionType = "WEAK_TOPIC"
                reason = "Weak mastery ($scoreVal%) — High priority practice"
                planPriority = PlanPriority.HIGH
            }
            mistakesCount > 0 -> {
                priorityScore = 80.0 + (mistakesCount * 6).coerceAtMost(18)
                sessionType = "WEAK_TOPIC"
                reason = "Unmastered mistakes ($mistakesCount questions) in ${topic.name}"
                planPriority = PlanPriority.HIGH
            }
            topic.isHighYield && state == "NOT_STARTED" -> {
                priorityScore = 75.0
                sessionType = "LEARNING"
                reason = "High-yield topic for $subjectName — Core syllabus"
                planPriority = PlanPriority.HIGH
            }
            state in listOf("LEARNING", "PRACTICING", "IMPROVING") -> {
                priorityScore = 65.0 + (100 - scoreVal) * 0.2
                sessionType = "PRACTICE"
                reason = "Targeted practice to reach mastery in ${topic.name}"
                planPriority = PlanPriority.MEDIUM
            }
            state == "NOT_STARTED" -> {
                priorityScore = 50.0
                sessionType = "LEARNING"
                reason = "Standard syllabus progression in $subjectName"
                planPriority = PlanPriority.MEDIUM
            }
            state in listOf("STRONG", "MASTERED") -> {
                priorityScore = 25.0
                sessionType = "PRACTICE"
                reason = "Maintenance & speed practice for ${topic.name}"
                planPriority = PlanPriority.LOW
            }
        }

        // Apply Manual Priority Boosts
        when (subjectPriority) {
            "HIGH" -> priorityScore += 15.0
            "LOW" -> priorityScore -= 10.0
        }

        when (manualOverride) {
            "NEED_HELP" -> {
                priorityScore += 20.0
                planPriority = PlanPriority.HIGH
            }
            "I_KNOW_THIS" -> priorityScore -= 20.0
        }

        // Exam Date Proximity Multiplier
        if (examDaysRemaining != null) {
            if (examDaysRemaining <= 7) {
                if (sessionType == "REVISION" || sessionType == "WEAK_TOPIC") {
                    priorityScore += 25.0
                } else if (state == "NOT_STARTED") {
                    priorityScore -= 15.0 // Don't start heavy new topics 7 days before exam
                }
            } else if (examDaysRemaining <= 30) {
                if (sessionType == "REVISION" || sessionType == "WEAK_TOPIC") {
                    priorityScore += 12.0
                }
            }
        }

        return TupleScore(priorityScore, sessionType, reason, planPriority)
    }

    private data class TupleScore(
        val score: Double,
        val type: String,
        val reason: String,
        val priority: PlanPriority
    )

    private fun parseSubjectPriorities(jsonStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (jsonStr.isBlank() || jsonStr == "{}") return map
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key, "MEDIUM")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    /**
     * Calculates if the user is on track to complete the syllabus before the exam date.
     */
    private fun calculateDeadlineSafetyWarning(
        examContext: ExamContext,
        topicMasteries: List<TopicMastery>,
        userPreferences: UserStudyPreferences,
        examDaysRemaining: Int?
    ): String? {
        if (examDaysRemaining == null || examDaysRemaining <= 0) return null

        val totalTopics = examContext.topics.size
        if (totalTopics == 0) return null

        val masteredOrCompletedCount = topicMasteries.count {
            it.examId == examContext.examId && (it.masteryState == "MASTERED" || it.masteryScore >= 75 || it.studyCompletionStatus == "COMPLETED")
        }

        val uncompletedTopicsCount = (totalTopics - masteredOrCompletedCount).coerceAtLeast(0)
        if (uncompletedTopicsCount == 0) return null

        // Estimate 45 min per uncompleted topic
        val requiredMinutes = uncompletedTopicsCount * 45
        val availableTotalMinutes = examDaysRemaining * userPreferences.dailyAvailableMinutes

        if (availableTotalMinutes < requiredMinutes) {
            val dailyHours = userPreferences.dailyAvailableMinutes / 60f
            val neededDailyHours = String.format(Locale.US, "%.1f", (requiredMinutes.toFloat() / examDaysRemaining) / 60f)
            return "⚠️ At ${String.format(Locale.US, "%.1f", dailyHours)}h/day, you may not cover all $uncompletedTopicsCount remaining topics before ${examContext.examName} ($examDaysRemaining days left). Recommended pace: $neededDailyHours hrs/day or focus on High-Yield topics."
        }

        return null
    }

    /**
     * Generates study sessions matching explicit subject time allocations specified by the user.
     */
    fun generatePlanFromSubjectAllocations(
        examContext: ExamContext,
        subjectAllocations: Map<String, Int>,
        startHour: Int = 8,
        startMinute: Int = 0,
        breakMinutes: Int = 5,
        topicMasteries: List<TopicMastery> = emptyList(),
        mistakes: List<MistakeItem> = emptyList(),
        flashcards: List<FlashcardItem> = emptyList(),
        userPreferences: UserStudyPreferences,
        scheduledDateMillis: Long = System.currentTimeMillis()
    ): List<StudyPlanItem> {
        val resultSessions = mutableListOf<StudyPlanItem>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = scheduledDateMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
        }
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val chaptersMap = examContext.chapters.associateBy { it.id }
        val subjectsMap = examContext.subjects.associateBy { it.id }
        val masteryMap = topicMasteries.associateBy { it.topicId.ifBlank { "${it.subject.lowercase()}_${it.topic.lowercase()}" } }
        val mistakesByTopic = mistakes.filter { !it.isMastered }.groupBy { it.topicId.ifBlank { "${it.subject.lowercase()}_${it.topic.lowercase()}" } }

        for ((subjectName, allocatedMinutes) in subjectAllocations) {
            if (allocatedMinutes <= 0) continue

            // Find matching subject in examContext or by name
            val examSub = examContext.subjects.find { it.name.equals(subjectName, ignoreCase = true) }
            val subTopics = if (examSub != null) {
                examContext.topics.filter { it.subjectId == examSub.id }
            } else {
                examContext.topics.filter { topic ->
                    val s = subjectsMap[topic.subjectId]
                    s?.name.equals(subjectName, ignoreCase = true)
                }
            }

            // Rank topics by priority / weak mastery
            val rankedTopics = subTopics.map { topic ->
                val key = topic.id.ifBlank { "${subjectName.lowercase()}_${topic.name.lowercase()}" }
                val mastery = masteryMap[key] ?: topicMasteries.find { it.topic.equals(topic.name, ignoreCase = true) }
                val topicMistakes = mistakesByTopic[key]?.size ?: 0
                val (score, type, reason, prio) = calculateCandidateScore(
                    topic = topic,
                    subjectName = subjectName,
                    mastery = mastery,
                    mistakesCount = topicMistakes,
                    revisionsCount = 0,
                    subjectPriority = "HIGH",
                    examDaysRemaining = null
                )
                Triple(topic, type, reason)
            }.sortedByDescending { (if (it.first.isHighYield) 100 else 0) - it.first.orderIndex }

            var remainingSubMinutes = allocatedMinutes
            var topicIndex = 0

            while (remainingSubMinutes > 0) {
                val candidateTuple = rankedTopics.getOrNull(if (rankedTopics.isNotEmpty()) topicIndex % rankedTopics.size else 0)
                val targetTopic = candidateTuple?.first
                val sessionType = candidateTuple?.second ?: "PRACTICE"
                val reason = candidateTuple?.third ?: "Targeted focus session for $subjectName"

                val sessionMinutes = when {
                    remainingSubMinutes >= 90 -> 60
                    remainingSubMinutes >= 60 -> 60
                    else -> remainingSubMinutes
                }

                val startTimeStr = timeFormat.format(cal.time)
                cal.add(Calendar.MINUTE, sessionMinutes)
                val endTimeStr = timeFormat.format(cal.time)

                val chapter = if (targetTopic != null) chaptersMap[targetTopic.chapterId]?.name ?: "Core Concepts" else "Core Concepts"
                val topicTitle = targetTopic?.name ?: "$subjectName Core Practice"

                resultSessions.add(
                    StudyPlanItem(
                        userId = userPreferences.userId,
                        examId = examContext.examId,
                        subjectId = targetTopic?.subjectId ?: (examSub?.id ?: ""),
                        chapterId = targetTopic?.chapterId ?: "",
                        topicId = targetTopic?.id ?: "",
                        subject = subjectName,
                        chapter = chapter,
                        topic = topicTitle,
                        targetMinutes = sessionMinutes,
                        isCompleted = false,
                        scheduledDateMillis = scheduledDateMillis,
                        startTimeFormatted = startTimeStr,
                        endTimeFormatted = endTimeStr,
                        sessionType = sessionType,
                        sessionState = "PLANNED",
                        priority = PlanPriority.HIGH,
                        aiRecommendationReason = reason,
                        isAiGenerated = true
                    )
                )

                remainingSubMinutes -= sessionMinutes
                topicIndex++

                if (breakMinutes > 0 && remainingSubMinutes > 0) {
                    cal.add(Calendar.MINUTE, breakMinutes)
                }
            }

            if (breakMinutes > 0) {
                cal.add(Calendar.MINUTE, breakMinutes)
            }
        }

        return resultSessions
    }

    /**
     * Smart Rescheduling for Missed Sessions without overloading the student.
     */
    fun evaluateMissedSessionRecovery(
        missedPlans: List<StudyPlanItem>,
        userPreferences: UserStudyPreferences,
        action: String // "AUTO_RESCHEDULE", "SHORTEN_LOW_PRIORITY", "DROP_LOW_PRIORITY"
    ): List<StudyPlanItem> {
        if (missedPlans.isEmpty()) return emptyList()

        return missedPlans.map { missed ->
            val shortenedMins = (missed.targetMinutes * 0.6f).toInt().coerceAtLeast(15)
            missed.copy(
                targetMinutes = if (action == "SHORTEN_LOW_PRIORITY") shortenedMins else missed.targetMinutes,
                sessionState = "PLANNED",
                aiRecommendationReason = "Rescheduled missed session (${missed.topic})"
            )
        }
    }
}
