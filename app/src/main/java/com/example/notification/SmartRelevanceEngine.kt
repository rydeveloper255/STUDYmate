package com.example.notification

import com.example.data.model.NotificationPreference
import com.example.data.model.RecruitmentContentType
import com.example.data.model.RecruitmentEntity
import com.example.data.model.UserProfile
import com.example.data.model.VacancyStatus

/**
 * Step 55: Smart Relevance & Personalization Engine for Notifications.
 * - Calculates internal relevance score (never exposed raw to users)
 * - Enforces zero-false-claim rule ("Apply now" ONLY when status is strictly OPEN)
 * - Formats verified event notifications in authentic Hindi / English
 */
object SmartRelevanceEngine {

    const val MINIMUM_RELEVANCE_THRESHOLD = 40

    /**
     * Calculates an internal relevance score (0-100) for a recruitment item.
     */
    fun calculateVacancyRelevance(
        item: RecruitmentEntity,
        userProfile: UserProfile?,
        prefs: NotificationPreference
    ): Int {
        var score = 30 // Base score

        val userTargetExam = (userProfile?.examName ?: "").lowercase()
        val userTargetState = "All India"
        val itemCategory = item.examCategory.lowercase()
        val itemState = item.state

        // 1. Muted category check
        if (prefs.mutedCategories.any { it.equals(item.examCategory, ignoreCase = true) }) {
            return 0
        }

        // Target exam only check
        if (prefs.targetExamOnly && userTargetExam.isNotBlank()) {
            val matches = userTargetExam.contains(itemCategory) || itemCategory.contains(userTargetExam) ||
                (userTargetExam.contains("railway") && itemCategory.contains("railway")) ||
                (userTargetExam.contains("ssc") && itemCategory.contains("ssc")) ||
                (userTargetExam.contains("bank") && itemCategory.contains("bank")) ||
                (userTargetExam.contains("upsc") && itemCategory.contains("upsc"))
            if (!matches) return 0
        }

        // 2. Target Exam / Category Match (+35)
        if (userTargetExam.isNotBlank()) {
            if (userTargetExam.contains(itemCategory) || itemCategory.contains(userTargetExam) ||
                (userTargetExam.contains("railway") && itemCategory.contains("railway")) ||
                (userTargetExam.contains("ssc") && itemCategory.contains("ssc")) ||
                (userTargetExam.contains("bank") && itemCategory.contains("bank")) ||
                (userTargetExam.contains("upsc") && itemCategory.contains("upsc")) ||
                (userTargetExam.contains("gate") && itemCategory.contains("engineering")) ||
                (userTargetExam.contains("neet") && itemCategory.contains("medical"))
            ) {
                score += 35
            }
        }

        // 3. State Match (+25)
        if (itemState.equals("All India", ignoreCase = true)) {
            score += 25
        } else if (itemState.equals(userTargetState, ignoreCase = true)) {
            score += 25
        } else if (prefs.homeStateOnly) {
            // Penalize unrelated distant state vacancies if user requested home-state only
            score -= 50
        }

        // 4. User Interest & Saved status (+20)
        if (item.isSaved || item.isApplied()) {
            score += 20
        }

        // 5. Freshness (+15)
        val hoursSinceFetch = (System.currentTimeMillis() - item.fetchedAt) / (1000 * 3600)
        if (hoursSinceFetch <= 48) {
            score += 15
        }

        // 6. Urgency & Closing Soon (+10)
        val computedStatus = item.getComputedStatus()
        if (computedStatus == VacancyStatus.LAST_DAY || computedStatus == VacancyStatus.EXTENDED) {
            score += 10
        }

        return score.coerceIn(0, 100)
    }

    fun calculateVacancyRelevance(
        item: RecruitmentEntity,
        userProfile: UserProfile?,
        isTargetExamOnly: Boolean = false,
        isHomeStateOnly: Boolean = false,
        mutedCategories: List<String> = emptyList(),
        isSavedByUser: Boolean = false
    ): Int {
        val prefs = NotificationPreference(
            targetExamOnly = isTargetExamOnly,
            homeStateOnly = isHomeStateOnly,
            mutedCategories = mutedCategories
        )
        val testItem = if (isSavedByUser) item.copy(isSaved = true) else item
        return calculateVacancyRelevance(testItem, userProfile, prefs)
    }

