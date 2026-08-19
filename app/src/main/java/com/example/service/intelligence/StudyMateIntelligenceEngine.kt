package com.example.service.intelligence

import android.content.Context
import com.example.data.model.*
import com.example.service.NovaUsageStatsHelper
import java.util.Calendar

/**
 * MASTER INTELLIGENCE ENGINE for StudyMate AI & NOVA.
 * Centralized context layer that maintains real-time student state, computes the
 * Next Best Action (NBA), schedules adaptive revisions, auto-scales question difficulty,
 * and powers NOVA's proactive coaching.
 */
object StudyMateIntelligenceEngine {

    /**
     * Aggregates the unified Student Master Context across all modules.
     */
    fun buildMasterContext(
        profile: UserProfile,
        plans: List<StudyPlanItem>,
        focusSessions: List<FocusSession>,
        mockAttempts: List<MockTestAttempt>,
        mistakes: List<MistakeItem>,
        flashcards: List<FlashcardItem>,
        availableTimeMinutes: Int? = null
    ): StudentMasterContext {
        val now = System.currentTimeMillis()
        val pendingPlansCount = plans.count { !it.isCompleted }
        val completedPlansCount = plans.count { it.isCompleted }
        val dueFlashcardsCount = flashcards.count { it.nextReviewDate <= now || it.status == RevisionCategory.REVISE_NOW }
        val unmasteredMistakesCount = mistakes.count { !it.isMastered }

        val avgAccuracy = if (mockAttempts.isNotEmpty()) {
            mockAttempts.map { it.accuracyPercent }.average().toFloat()
        } else {
            75f
        }

        val totalFocusMins = focusSessions.sumOf { it.actualMinutesSpent }
        val examDaysRemaining = ((profile.examDateMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()

        val nextBestAction = calculateNextBestAction(
            profile = profile,
            plans = plans,
            mockAttempts = mockAttempts,
            mistakes = mistakes,
            flashcards = flashcards,
            timeAvailableMinutes = availableTimeMinutes
        )

        val goalRadar = calculateGoalRadar(
            profile = profile,
            plans = plans,
            focusSessions = focusSessions,
            mockAttempts = mockAttempts
        )

        return StudentMasterContext(
            userProfile = profile,
            pendingPlansCount = pendingPlansCount,
            completedPlansCount = completedPlansCount,
            dueFlashcardsCount = dueFlashcardsCount,
            unmasteredMistakesCount = unmasteredMistakesCount,
            avgTestAccuracy = avgAccuracy,
            totalFocusMinutes = totalFocusMins,
            streakDays = profile.streakDays,
            examDaysRemaining = examDaysRemaining,
            nextBestAction = nextBestAction,
            goalRadar = goalRadar
        )
    }

    /**
     * NEXT BEST ACTION (NBA) ENGINE
     * Dynamically calculates the single highest-leverage study task right now.
     */
    fun calculateNextBestAction(
        profile: UserProfile,
        plans: List<StudyPlanItem>,
        mockAttempts: List<MockTestAttempt>,
        mistakes: List<MistakeItem>,
        flashcards: List<FlashcardItem>,
        timeAvailableMinutes: Int? = null
    ): NextBestAction {
        val now = System.currentTimeMillis()
        val daysLeft = ((profile.examDateMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()
        val dueCards = flashcards.filter { it.nextReviewDate <= now || it.status == RevisionCategory.REVISE_NOW }
        val unmasteredMistakes = mistakes.filter { !it.isMastered }
        val pendingPlans = plans.filter { !it.isCompleted }

        // User explicit time duration preference
        val targetDuration = timeAvailableMinutes ?: 25

        // Decision Tree Logic based on pedagogy & Spaced Repetition priority:
        // 1. Spaced Repetition Due (Short time window or high backlog)
        if (dueCards.size >= 4 && targetDuration <= 20) {
            val dueSubject = dueCards.firstOrNull()?.subject ?: profile.subjects.firstOrNull() ?: "Physics"
            return NextBestAction(
                title = "Spaced Recall: $dueSubject Flashcards",
                subject = dueSubject,
                topic = "${dueCards.size} cards due for active recall",
                durationMinutes = targetDuration.coerceAtMost(15),
                priority = PlanPriority.HIGH,
                actionType = NextBestActionType.SPACED_REVISION,
                reason = "${dueCards.size} cards reached their forgetting curve threshold.",
                whyThisHelpful = "Active recall strengthens neural memory consolidation right before decay happens.",
                questionsCount = dueCards.size,
                urgencyTag = "RETENTION CRITICAL",
                isAvailableTimeAdjusted = timeAvailableMinutes != null
            )
        }

        // 2. Unmastered Quiz Mistakes Remediation
        if (unmasteredMistakes.size >= 3) {
            val topMistakeSubject = unmasteredMistakes.groupBy { it.subject }.maxByOrNull { it.value.size }?.key ?: profile.subjects.firstOrNull() ?: "Physics"
            val topMistakeTopic = unmasteredMistakes.firstOrNull { it.subject == topMistakeSubject }?.topic ?: "Past Errors"
            return NextBestAction(
                title = "Mistake Remediation: $topMistakeSubject",
                subject = topMistakeSubject,
                topic = topMistakeTopic,
                durationMinutes = targetDuration.coerceIn(15, 30),
                priority = PlanPriority.HIGH,
                actionType = NextBestActionType.MISTAKE_REMEDIATION,
                reason = "You have ${unmasteredMistakes.size} unmastered mistakes in recent quizzes.",
                whyThisHelpful = "Fixing known weak spots gives 3x higher score boost than re-reading mastered topics.",
                questionsCount = unmasteredMistakes.size,
                urgencyTag = "HIGH SCORE LEVERAGE",
                isAvailableTimeAdjusted = timeAvailableMinutes != null
            )
        }

        // 3. Urgent Exam Proximity (< 30 days) -> High-Yield Practice / Mock Test
        if (daysLeft in 1..30 && targetDuration >= 30) {
            val weakSub = mockAttempts.minByOrNull { it.accuracyPercent }?.subject
                ?: profile.weakSubjects.firstOrNull()
                ?: profile.subjects.firstOrNull() ?: "Physics"
            val weakTopic = profile.weakTopics.firstOrNull() ?: "High Yield Core"

            return NextBestAction(
                title = "High-Yield Sprint: $weakSub",
                subject = weakSub,
                topic = weakTopic,
                durationMinutes = targetDuration,
                priority = PlanPriority.HIGH,
                actionType = NextBestActionType.MOCK_TEST,
                reason = "Exam is in $daysLeft days. Timed practice on $weakSub maximizes speed and accuracy.",
                whyThisHelpful = "Simulating real exam pressure prevents negative marking on tricky questions.",
                questionsCount = 10,
                urgencyTag = "EXAM READINESS",
                isAvailableTimeAdjusted = timeAvailableMinutes != null
            )
        }

        // 4. Pending Scheduled Plan Item
        val highPriorityPlan = pendingPlans.firstOrNull { it.priority == PlanPriority.HIGH }
            ?: pendingPlans.firstOrNull()

        if (highPriorityPlan != null) {
            val planDuration = if (timeAvailableMinutes != null) timeAvailableMinutes else highPriorityPlan.targetMinutes.coerceIn(20, 45)
            return NextBestAction(
                title = "${highPriorityPlan.subject}: ${highPriorityPlan.topic}",
                subject = highPriorityPlan.subject,
                topic = highPriorityPlan.topic,
                durationMinutes = planDuration,
                priority = highPriorityPlan.priority,
                actionType = NextBestActionType.FOCUS_SESSION,
                reason = "Scheduled on your study plan for today's syllabus progression.",
                whyThisHelpful = "Consistent syllabus coverage keeps your preparation on track without last-minute cramming.",
                urgencyTag = if (highPriorityPlan.priority == PlanPriority.HIGH) "TOP GOAL TODAY" else "SCHEDULED TASK",
                isAvailableTimeAdjusted = timeAvailableMinutes != null
            )
        }

        // 5. Default High-Leverage Focus Topic
        val fallbackSubject = profile.weakSubjects.firstOrNull() ?: profile.subjects.firstOrNull() ?: "Physics"
        val fallbackTopic = profile.weakTopics.firstOrNull() ?: "Concept Strengthening"

        return NextBestAction(
            title = "Deep Focus: $fallbackSubject",
            subject = fallbackSubject,
            topic = fallbackTopic,
            durationMinutes = targetDuration,
            priority = PlanPriority.MEDIUM,
            actionType = NextBestActionType.FOCUS_SESSION,
            reason = "No pending tasks due right now. Deep focus on your weakest area boosts mastery.",
            whyThisHelpful = "Targeting challenging concepts during free windows builds long-term retention.",
            urgencyTag = "MASTERY BUILDER",
            isAvailableTimeAdjusted = timeAvailableMinutes != null
        )
    }

    /**
     * GOAL RADAR & SYLLABUS PACE CALCULATOR
     */
    fun calculateGoalRadar(
        profile: UserProfile,
        plans: List<StudyPlanItem>,
        focusSessions: List<FocusSession>,
        mockAttempts: List<MockTestAttempt>
    ): GoalRadarStatus {
        val now = System.currentTimeMillis()
        val daysLeft = ((profile.examDateMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(1).toInt()

        val totalPlans = plans.size.coerceAtLeast(1)
        val completedPlans = plans.count { it.isCompleted }
        val syllabusCoveredPercent = ((completedPlans.toFloat() / totalPlans) * 100).toInt().coerceIn(0, 100)

        // Weekly Focus Hours
        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
        val weeklyMinutes = focusSessions.filter { it.timestamp >= oneWeekAgo }.sumOf { it.actualMinutesSpent }
        val weeklyHours = weeklyMinutes / 60f
        val targetWeeklyHours = (profile.dailyTargetMinutes * 7) / 60f

        val paceRatio = if (targetWeeklyHours > 0) weeklyHours / targetWeeklyHours else 1f
        val paceStatus = when {
            paceRatio >= 0.9f -> "On Track 🚀"
            paceRatio >= 0.6f -> "Slightly Behind ⚠️"
            else -> "Needs Focus 🎯"
        }

        val weakSubject = mockAttempts.minByOrNull { it.accuracyPercent }?.subject
            ?: profile.weakSubjects.firstOrNull() ?: profile.subjects.firstOrNull() ?: "Physics"
        val weakTopic = profile.weakTopics.firstOrNull() ?: "Fundamentals"

        val calmAdvice = when {
            paceRatio >= 0.9f -> "You are pacing ahead of your target! Maintain this rhythm and do active recalls."
            paceRatio >= 0.6f -> "A solid foundation is in place. Add one 25-min session today on $weakSubject to recover pace."
            else -> "Don't stress over backlogs. Focus on one high-yield topic at a time starting with $weakSubject ($weakTopic)."
        }

        return GoalRadarStatus(
            examName = profile.examName.ifBlank { "Competitive Exam" },
            daysRemaining = daysLeft,
            syllabusCoveredPercent = syllabusCoveredPercent,
            studyPaceStatus = paceStatus,
            weeklyHoursCompleted = (weeklyHours * 10).toInt() / 10f,
            weeklyHoursTarget = (targetWeeklyHours * 10).toInt() / 10f,
            calmAdvice = calmAdvice,
            prioritySubject = weakSubject,
            weakTopicNeedCare = weakTopic
        )
    }

    /**
     * ADAPTIVE QUESTION DIFFICULTY SCALER
     */
    fun getRecommendedDifficulty(
        subject: String,
        topic: String,
        mockAttempts: List<MockTestAttempt>,
        mistakes: List<MistakeItem>
    ): String {
        val subjectAttempts = mockAttempts.filter { it.subject.equals(subject, ignoreCase = true) }
        val topicMistakes = mistakes.filter { it.subject.equals(subject, ignoreCase = true) && !it.isMastered }

        if (subjectAttempts.isEmpty()) return "Medium"

        val avgAccuracy = subjectAttempts.map { it.accuracyPercent }.average()
        return when {
            avgAccuracy >= 85.0 && topicMistakes.isEmpty() -> "Hard"
            avgAccuracy >= 65.0 -> "Medium"
            else -> "Easy"
        }
    }

    /**
     * EVALUATES ADAPTIVE REVISION RADAR FOR A SPECIFIC TOPIC
     */
    fun evaluateAdaptiveRevisionTopic(
        subject: String,
        topic: String,
        lastStudiedMillis: Long?,
        quizAccuracy: Float?,
        mistakesCount: Int
    ): Pair<RevisionCategory, String> {
        val now = System.currentTimeMillis()
        val daysSinceStudied = if (lastStudiedMillis != null) {
            ((now - lastStudiedMillis) / (1000L * 60 * 60 * 24)).toInt()
        } else {
            10
        }

        val accuracy = quizAccuracy ?: 70f

        return when {
            mistakesCount >= 3 || accuracy < 60f || daysSinceStudied >= 7 -> {
                Pair(RevisionCategory.REVISE_NOW, "High Urgency: Memory decay or high error rate detected.")
            }
            daysSinceStudied in 3..6 || accuracy in 60f..80f -> {
                Pair(RevisionCategory.PRACTICE_SOON, "Moderate Urgency: Practice questions recommended to lock memory.")
            }
            else -> {
                Pair(RevisionCategory.STRONG, "Low Urgency: Mastery solid. Schedule light recall in 1 week.")
            }
        }
    }

    /**
     * WEEKLY INTELLIGENCE REPORT GENERATOR
     */
    fun generateWeeklyIntelligenceReport(
        profile: UserProfile,
        focusSessions: List<FocusSession>,
        mockAttempts: List<MockTestAttempt>,
        mistakes: List<MistakeItem>,
        plans: List<StudyPlanItem>
    ): String {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
        val weekSessions = focusSessions.filter { it.timestamp >= oneWeekAgo }
        val weekAttempts = mockAttempts.filter { it.timestamp >= oneWeekAgo }

        val totalMinutes = weekSessions.sumOf { it.actualMinutesSpent }
        val totalHours = totalMinutes / 60f
        val avgAccuracy = if (weekAttempts.isNotEmpty()) weekAttempts.map { it.accuracyPercent }.average().toFloat() else 0f

        val subjectDistribution = weekSessions.groupBy { it.subject }
            .mapValues { (_, list) -> list.sumOf { it.actualMinutesSpent } }

        val strongestSubject = subjectDistribution.maxByOrNull { it.value }?.key ?: "Physics"
        val lowestAccuracySub = weekAttempts.minByOrNull { it.accuracyPercent }?.subject ?: profile.weakSubjects.firstOrNull() ?: "Chemistry"

        return buildString {
            append("📊 StudyMate Weekly Intelligence Report\n\n")
            append("⏱️ Total Focus Time: ${"%.1f".format(totalHours)} hours\n")
            append("🎯 Average Quiz Accuracy: ${if (avgAccuracy > 0) "${"%.1f".format(avgAccuracy)}%" else "N/A"}\n")
            append("🏆 Most Studied: $strongestSubject (${subjectDistribution[strongestSubject] ?: 0}m)\n")
            append("⚠️ Needs Reinforcement: $lowestAccuracySub\n\n")
            append("💡 Action Plan for Next Week:\n")
            append("1. Start with 15-min daily mistake reviews.\n")
            append("2. Balance study time on $lowestAccuracySub.\n")
            append("3. Take 1 full-length adaptive mock test on weekends.")
        }
    }
}
