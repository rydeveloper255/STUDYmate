package com.example.notification

import android.content.Context
import com.example.data.model.MotivationCategory
import com.example.data.model.MotivationHistoryEntity
import java.util.Calendar

/**
 * Step 50: Nova Natural Hinglish Motivation & Notification Engine
 * - Natural Hinglish tone
 * - Anti-repetition & cooldown tracking
 * - Safe fallback pool + optional Gemini enhancement
 * - Event-driven triggers
 */
class NovaHinglishMotivationEngine(private val context: Context) {

    private val notificationEngine = NovaNotificationEngine(context)
    private val prefs = context.getSharedPreferences("nova_motivation_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_NOTIF_TIME = "last_motivation_notif_time"
        private const val COOLDOWN_MINUTES = 30
    }

    /**
     * Map of pre-approved Natural Hinglish motivational messages with appropriate emojis (1-4 emojis max).
     */
    private val motivationPool: Map<MotivationCategory, List<String>> = mapOf(
        MotivationCategory.FOCUS_COMPLETED to listOf(
            "🎯 Focus Complete! Aaj ka study session complete ho gaya. Nice work bhai! 💪📚",
            "🔥 Session Done! Badhiya focus tha aaj, isi flow ko maintain rakho. 🧠✨",
            "🚀 Focus Completed! Ek aur solid step target ki taraf. Keep it up bhai! 💪🔥"
        ),
        MotivationCategory.DAILY_GOAL_COMPLETED to listOf(
            "🌟 Daily Target Accomplished! Aaj ka pura goal achieve ho gaya bhai. Outstanding effort! 💪🏆",
            "🎉 Goal Completed! Sabhi planned sessions complete kar liye. Ab thoda rest banta hai. 📚✨",
            "⚡ Daily Goal Finished! Shabash bhai, target tracking ekdam spot on hai. 💪🎯"
        ),
        MotivationCategory.STREAK_MILESTONE to listOf(
            "🔥 Streak Alert! Tumhara study streak ab aur strong ho raha hai bhai. Consistency is key! 💪📚",
            "⭐ Streak Maintained! Continuous effort se result zaroor milega bhai. Bas aise hi lge raho! 🔥🎯",
            "🚀 Unstoppable Streak! Roz ka disciplined effort target clear karwayega. Great job! 💪✨"
        ),
        MotivationCategory.STUDY_COMEBACK to listOf(
            "💙 Great Comeback! Rest ke baad phirse focus mode on karna solid move hai bhai. 📚🔥",
            "🌱 Comeback Completed! Minor breaks ke baad restart karna sabse bada effort hota hai. 💪✨",
            "⚡ Strong Return! Chalo bhai, naye zeal ke saath next session start karte hain. 🎯📚"
        ),
        MotivationCategory.MISSED_SESSION to listOf(
            "💙 Koi baat nahi bhai, aaj ka session miss ho gaya. Agar time hai to ek short session se comeback kar sakte ho. 📚✨",
            "🤝 Miss ho gaya schedule? Don't worry, 15-20 min ka quick revision session try karo bhai. 💪🧠",
            "🌱 Missed session stress mat lo. Target ko adapt karke aage badhte hain. Ready ho? 🎯✨"
        ),
        MotivationCategory.WEEKLY_GOAL_PROGRESS to listOf(
            "📊 Weekly Goal Update! Tum target ki taraf steady speed se aage badh rahe ho bhai. 🚀📚",
            "🎯 Weekly Progress On Track! Sahi pacing chal rahi hai, weekend tak goal zaroor achieve hoga. 💪🔥",
            "📈 Solid Weekly Progress! Consistency se har week better speed milegi. Great job! ✨📚"
        ),
        MotivationCategory.PRACTICE_IMPROVEMENT to listOf(
            "🎯 Practice Accuracy Boost! Questions me accuracy improve hui hai bhai. Conceptual clarity badh rahi hai! 💡✨",
            "🧠 Strong Practice Performance! Mistakes kam ho rahe hain. Keep solving bhai! 💪📚",
            "🔥 Accuracy Upgrade! Speed and correctness dono improve ho rahe hain. Nice work! 🎯✨"
        ),
        MotivationCategory.MOCK_TEST_IMPROVEMENT to listOf(
            "🏆 Mock Test Milestone! Test score me improvement dikh raha hai bhai. Analysis pe dhyaan dete raho! 📊🔥",
            "📈 Mock Analysis Insight! Test attempts disciplined hain, weakness area pe focus increase karo. 💪🧠",
            "⚡ Mock Target Achieved! Great test attempt bhai, review karke key formulas revise kar lo. 🎯📚"
        ),
        MotivationCategory.EXAM_PREPARATION to listOf(
            "🌟 Aaj ka effort kal ka result better banata hai. Bas consistency maintain rakho bhai. 💪📚🔥",
            "🎯 Clear Vision! Exam date kareeb hai, daily planned targets pe rely karo. Sab badiya hoga! 🚀✨",
            "⚡ Focused Prep! Ek ek chapter master karke confidence score build hoga. Warm up for next session! 💪📚"
        ),
        MotivationCategory.GENERAL_ENCOURAGEMENT to listOf(
            "📚 Study Time! Bhai tumhara scheduled session 15 min me start hone wala hai. Ready ho? 📚🔥",
            "💡 Smart Study Tip! Hard work ke saath daily 10 min recall practice lagao bhai. Result instantly dikhega! 🧠✨",
            "🔥 Daily Power Up! Concentrated 30 minutes active recall is better than 2 hours passive reading. Let's go! 💪🎯"
        )
    )