    /**
     * Aliases for test convenience
     */
    fun formatSafeVacancyNotification(item: RecruitmentEntity, isHindi: Boolean = true): Pair<String, String> =
        formatNewVacancyNotification(item, isHindi)

    fun formatDeadlineNotification(item: RecruitmentEntity, daysRemaining: Int, isHindi: Boolean = true): Pair<String, String> =
        formatDeadlineAlert(item, daysRemaining, isHindi)

    /**
     * Generates a safe, verified new vacancy notification.
     * RULE 6: Never says "Apply now" unless status is verified OPEN.
     */
    fun formatNewVacancyNotification(
        item: RecruitmentEntity,
        isHindiPreferred: Boolean = true
    ): Pair<String, String> {
        val status = item.getComputedStatus()
        val vacancyCountText = if (item.totalVacancies != null && item.totalVacancies > 0) "${item.totalVacancies} Posts" else ""

        val title = if (isHindiPreferred) {
            "🚀 नई Vacancy: ${item.organization.take(30)}"
        } else {
            "🚀 New Vacancy: ${item.organization.take(30)}"
        }

        val message = if (status.isApplyActive) {
            if (isHindiPreferred) {
                "${item.postName} के लिए आवेदन शुरू हो गए हैं${if (vacancyCountText.isNotBlank()) " ($vacancyCountText)" else ""}. अंतिम तिथि: ${item.applicationLastDate ?: "अधिसूचना देखें"}."
            } else {
                "Applications open for ${item.postName}${if (vacancyCountText.isNotBlank()) " ($vacancyCountText)" else ""}. Last date: ${item.applicationLastDate ?: "Check notice"}."
            }
        } else {
            // Application NOT currently active or upcoming
            if (isHindiPreferred) {
                "${item.postName} की आधिकारिक अधिसूचना जारी हुई है${if (vacancyCountText.isNotBlank()) " ($vacancyCountText)" else ""}. पूरी जानकारी StudyMate में देखें।"
            } else {
                "Official notification released for ${item.postName}${if (vacancyCountText.isNotBlank()) " ($vacancyCountText)" else ""}. View details in StudyMate."
            }
        }

        return Pair(title, message)
    }

    /**
     * Generates deadline alert. Only generated when deadline is verified.
     */
    fun formatDeadlineAlert(
        item: RecruitmentEntity,
        daysRemaining: Int,
        isHindiPreferred: Boolean = true
    ): Pair<String, String> {
        val deadlineDate = item.applicationLastDate ?: "शीघ्र"

        val title = if (daysRemaining == 0) {
            if (isHindiPreferred) "🔴 अंतिम दिन: ${item.organization.take(30)}" else "🔴 Last Day: ${item.organization.take(30)}"
        } else {
            if (isHindiPreferred) "⏰ ${item.organization.take(25)} Vacancy Reminder" else "⏰ ${item.organization.take(25)} Deadline Alert"
        }

        val message = if (isHindiPreferred) {
            if (daysRemaining == 0) {
                "${item.postName} के लिए आवेदन करने का आज अंतिम दिन है। Last date: $deadlineDate."
            } else {
                "${item.postName} की application की last date पास आ रही है। Last date: $deadlineDate ($daysRemaining दिन शेष)."
            }
        } else {
            if (daysRemaining == 0) {
                "Today is the last day to apply for ${item.postName}. Last date: $deadlineDate."
            } else {
                "Application deadline is approaching for ${item.postName}. Last date: $deadlineDate ($daysRemaining days left)."
            }
        }

        return Pair(title, message)
    }

