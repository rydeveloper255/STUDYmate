package com.example.service.intelligence

import com.example.data.model.*

/**
 * Step 54: Nova Context Builder
 * Constructs compact, minimal, privacy-conscious context per user intent.
 * Guarantees zero hallucinations by only extracting verified real persisted metrics.
 */
object NovaContextBuilder {

    /**
     * Builds a compact context tailored specifically to the given intent.
     */
    fun buildCompactContext(
        intent: NovaIntent,
        studyContext: NovaStudyContext,
        activeScreen: String = "Chat"
    ): NovaCompactContext {
        return when (intent) {
            NovaIntent.STUDY_STATUS -> {
                // For progress checks: prioritize real study minutes, streak, pending vs completed tasks
                NovaCompactContext(
                    studentName = studyContext.studentName,
                    targetExam = studyContext.targetExam,
                    examDaysRemaining = studyContext.examDaysRemaining,
                    preferredLanguage = studyContext.preferredLanguage,
                    activeScreen = activeScreen,
                    todayFocusMinutes = studyContext.todayFocusMinutes,
                    dailyTargetMinutes = studyContext.dailyTargetMinutes,
                    currentStreak = studyContext.currentStreak,
                    completedTasksToday = studyContext.completedPlanCount,
                    pendingTasksCount = studyContext.pendingPlanCount,
                    pendingTasksPreview = studyContext.pendingTasksSummary.take(2),
                    revisionsDueCount = studyContext.revisionsDueCount,
                    recentMockAccuracy = studyContext.recentMockAccuracyPercent.takeIf { it > 0f },
                    topWeakTopics = studyContext.weakTopics.take(2),
                    topStrongTopics = studyContext.strongTopics.take(2)
                )
            }
            NovaIntent.START_FOCUS, NovaIntent.PLAN_DAY -> {
                // For scheduling & focus: prioritize daily target, current focus, pending tasks, weak areas
                NovaCompactContext(
                    studentName = studyContext.studentName,
                    targetExam = studyContext.targetExam,
                    examDaysRemaining = studyContext.examDaysRemaining,
                    preferredLanguage = studyContext.preferredLanguage,
                    activeScreen = activeScreen,
                    todayFocusMinutes = studyContext.todayFocusMinutes,
                    dailyTargetMinutes = studyContext.dailyTargetMinutes,
                    pendingTasksCount = studyContext.pendingPlanCount,
                    pendingTasksPreview = studyContext.todayTasks.ifEmpty { studyContext.pendingTasksSummary.take(3) },
                    topWeakTopics = studyContext.weakTopics.take(3)
                )
            }
            NovaIntent.REVISION_DUE -> {
                // For revision: prioritize due cards and weak topics
                NovaCompactContext(
                    studentName = studyContext.studentName,
                    targetExam = studyContext.targetExam,
                    examDaysRemaining = studyContext.examDaysRemaining,
                    preferredLanguage = studyContext.preferredLanguage,
                    activeScreen = activeScreen,
                    revisionsDueCount = studyContext.revisionsDueCount,
                    revisionsDuePreview = studyContext.revisionsDueTopics.take(4),
                    topWeakTopics = studyContext.weakTopics.take(3)
                )
            }
            NovaIntent.SOLVE_DOUBT, NovaIntent.EXPLAIN_EASIER, NovaIntent.WHY_STUDY -> {
                // For academic doubts: minimal context to keep prompt clean, focused, and fast
                NovaCompactContext(
                    studentName = studyContext.studentName,
                    targetExam = studyContext.targetExam,
                    examDaysRemaining = studyContext.examDaysRemaining,
                    preferredLanguage = studyContext.preferredLanguage,
                    activeScreen = activeScreen,
                    topWeakTopics = studyContext.weakTopics.take(1)
                )
            }
            NovaIntent.RECRUITMENT_INFO -> {
                // For vacancies/recruitment: focus on target exam
                NovaCompactContext(
                    studentName = studyContext.studentName,
                    targetExam = studyContext.targetExam,
                    examDaysRemaining = studyContext.examDaysRemaining,
                    preferredLanguage = studyContext.preferredLanguage,
                    activeScreen = activeScreen
                )
            }
            else -> {
                // General compact context
                NovaCompactContext(
                    studentName = studyContext.studentName,
                    targetExam = studyContext.targetExam,
                    examDaysRemaining = studyContext.examDaysRemaining,
                    preferredLanguage = studyContext.preferredLanguage,
                    activeScreen = activeScreen,
                    todayFocusMinutes = studyContext.todayFocusMinutes,
                    dailyTargetMinutes = studyContext.dailyTargetMinutes,
                    currentStreak = studyContext.currentStreak,
                    pendingTasksCount = studyContext.pendingPlanCount,
                    revisionsDueCount = studyContext.revisionsDueCount,
                    topWeakTopics = studyContext.weakTopics.take(2),
                    memoriesSummary = studyContext.memories.take(4).map { "${it.key}: ${it.value}" }
                )
            }
        }
    }

    /**
     * Formats compact context into clean, scannable system prompt lines.
     */
    fun formatContextForPrompt(ctx: NovaCompactContext): String {
        val sb = StringBuilder()
        sb.append("VERIFIED REAL APP STATE (Zero Hallucinations - Do NOT invent other numbers):\n")
        sb.append("- Student Name: ${ctx.studentName}\n")
        sb.append("- Target Exam: ${ctx.targetExam} (${ctx.examDaysRemaining} days left)\n")
        sb.append("- Today Focused: ${ctx.todayFocusMinutes} mins / Target: ${ctx.dailyTargetMinutes} mins\n")
        sb.append("- Study Streak: ${ctx.currentStreak} days\n")
        if (ctx.pendingTasksCount > 0) {
            sb.append("- Pending Tasks: ${ctx.pendingTasksCount} (${ctx.pendingTasksPreview.joinToString(", ")})\n")
        }
        if (ctx.revisionsDueCount > 0) {
            sb.append("- Revisions Due: ${ctx.revisionsDueCount} (${ctx.revisionsDuePreview.joinToString(", ")})\n")
        }
        if (ctx.topWeakTopics.isNotEmpty()) {
            sb.append("- Weak Topics: ${ctx.topWeakTopics.joinToString(", ")}\n")
        }
        if (ctx.recentMockAccuracy != null) {
            sb.append("- Recent Test Accuracy: ${ctx.recentMockAccuracy.toInt()}%\n")
        }
        return sb.toString()
    }
}