    /**
     * Get a suitable Natural Hinglish message with anti-repetition check.
     */
    fun getMotivationMessage(
        category: MotivationCategory,
        customActualMinutes: Int? = null,
        recentSentHashes: List<String> = emptyList()
    ): String {
        // Handle personalized stats
        if (category == MotivationCategory.FOCUS_COMPLETED && customActualMinutes != null && customActualMinutes > 0) {
            return "🔥 Aaj $customActualMinutes minutes complete kar liye bhai! Consistency isi tarah maintain rakho. 💪📚"
        }

        val pool = motivationPool[category] ?: motivationPool[MotivationCategory.GENERAL_ENCOURAGEMENT]!!
        val filtered = pool.filter { msg ->
            val hash = msg.hashCode().toString()
            !recentSentHashes.contains(hash)
        }

        val selected = if (filtered.isNotEmpty()) filtered.random() else pool.random()
        return selected
    }

    /**
     * Trigger a smart notification respecting user preferences & cooldown.
     */
    fun triggerNotificationIfAllowed(
        category: MotivationCategory,
        customTitle: String? = null,
        customMinutes: Int? = null,
        notificationsEnabled: Boolean = true
    ) {
        if (!notificationsEnabled) return
        if (notificationEngine.isQuietHours()) return

        val lastNotifTime = prefs.getLong(KEY_LAST_NOTIF_TIME, 0L)
        val now = System.currentTimeMillis()
        if (now - lastNotifTime < COOLDOWN_MINUTES * 60 * 1000L) {
            return // Cooldown enforced
        }

        val message = getMotivationMessage(category, customMinutes)
        val title = customTitle ?: when (category) {
            MotivationCategory.FOCUS_COMPLETED -> "🎯 Focus Completed!"
            MotivationCategory.DAILY_GOAL_COMPLETED -> "🎉 Daily Goal Finished!"
            MotivationCategory.STREAK_MILESTONE -> "🔥 Streak Update!"
            MotivationCategory.MISSED_SESSION -> "💙 Schedule Update"
            MotivationCategory.PRACTICE_IMPROVEMENT -> "🎯 Accuracy Improvement!"
            else -> "🔔 Study Companion Nudge"
        }

        notificationEngine.sendStudyReminder(
            subject = title,
            topic = message,
            minutes = customMinutes ?: 30
        )

        prefs.edit().putLong(KEY_LAST_NOTIF_TIME, now).apply()
    }
}
