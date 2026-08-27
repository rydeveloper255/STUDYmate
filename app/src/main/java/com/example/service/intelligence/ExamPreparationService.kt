package com.example.service.intelligence

import com.example.data.local.ExamPrepDao
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/**
 * Step 57: Central Exam Preparation & Adaptive Study Planner Service 2.0.
 * Operates on verified database models as the absolute single source of truth.
 */
class ExamPreparationService(
    private val database: StudyMateDatabase
) {
    private val prepDao = database.examPrepDao()
    private val scheduleDao = database.studyScheduleDao()
    private val preferencesDao = database.userStudyPreferencesDao()

    private val _updateTrigger = MutableStateFlow(System.currentTimeMillis())
    val updateTrigger: Flow<Long> = _updateTrigger.asStateFlow()

    fun invalidateCache() {
        _updateTrigger.value = System.currentTimeMillis()
    }

    // --- 1. EXAM GOAL MANAGEMENT ---

    suspend fun createOrUpdateExamGoal(
        examName: String,
        organization: String = "",
        examDateMillis: Long? = null,
        isExamDateKnown: Boolean = true,
        target: String = "Top Merit / High Score",
        priority: String = "PRIMARY",
        subjectsList: List<String> = listOf("Quantitative Aptitude", "Reasoning", "English", "General Awareness"),
        examIdOverride: String = ""
    ): ExamGoalEntity = withContext(Dispatchers.IO) {
        val examId = if (examIdOverride.isNotBlank()) examIdOverride else "exam_${UUID.randomUUID().toString().take(8)}"
        val subjectsJson = org.json.JSONArray(subjectsList).toString()

        val existing = prepDao.getExamGoalById(examId)
        val now = System.currentTimeMillis()

        val goal = ExamGoalEntity(
            examId = examId,
            userId = "current_user",
            examName = examName,
            organization = organization,
            examDateMillis = if (isExamDateKnown) examDateMillis else null,
            isExamDateKnown = isExamDateKnown,
            target = target,
            priority = priority,
            status = if (existing != null) existing.status else "ACTIVE",
            subjectsJson = subjectsJson,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        prepDao.insertExamGoal(goal)

        // Seed standard syllabus if no topics exist for this exam
        val currentTopics = prepDao.getSyllabusTopicsForExamOnce(examId)
        if (currentTopics.isEmpty()) {
            seedDefaultSyllabusForExam(examId, subjectsList)
        }

        invalidateCache()
        goal
    }

    suspend fun getAllExamGoals(): List<ExamGoalEntity> = withContext(Dispatchers.IO) {
        prepDao.getAllExamGoalsOnce()
    }

    suspend fun getPrimaryExamGoal(): ExamGoalEntity? = withContext(Dispatchers.IO) {
        prepDao.getPrimaryExamGoalOnce() ?: prepDao.getAllExamGoalsOnce().firstOrNull()
    }

    suspend fun updateExamPriority(examId: String, newPriority: String) = withContext(Dispatchers.IO) {
        val goal = prepDao.getExamGoalById(examId) ?: return@withContext
        prepDao.updateExamGoal(goal.copy(priority = newPriority, updatedAt = System.currentTimeMillis()))
        invalidateCache()
    }

    suspend fun archiveOrDeleteExam(examId: String) = withContext(Dispatchers.IO) {
        prepDao.deleteSyllabusTopicsForExam(examId)
        prepDao.deleteExamGoal(examId)
        invalidateCache()
    }

    // --- 2. SYLLABUS & TOPIC MANAGEMENT ---

    private suspend fun seedDefaultSyllabusForExam(examId: String, subjects: List<String>) {
        val topicsToInsert = mutableListOf<SyllabusTopicEntity>()

        val defaultTopicMap = mapOf(
            "Quantitative Aptitude" to listOf("Number System", "Percentage", "Profit & Loss", "Ratio & Proportion", "Simple & Compound Interest", "Time & Work", "Speed Time Distance", "Algebra", "Geometry & Mensuration"),
            "Mathematics" to listOf("Number System", "Percentage", "Profit & Loss", "Ratio & Proportion", "Algebra", "Geometry", "Trigonometry", "Statistics"),
            "Reasoning" to listOf("Analogy & Classification", "Coding Decoding", "Blood Relations", "Syllogism", "Series & Sequences", "Seating Arrangement", "Puzzles", "Venn Diagrams"),
            "English" to listOf("Grammar & Error Spotting", "Reading Comprehension", "Vocabulary & Synonyms", "Cloze Test", "Sentence Improvement", "Para Jumbles"),
            "General Awareness" to listOf("Indian History", "Indian Polity & Constitution", "Geography", "Indian Economy", "General Science", "Current Affairs")
        )

        subjects.forEach { subjectName ->
            val topics = defaultTopicMap[subjectName] ?: listOf("Fundamental Concepts", "Core Practice Topics", "Advanced Practice", "Previous Year Questions")
            topics.forEachIndexed { index, topicName ->
                topicsToInsert.add(
                    SyllabusTopicEntity(
                        topicId = "${examId}_${subjectName.lowercase().replace(" ", "_")}_${topicName.lowercase().replace(" ", "_")}",
                        examId = examId,
                        subjectName = subjectName,
                        topicName = topicName,
                        orderIndex = index,
                        status = "NOT_STARTED",
                        isHighYield = index < 3
                    )
                )
            }
        }

        prepDao.insertSyllabusTopics(topicsToInsert)
    }

    suspend fun addCustomTopic(
        examId: String,
        subjectName: String,
        topicName: String,
        subtopicName: String = ""
    ): SyllabusTopicEntity = withContext(Dispatchers.IO) {
        val existingTopics = prepDao.getSyllabusTopicsForSubjectOnce(examId, subjectName)
        val topic = SyllabusTopicEntity(
            topicId = "custom_${examId}_${UUID.randomUUID().toString().take(8)}",
            examId = examId,
            subjectName = subjectName,
            topicName = topicName,
            subtopicName = subtopicName,
            orderIndex = existingTopics.size,
            status = "NOT_STARTED",
            isCustom = true
        )
        prepDao.insertSyllabusTopic(topic)
        invalidateCache()
        topic
    }

    suspend fun updateTopicStatus(topicId: String, newStatus: String) = withContext(Dispatchers.IO) {
        val topic = prepDao.getTopicById(topicId) ?: return@withContext
        val now = System.currentTimeMillis()
        var revStatus = topic.revisionStatus
        var nextRevDue = topic.nextRevisionDueMillis

        // If completed, put into REVISION_PENDING with 3-day spaced review rule
        if (newStatus == "COMPLETED" && topic.status != "COMPLETED") {
            revStatus = "REVISION_PENDING"
            nextRevDue = now + (3L * 24 * 60 * 60 * 1000)
        }

        prepDao.updateSyllabusTopic(
            topic.copy(
                status = newStatus,
                revisionStatus = revStatus,
                nextRevisionDueMillis = nextRevDue,
                lastStudiedAt = if (newStatus == "COMPLETED" || newStatus == "IN_PROGRESS") now else topic.lastStudiedAt
            )
        )
        invalidateCache()
    }

    suspend fun deleteTopic(topicId: String) = withContext(Dispatchers.IO) {
        prepDao.deleteSyllabusTopic(topicId)
        invalidateCache()
    }

    // --- 3. EXAM PREPARATION SUMMARY & PROGRESS FORMULA ---

    suspend fun getExamPreparationSummary(
        examId: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ExamPreparationSummary? = withContext(Dispatchers.IO) {
        val goal = prepDao.getExamGoalById(examId) ?: return@withContext null
        val topics = prepDao.getSyllabusTopicsForExamOnce(examId)

        val daysRem = goal.getDaysRemaining(nowMillis, zoneId)
        val isDatePassed = daysRem != null && daysRem < 0

        val totalTopics = topics.size
        val completedTopics = topics.count { it.status == "COMPLETED" }
        val inProgressTopics = topics.count { it.status == "IN_PROGRESS" }
        val pendingRevisions = topics.count { it.revisionStatus == "REVISION_PENDING" || it.status == "REVIEW_REQUIRED" }
        val totalMins = topics.sumOf { it.studyTimeMinutes }

        val syllabusCoveragePct = if (totalTopics > 0) ((completedTopics.toFloat() / totalTopics) * 100).toInt() else 0

        val subjectGroups = topics.groupBy { it.subjectName }
        val subjectProgressList = subjectGroups.map { (subjectName, subjectTopics) ->
            SubjectSyllabusProgress(
                subjectName = subjectName,
                totalTopicsCount = subjectTopics.size,
                completedTopicsCount = subjectTopics.count { it.status == "COMPLETED" },
                inProgressTopicsCount = subjectTopics.count { it.status == "IN_PROGRESS" },
                notStartedTopicsCount = subjectTopics.count { it.status == "NOT_STARTED" },
                reviewRequiredTopicsCount = subjectTopics.count { it.status == "REVIEW_REQUIRED" },
                totalStudyMinutes = subjectTopics.sumOf { it.studyTimeMinutes }
            )
        }.sortedByDescending { it.coveragePercentage }

        ExamPreparationSummary(
            examGoal = goal,
            daysRemaining = daysRem,
            isDatePassed = isDatePassed,
            totalTopicsCount = totalTopics,
            completedTopicsCount = completedTopics,
            inProgressTopicsCount = inProgressTopics,
            syllabusCoveragePercentage = syllabusCoveragePct,
            subjectProgressList = subjectProgressList,
            pendingRevisionTopicsCount = pendingRevisions,
            totalStudyMinutes = totalMins
        )
    }

    // --- 4. SMART DAILY PLANNER WITH BREAKS & CONFLICT PROTECTION ---

    suspend fun generateDailyStudyPlanPreview(
        examId: String,
        dailyAvailableMinutes: Int? = null,
        targetDateMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyStudyPlanPreview = withContext(Dispatchers.IO) {
        val goal = prepDao.getExamGoalById(examId) ?: ExamGoalEntity(examId = examId, examName = "Target Exam")
        val topics = prepDao.getSyllabusTopicsForExamOnce(examId)
        val prefs = preferencesDao.getUserPreferencesSync("current_user")

        val availableMins = dailyAvailableMinutes ?: prefs?.dailyAvailableMinutes ?: 120
        val preferredSessionMins = prefs?.preferredSessionMinutes?.coerceIn(20, 60) ?: 45
        val breakMins = 15

        val dateFormatted = Instant.ofEpochMilli(targetDateMillis).atZone(zoneId).toLocalDate().toString()

        // Prioritize: 1. Pending Revision Topics, 2. In Progress Topics, 3. Untouched High Yield Topics
        val revisionTopics = topics.filter { it.revisionStatus == "REVISION_PENDING" || it.status == "REVIEW_REQUIRED" }
        val inProgressTopics = topics.filter { it.status == "IN_PROGRESS" }
        val untouchedTopics = topics.filter { it.status == "NOT_STARTED" }.sortedByDescending { it.isHighYield }

        val proposedItems = mutableListOf<ProposedPlanItem>()
        var remainingBudget = availableMins
        var currentHour = prefs?.windowStartHour ?: 18 // Default evening study block starting 6:00 PM
        var currentMinute = 0

        fun formatTimeSlot(startH: Int, startM: Int, durationMins: Int): Pair<String, Pair<Int, Int>> {
            val totalStart = startH * 60 + startM
            val totalEnd = totalStart + durationMins

            val sH = (totalStart / 60) % 24
            val sM = totalStart % 60
            val eH = (totalEnd / 60) % 24
            val eM = totalEnd % 60

            val amPmStart = if (sH >= 12) "PM" else "AM"
            val displaySH = if (sH % 12 == 0) 12 else sH % 12
            val startFormatted = String.format(Locale.ROOT, "%d:%02d %s", displaySH, sM, amPmStart)

            val amPmEnd = if (eH >= 12) "PM" else "AM"
            val displayEH = if (eH % 12 == 0) 12 else eH % 12
            val endFormatted = String.format(Locale.ROOT, "%d:%02d %s", displayEH, eM, amPmEnd)

            return Pair("$startFormatted - $endFormatted", Pair(eH, eM))
        }

        // Add 1 Revision topic session if available
        if (revisionTopics.isNotEmpty() && remainingBudget >= 30) {
            val revTopic = revisionTopics.first()
            val sessionDur = minOf(30, remainingBudget)
            val (timeSlot, endPair) = formatTimeSlot(currentHour, currentMinute, sessionDur)
            proposedItems.add(
                ProposedPlanItem(
                    examId = examId,
                    subjectName = revTopic.subjectName,
                    topicName = revTopic.topicName,
                    sessionType = "REVISION",
                    targetMinutes = sessionDur,
                    startTimeFormatted = timeSlot.split(" - ").firstOrNull() ?: "",
                    endTimeFormatted = timeSlot.split(" - ").lastOrNull() ?: "",
                    priority = "HIGH",
                    rationale = "Spaced revision due for ${revTopic.topicName}"
                )
            )
            remainingBudget -= sessionDur
            currentHour = endPair.first
            currentMinute = endPair.second

            // Add break if budget remains
            if (remainingBudget >= 30) {
                val (_, breakEnd) = formatTimeSlot(currentHour, currentMinute, breakMins)
                currentHour = breakEnd.first
                currentMinute = breakEnd.second
            }
        }

        // Add In-Progress topics or Untouched topics
        val candidateQueue = (inProgressTopics + untouchedTopics).toMutableList()

        while (candidateQueue.isNotEmpty() && remainingBudget >= 20) {
            val topic = candidateQueue.removeAt(0)
            val sessionDur = minOf(preferredSessionMins, remainingBudget)

            val (timeSlot, endPair) = formatTimeSlot(currentHour, currentMinute, sessionDur)
            proposedItems.add(
                ProposedPlanItem(
                    examId = examId,
                    subjectName = topic.subjectName,
                    topicName = topic.topicName,
                    sessionType = if (topic.status == "IN_PROGRESS") "PRACTICE" else "LEARNING",
                    targetMinutes = sessionDur,
                    startTimeFormatted = timeSlot.split(" - ").firstOrNull() ?: "",
                    endTimeFormatted = timeSlot.split(" - ").lastOrNull() ?: "",
                    priority = if (topic.isHighYield) "HIGH" else "MEDIUM",
                    rationale = if (topic.isHighYield) "High-yield topic for ${goal.examName}" else "Core syllabus coverage"
                )
            )

            remainingBudget -= sessionDur
            currentHour = endPair.first
            currentMinute = endPair.second

            // Add break between study sessions if budget allows
            if (candidateQueue.isNotEmpty() && remainingBudget >= 30) {
                val (_, breakEnd) = formatTimeSlot(currentHour, currentMinute, breakMins)
                currentHour = breakEnd.first
                currentMinute = breakEnd.second
            }
        }

        // Conflict check with existing StudyScheduleItems
        val existingSchedules = scheduleDao.getAllScheduleItems()
        val conflicts = mutableListOf<PlanScheduleConflict>()

        proposedItems.forEach { proposed ->
            val matchingSchedule = existingSchedules.find {
                it.subject.equals(proposed.subjectName, ignoreCase = true) ||
                        it.startTime.equals(proposed.startTimeFormatted, ignoreCase = true)
            }
            if (matchingSchedule != null) {
                conflicts.add(
                    PlanScheduleConflict(
                        proposedItem = proposed,
                        existingScheduleSubject = matchingSchedule.subject,
                        existingScheduleTime = "${matchingSchedule.startTime} - ${matchingSchedule.endTime}",
                        message = "Your ${matchingSchedule.startTime} ${matchingSchedule.subject} session already exists. Proposed plan slot overlaps."
                    )
                )
            }
        }

        DailyStudyPlanPreview(
            examId = examId,
            examName = goal.examName,
            targetDateFormatted = dateFormatted,
            availableHoursPerDay = availableMins / 60f,
            plannedItems = proposedItems,
            scheduleConflicts = conflicts,
            requiresUserConfirmation = true
        )
    }

    /**
     * Confirms and creates actual schedule items safely.
     */
    suspend fun confirmAndScheduleDailyPlan(preview: DailyStudyPlanPreview): Boolean = withContext(Dispatchers.IO) {
        preview.plannedItems.forEach { proposed ->
            val scheduleItem = StudyScheduleItem(
                subject = proposed.subjectName,
                topic = proposed.topicName,
                durationMinutes = proposed.targetMinutes,
                dayOfWeek = "MON",
                startTime = proposed.startTimeFormatted,
                endTime = proposed.endTimeFormatted,
                repeatType = "ONCE"
            )
            scheduleDao.insertOrUpdateScheduleItem(scheduleItem)
        }
        invalidateCache()
        true
    }

    // --- 5. FOCUS COMPLETION PROGRESS SYNC ---

    suspend fun recordFocusSessionStudyProgress(
        subject: String,
        topic: String,
        actualMinutesSpent: Int,
        examIdFilter: String = ""
    ) = withContext(Dispatchers.IO) {
        if (subject.isBlank() || topic.isBlank() || actualMinutesSpent <= 0) return@withContext

        // Search for matching SyllabusTopicEntity across active exams
        val activeGoals = getAllExamGoals().filter { it.status == "ACTIVE" }
        val targetExams = if (examIdFilter.isNotBlank()) listOf(examIdFilter) else activeGoals.map { it.examId }

        for (examId in targetExams) {
            val topics = prepDao.getSyllabusTopicsForExamOnce(examId)
            val matchedTopic = topics.find {
                it.subjectName.equals(subject, ignoreCase = true) && it.topicName.equals(topic, ignoreCase = true)
            }
            if (matchedTopic != null) {
                val newMins = matchedTopic.studyTimeMinutes + actualMinutesSpent
                val newSessions = matchedTopic.sessionsCount + 1
                val now = System.currentTimeMillis()
                val updatedStatus = if (matchedTopic.status == "NOT_STARTED") "IN_PROGRESS" else matchedTopic.status

                prepDao.updateSyllabusTopic(
                    matchedTopic.copy(
                        studyTimeMinutes = newMins,
                        sessionsCount = newSessions,
                        lastStudiedAt = now,
                        status = updatedStatus
                    )
                )
            }
        }
        invalidateCache()
    }

    // --- 6. NOVA VERIFIED EXAM PREPARATION ENGINE ---

    data class NovaPrepResponse(
        val answerText: String,
        val actions: List<NovaContextualAction> = emptyList()
    )

    suspend fun getNovaExamPrepAnswer(userQuery: String): NovaPrepResponse = withContext(Dispatchers.IO) {
        val lower = userQuery.lowercase()
        val primaryGoal = getPrimaryExamGoal()

        if (primaryGoal == null) {
            return@withContext NovaPrepResponse(
                answerText = "🎯 Currently koi Active Exam Goal set nahi hai.\n\nSyllabus, countdown aur study planner connect karne ke liye aap 'SSC CGL' ya koi bhi exam target select kar sakte hain.",
                actions = listOf(
                    NovaContextualAction(
                        label = "🎯 Create Exam Goal",
                        iconName = "flag",
                        actionType = NovaActionType.OPEN_EXAM_PREPARATION,
                        isPrimary = true
                    )
                )
            )
        }

        val summary = getExamPreparationSummary(primaryGoal.examId)

        // 1. Exam Date & Countdown Queries
        if (lower.contains("exam kab hai") || lower.contains("din bache") || lower.contains("exam date") || lower.contains("countdown")) {
            val dateText = if (primaryGoal.isExamDateKnown && primaryGoal.examDateMillis != null) {
                val formattedDate = java.time.Instant.ofEpochMilli(primaryGoal.examDateMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                val days = summary?.daysRemaining
                if (days != null && days >= 0) {
                    "🗓️ **${primaryGoal.examName}** exam **$formattedDate** ko hai.\n⏳ **$days days remaining** for your preparation."
                } else if (days != null && days < 0) {
                    "🗓️ **${primaryGoal.examName}** target date ($formattedDate) pass ho chuki hai."
                } else {
                    "🗓️ Target Date: $formattedDate"
                }
            } else {
                "🗓️ **${primaryGoal.examName}**: Exam date not available right now."
            }

            return@withContext NovaPrepResponse(
                answerText = "$dateText\n\n🎯 Target: ${primaryGoal.target}\n📊 Syllabus Completed: ${summary?.syllabusCoveragePercentage ?: 0}%",
                actions = listOf(
                    NovaContextualAction(
                        label = "📋 View Full Preparation",
                        iconName = "book",
                        actionType = NovaActionType.OPEN_EXAM_PREPARATION,
                        isPrimary = true
                    )
                )
            )
        }

        // 2. Syllabus Coverage & Subject Queries
        if (lower.contains("syllabus") || lower.contains("kitna hua") || lower.contains("kitna padha") || lower.contains("coverage")) {
            val sb = StringBuilder()
            sb.append("📊 **${primaryGoal.examName} — Syllabus Progress**\n\n")
            sb.append("• Overall Topics Covered: **${summary?.syllabusCoveragePercentage ?: 0}%** (${summary?.completedTopicsCount ?: 0}/${summary?.totalTopicsCount ?: 0} topics)\n\n")

            summary?.subjectProgressList?.forEach { subject ->
                sb.append("• **${subject.subjectName}**: ${subject.coveragePercentage}% covered (${subject.completedTopicsCount}/${subject.totalTopicsCount} topics)\n")
            }

            return@withContext NovaPrepResponse(
                answerText = sb.toString(),
                actions = listOf(
                    NovaContextualAction(
                        label = "📚 Open Syllabus Breakdown",
                        iconName = "list",
                        actionType = NovaActionType.OPEN_EXAM_PREPARATION,
                        isPrimary = true
                    )
                )
            )
        }

        // 3. Today's / Tomorrow's Plan & Schedule
        if (lower.contains("aaj kya") || lower.contains("aaj ka plan") || lower.contains("kal ka plan") || lower.contains("plan bata")) {
            val preview = generateDailyStudyPlanPreview(primaryGoal.examId)
            val sb = StringBuilder()
            sb.append("📅 **Today's Recommended Study Plan (${primaryGoal.examName})**\n\n")

            if (preview.plannedItems.isEmpty()) {
                sb.append("Aapka daily schedule already complete hai ya budget available nahi hai.")
            } else {
                preview.plannedItems.forEachIndexed { idx, item ->
                    sb.append("${idx + 1}. **${item.startTimeFormatted} - ${item.endTimeFormatted}**: ${item.subjectName} — *${item.topicName}* (${item.targetMinutes} min ${item.sessionType})\n")
                }
            }

            if (preview.scheduleConflicts.isNotEmpty()) {
                sb.append("\n⚠️ **Schedule Conflict Detected**: ${preview.scheduleConflicts.first().message}")
            }

            return@withContext NovaPrepResponse(
                answerText = sb.toString(),
                actions = listOf(
                    NovaContextualAction(
                        label = "✅ Confirm & Save Plan",
                        iconName = "check",
                        actionType = NovaActionType.CONFIRM_STUDY_PLAN,
                        payload = "{\"examId\":\"${primaryGoal.examId}\"}",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "✏️ Customize Plan",
                        iconName = "edit",
                        actionType = NovaActionType.OPEN_EXAM_PREPARATION
                    )
                )
            )
        }

        // 4. Revision & Pending Topics Query
        if (lower.contains("revision") || lower.contains("pending") || lower.contains("baaki")) {
            val pending = prepDao.getPendingRevisionTopicsOnce(primaryGoal.examId)
            if (pending.isEmpty()) {
                return@withContext NovaPrepResponse(
                    answerText = "✨ **Revision Queue Clean!** Sabhi completed topics revise ho chuke hain aur koi immediate revision pending nahi hai.",
                    actions = listOf(
                        NovaContextualAction(
                            label = "📚 View All Topics",
                            iconName = "book",
                            actionType = NovaActionType.OPEN_EXAM_PREPARATION
                        )
                    )
                )
            } else {
                val sb = StringBuilder()
                sb.append("🔄 **Pending Revision & Review Topics (${pending.size})**\n\n")
                pending.take(5).forEach { topic ->
                    sb.append("• **${topic.subjectName}**: ${topic.topicName} (${if (topic.isHighYield) "🔥 High Yield" else "Standard"})\n")
                }
                return@withContext NovaPrepResponse(
                    answerText = sb.toString(),
                    actions = listOf(
                        NovaContextualAction(
                            label = "🎯 Start Revision Focus",
                            iconName = "timer",
                            actionType = NovaActionType.START_FOCUS,
                            payload = "{\"subject\":\"${pending.first().subjectName}\",\"topic\":\"${pending.first().topicName}\",\"minutes\":30}",
                            isPrimary = true
                        )
                    )
                )
            }
        }

        // 5. Topic-Aware Focus Start ("Percentage start kar")
        val topics = prepDao.getSyllabusTopicsForExamOnce(primaryGoal.examId)
        val matchedTopic = topics.find { lower.contains(it.topicName.lowercase()) }
        if (matchedTopic != null && (lower.contains("start") || lower.contains("chalu") || lower.contains("padhna") || lower.contains("focus"))) {
            return@withContext NovaPrepResponse(
                answerText = "🎯 **Focus Ready!** Starting focused session for **${matchedTopic.subjectName} — ${matchedTopic.topicName}** (45 mins).",
                actions = listOf(
                    NovaContextualAction(
                        label = "🚀 Launch Focus Timer",
                        iconName = "play",
                        actionType = NovaActionType.START_FOCUS,
                        payload = "{\"subject\":\"${matchedTopic.subjectName}\",\"topic\":\"${matchedTopic.topicName}\",\"minutes\":45}",
                        isPrimary = true
                    )
                )
            )
        }

        // Default Overview
        NovaPrepResponse(
            answerText = "🎯 **${primaryGoal.examName} Preparation Overview**\n\n• Days Remaining: ${summary?.daysRemaining ?: "N/A"}\n• Syllabus Covered: ${summary?.syllabusCoveragePercentage ?: 0}%\n• Total Study Time: ${summary?.totalStudyMinutes ?: 0} mins",
            actions = listOf(
                NovaContextualAction(
                    label = "📋 Exam Preparation Dashboard",
                    iconName = "dashboard",
                    actionType = NovaActionType.OPEN_EXAM_PREPARATION,
                    isPrimary = true
                )
            )
        )
    }
}