    /**
     * Generates genuine Result declaration notification.
     */
    fun formatResultNotification(
        item: RecruitmentEntity,
        isHindiPreferred: Boolean = true
    ): Pair<String, String> {
        val title = if (isHindiPreferred) "📢 नया Result जारी" else "📢 Result Declared"
        val message = if (isHindiPreferred) {
            "${item.organization} ${item.postName} का result जारी हो गया है। पूरी जानकारी StudyMate में देखें।"
        } else {
            "${item.organization} has released the result for ${item.postName}. View results in StudyMate."
        }
        return Pair(title, message)
    }

    /**
     * Generates genuine Admit Card release notification.
     */
    fun formatAdmitCardNotification(
        item: RecruitmentEntity,
        isHindiPreferred: Boolean = true
    ): Pair<String, String> {
        val title = if (isHindiPreferred) "🎫 Admit Card जारी" else "🎫 Admit Card Released"
        val examDateSnippet = if (!item.examDate.isNullOrBlank()) {
            if (isHindiPreferred) "\nExam Date: ${item.examDate}" else "\nExam Date: ${item.examDate}"
        } else ""

        val message = if (isHindiPreferred) {
            "${item.organization} ${item.postName} का admit card उपलब्ध है।$examDateSnippet"
        } else {
            "Admit card is available for ${item.organization} ${item.postName}.$examDateSnippet"
        }
        return Pair(title, message)
    }

    /**
     * Generates study session reminder with respectful tone.
     */
    fun formatStudyReminder(
        subject: String,
        topic: String,
        minutesAhead: Int = 30,
        isHinglish: Boolean = true
    ): Pair<String, String> {
        val title = "📚 Study Reminder"
        val message = if (isHinglish) {
            if (topic.isNotBlank()) {
                "$minutesAhead minutes में तुम्हारा $subject ($topic) session शुरू होने वाला है।"
            } else {
                "$minutesAhead minutes में तुम्हारा $subject session शुरू होने वाला है।"
            }
        } else {
            if (topic.isNotBlank()) {
                "Your $subject ($topic) study session begins in $minutesAhead minutes."
            } else {
                "Your $subject study session begins in $minutesAhead minutes."
            }
        }
        return Pair(title, message)
    }

    /**
     * Generates gentle missed session recovery without guilt.
     */
    fun formatMissedSessionMessage(
        subject: String,
        suggestedMinutes: Int = 20,
        isHinglish: Boolean = true
    ): Pair<String, String> {
        val title = "🔄 Study Recovery"
        val message = if (isHinglish) {
            "आज का $subject session miss हो गया. कोई बात नहीं 👍 Chaaho to ab ek short ($suggestedMinutes min) session start kar sakte ho."
        } else {
            "Missed your scheduled $subject session today. No worries 👍 You can start a quick $suggestedMinutes min recovery sprint whenever ready."
        }
        return Pair(title, message)
    }

    /**
     * Generates focus started confirmation message.
     */
    fun formatFocusStartedMessage(
        subject: String,
        minutes: Int,
        isHinglish: Boolean = true
    ): Pair<String, String> {
        val title = "🎯 Focus Mode Started"
        val message = if (isHinglish) {
            "$subject — $minutes min\nAb bas session pe focus karo 💪"
        } else {
            "$subject — $minutes min\nFocus mode active. Stay in the zone 💪"
        }
        return Pair(title, message)
    }

    /**
     * Generates focus completed celebration message.
     */
    fun formatFocusCompletedMessage(
        minutes: Int,
        isHinglish: Boolean = true
    ): Pair<String, String> {
        val title = "🔥 Focus Complete"
        val message = if (isHinglish) {
            "$minutes minutes ka session complete! Nice work bhai 💪"
        } else {
            "$minutes minutes focus session completed! Great work 💪"
        }
        return Pair(title, message)
    }

    /**
     * Generates focus interrupted message without guilt.
     */
    fun formatFocusInterruptedMessage(
        isHinglish: Boolean = true
    ): Pair<String, String> {
        val title = "Focus Interrupted"
        val message = if (isHinglish) {
            "Focus session interrupt ho gaya. Jab ready ho, dobara start kar sakte ho."
        } else {
            "Focus session was interrupted. Feel free to restart whenever you are ready."
        }
        return Pair(title, message)
    }
}
