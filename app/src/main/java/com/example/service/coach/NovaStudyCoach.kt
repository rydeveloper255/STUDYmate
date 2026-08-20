package com.example.service.coach

import android.content.Context
import com.example.data.model.*
import com.example.service.AppUsageSummary
import com.example.service.NovaUsageStatsHelper
import com.example.service.intelligence.PerformanceReport
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Intelligent decision-making engine for NOVA Study Coach.
 * Analyzes local study schedule, exam timeline, weak topics, and app usage to deliver proactive guidance.
 */
object NovaStudyCoach {

    fun generateDailyBriefing(
        profile: UserProfile,
        todayPlans: List<StudyPlanItem>,
        recentMistakes: List<MistakeItem>,
        memories: List<NovaMemoryItem>
    ): String {
        val pendingCount = todayPlans.count { !it.isCompleted }
        val targetHours = profile.dailyTargetMinutes / 60
        val targetMinsRemainder = profile.dailyTargetMinutes % 60
        val targetFormatted = if (targetMinsRemainder > 0) "${targetHours}h ${targetMinsRemainder}m" else "${targetHours} hours"

        val firstPendingSubject = todayPlans.firstOrNull { !it.isCompleted }?.subject ?: profile.subjects.firstOrNull() ?: "Physics"
        val topWeakTopic = profile.weakTopics.firstOrNull() ?: recentMistakes.firstOrNull()?.topic ?: "Core Concepts"

        val daysLeft = ((profile.examDateMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)

        return buildString {
            append("Good morning Boss ☀️\n\n")
            append("Aaj ka plan:\n")
            append("📚 $pendingCount scheduled study sessions\n")
            append("🎯 $targetFormatted daily study goal\n")
            append("📝 $firstPendingSubject revision ($topWeakTopic)\n")
            if (daysLeft > 0) {
                append("⏳ ${profile.examName}: $daysLeft days remaining\n")
            }
            append("\nLet's make today count! Ready for session 1?")
        }
    }

    fun generateDailyReview(
        profile: UserProfile,
        todayPlans: List<StudyPlanItem>,
        todaySessions: List<FocusSession>
    ): String {
        val totalMinutesStudied = todaySessions.sumOf { it.actualMinutesSpent }
        val targetMinutes = profile.dailyTargetMinutes.coerceAtLeast(1)
        val completionPercent = ((totalMinutesStudied.toFloat() / targetMinutes) * 100).toInt().coerceIn(0, 100)

        val hours = totalMinutesStudied / 60
        val mins = totalMinutesStudied % 60
        val studiedFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val subjectMinutesMap = todaySessions.groupBy { it.subject }
            .mapValues { (_, sessions) -> sessions.sumOf { it.actualMinutesSpent } }

        val strongestSubject = subjectMinutesMap.maxByOrNull { it.value }?.key ?: profile.subjects.firstOrNull() ?: "Physics"
        val pendingSubject = todayPlans.firstOrNull { !it.isCompleted }?.subject ?: "Scheduled Revision"

        return buildString {
            append("🌙 NOVA Daily Review\n\n")
            append("Today's study: $studiedFormatted\n")
            append("Daily Goal: ${profile.dailyTargetMinutes / 60}h\n")
            append("Goal Progress: $completionPercent%\n\n")
            append("🏆 Strongest subject: $strongestSubject\n")
            if (pendingSubject.isNotBlank()) {
                append("📌 Priority tomorrow: $pendingSubject\n")
            }
            append("\nGreat effort today! Rest well and recharge for tomorrow.")
        }
    }

    fun generatePerformanceQueryResponse(
        userPrompt: String,
        report: PerformanceReport,
        latestAttempt: MockTestAttempt?
    ): String {
        val promptLower = userPrompt.lowercase()
        return when {
            promptLower.contains("result") || promptLower.contains("kaisa") || promptLower.contains("score") || promptLower.contains("performance") -> {
                if (latestAttempt == null) {
                    "Boss! Abhi tak koi mock test complete nahi hua hai. Phele ek Quick Test ya Mock Test start karo, fir main detailed analysis dunga!"
                } else {
                    buildString {
                        append("Boss! Is ${latestAttempt.title} mein tumhari accuracy **${latestAttempt.accuracyPercent.toInt()}%** rahi.\n")
                        if (report.previousAccuracyPercent != null) {
                            append("Pichhle test ke comparison mein: **${report.accuracyDeltaText}**.\n")
                        }
                        if (report.weakAreas.isNotEmpty() && report.weakAreas.first() != "None — Keep up the good work!") {
                            append("\n⚠️ Sabse zyada focus **${report.weakAreas.first()}** par zaroori hai.")
                        }
                        if (report.strongAreas.isNotEmpty()) {
                            append("\n✅ Strong area: **${report.strongAreas.first()}**.")
                        }
                    }
                }
            }
            promptLower.contains("ab kya") || promptLower.contains("next") || promptLower.contains("kya karun") -> {
                val nba = report.nextBestAction
                "Boss! Real-time performance analysis ke mutabiq tumhara Next Best Action hai:\n\n🎯 **${nba.title}**\n• Subject: ${nba.subject}\n• Duration: ${nba.durationMinutes} mins\n• Reason: ${nba.reason}\n\nKya main ise tumhare aaj ke plan mein add kar doon?"
            }
            else -> {
                "Boss! Tumhara overall accuracy **${report.overallAccuracyPercent.toInt()}%** hai. Total tests: ${report.totalMocksTaken}. High recommendation: ${report.topRecommendations.firstOrNull() ?: "Keep practicing!"}"
            }
        }
    }

    fun generateNovaPlanResponse(
        userPrompt: String,
        todayPlans: List<StudyPlanItem>,
        userPrefs: UserStudyPreferences
    ): String {
        val pendingPlans = todayPlans.filter { !it.isCompleted }
        if (pendingPlans.isEmpty()) {
            return "Boss! Aaj ke saare planned sessions poore ho chuke hain, ya koi naya plan nahi bana. Tap **[⚡ Adaptive AI Plan]** in Study Planner to generate fresh targeted sessions!"
        }

        val promptLower = userPrompt.lowercase()
        return when {
            promptLower.contains("1 ghanta") || promptLower.contains("1 hour") || promptLower.contains("sirf") || promptLower.contains("only") -> {
                val next1Or2 = pendingPlans.take(2)
                val summary = next1Or2.joinToString("\n") { "• **${it.subject}**: ${it.topic} (${it.targetMinutes}m) [${it.sessionType}]" }
                "Boss! Agar abhi sirf 1 ghanta hai, toh yeh top priority topics complete karo:\n\n$summary\n\nReady? Start button tap karo in Study Planner!"
            }
            promptLower.contains("miss") || promptLower.contains("chhoot") || promptLower.contains("delay") -> {
                val missed = pendingPlans.firstOrNull { it.priority == PlanPriority.HIGH } ?: pendingPlans.first()
                "Don't worry Boss! Skipped session (**${missed.subject}: ${missed.topic}**) ko overload nahi karenge. Main isse shorten karke 20 min recovery session mein convert kar raha hoon. Let's do it!"
            }
            else -> {
                val summary = pendingPlans.take(3).joinToString("\n") { "• **${it.subject}**: ${it.topic} (${it.targetMinutes}m) — ${it.aiRecommendationReason.ifBlank { "Targeted practice" }}" }
                "Boss! Aaj ke priority sessions:\n\n$summary\n\nKonsa subject pehle start karoge?"
            }
        }
    }

    fun calculateMissedSessionRecovery(
        missedPlan: StudyPlanItem,
        originalMinutes: Int = 45
    ): Pair<String, Int> {
        val suggestedMinutes = (originalMinutes / 2).coerceIn(15, 25)
        val message = "Boss, ${missedPlan.subject} (${missedPlan.topic}) ka session miss ho gaya. " +
                "Aaj ek quick ${suggestedMinutes}-minute recovery session kar lete hain. Baaki kal adjust kar denge."
        return Pair(message, suggestedMinutes)
    }

    fun generateAdaptiveRecommendation(
        profile: UserProfile,
        recentAttempts: List<MockTestAttempt>,
        recentMistakes: List<MistakeItem>,
        todayPlans: List<StudyPlanItem>
    ): StudyNowRecommendation {
        val daysLeft = ((profile.examDateMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)

        // Find subject with lowest quiz accuracy or most mistakes
        val lowestAccuracySubject = recentAttempts.minByOrNull { it.accuracyPercent }?.subject
        val mostMistakenSubject = recentMistakes.groupBy { it.subject }.maxByOrNull { it.value.size }?.key
        val candidateSubject = lowestAccuracySubject ?: mostMistakenSubject ?: profile.weakSubjects.firstOrNull() ?: profile.subjects.firstOrNull() ?: "Physics"

        val topic = recentMistakes.firstOrNull { it.subject == candidateSubject }?.topic
            ?: profile.weakTopics.firstOrNull()
            ?: todayPlans.firstOrNull { it.subject == candidateSubject && !it.isCompleted }?.topic
            ?: "High Yield Revision"

        val reasoning = if (daysLeft in 1..30) {
            "Exam sirf $daysLeft din door hai. $candidateSubject ke $topic concept par accuracy boost karna ranking ke liye crucial hai."
        } else {
            "Tumhare recent questions mein $candidateSubject: $topic par practice ki zaroorat hai. Aaj 30 minutes is par focus karna best rahega."
        }

        return StudyNowRecommendation(
            subject = candidateSubject,
            topic = topic,
            targetMinutes = 30,
            reasoning = reasoning,
            actionType = "Focus Session",
            urgencyLabel = if (daysLeft <= 30) "High Priority (Exam Prep)" else "Recommended Topic"
        )
    }

    fun checkSocialMediaIntervention(
        context: Context,
        pendingPlans: List<StudyPlanItem>
    ): String? {
        if (pendingPlans.isEmpty()) return null
        if (!NovaUsageStatsHelper.hasUsageStatsPermission(context)) return null

        val summary = NovaUsageStatsHelper.getTodayDistractingAppUsage(context)
        if (summary.totalDistractingMinutes >= 35 && summary.topDistractingAppName != null) {
            val pendingSubject = pendingPlans.firstOrNull()?.subject ?: "Study"
            return "Boss 😅 ${summary.topDistractingAppName} par ${summary.topDistractingAppMinutes} minutes ho gaye hain. " +
                    "Aaj ka $pendingSubject session abhi pending hai. 25 minutes padh lete hain?"
        }
        return null
    }

    fun checkBreakRecommendation(focusedMinutes: Int): String? {
        if (focusedMinutes >= 50) {
            return "Boss, kaafi focused work ho gaya (${focusedMinutes}m). 5-10 minute ka healthy break le lo, phir next session start karte hain ☕"
        }
        return null
    }
}
