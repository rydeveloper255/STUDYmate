package com.example.service.intelligence

import com.example.data.model.NovaActionType
import com.example.data.model.NovaIntent
import com.example.data.model.NovaIntentClassificationResult

/**
 * Step 54: Nova Intent Classifier
 * Categorizes user queries into structured intents with high accuracy.
 * Identifies deterministic local fast-paths to avoid unnecessary LLM latency & quota burns.
 */
object NovaIntentClassifier {

    fun classify(userPrompt: String): NovaIntentClassificationResult {
        val raw = userPrompt.trim()
        val lower = raw.lowercase()

        // 1. Study Status / Progress Check
        if (lower.contains("kaisa chal raha") || lower.contains("how am i doing") ||
            lower.contains("mera progress") || lower.contains("my progress") ||
            lower.contains("aaj kitna padha") || lower.contains("today focus") ||
            lower.contains("meri streak") || lower.contains("streak check") ||
            lower.contains("performance analysis") || lower.contains("padhai kaisi chal") ||
            lower.contains("show progress") || lower.contains("study status")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.STUDY_STATUS,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.SHOW_PROGRESS
            )
        }

        // 2. Start Focus Session / Timer
        if (lower.contains("focus start") || lower.contains("start focus") ||
            lower.contains("focus mode") || lower.contains("pomodoro") ||
            lower.contains("padhai shuru") || lower.contains("timer chalao") ||
            lower.contains("25 minute") || lower.contains("study timer") ||
            lower.contains("deep work") || lower.contains("focus shield")
        ) {
            val minutes = when {
                lower.contains("50 min") || lower.contains("45 min") -> 45
                lower.contains("15 min") -> 15
                lower.contains("60 min") || lower.contains("1 hour") || lower.contains("1 ghanta") -> 60
                else -> 25
            }
            return NovaIntentClassificationResult(
                intent = NovaIntent.START_FOCUS,
                extractedMinutes = minutes,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.START_FOCUS,
                actionPayload = """{"minutes":$minutes}"""
            )
        }

        // 3. Current Affairs & Daily News
        if (lower.contains("current affair") || lower.contains("current affairs") ||
            lower.contains("aaj ke current affairs") || lower.contains("aaj ka current affairs") ||
            lower.contains("daily ca") || lower.contains("samayiki") ||
            lower.contains("today ca") || lower.contains("aaj ki khabar")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.CURRENT_AFFAIRS,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.OPEN_CURRENT_AFFAIRS
            )
        }

        // 4. Recruitment, Vacancy, Admit Card, Results Radar
        if (lower.contains("vacancy") || lower.contains("vacancies") ||
            lower.contains("sarkari job") || lower.contains("sarkari naukri") ||
            lower.contains("admit card") || lower.contains("hall ticket") ||
            lower.contains("result") || lower.contains("cutoff") ||
            lower.contains("answer key") || lower.contains("sarkari result")
        ) {
            val action = when {
                lower.contains("result") || lower.contains("cutoff") -> NovaActionType.OPEN_RESULTS_HUB
                lower.contains("admit") || lower.contains("hall ticket") -> NovaActionType.OPEN_ADMIT_CARDS
                else -> NovaActionType.OPEN_VACANCIES
            }
            return NovaIntentClassificationResult(
                intent = NovaIntent.RECRUITMENT_INFO,
                isDeterministicFastPath = true,
                proposedAction = action
            )
        }

        // 5. CBT Mock Test
        if (lower.contains("mock test") || lower.contains("cbt test") ||
            lower.contains("test dena hai") || lower.contains("start test") ||
            lower.contains("test shuru") || lower.contains("full mock")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.MOCK_TEST,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.OPEN_MOCK_TEST
            )
        }

        // 6. Practice Quiz / MCQs
        if (lower.contains("practice question") || lower.contains("practice questions") ||
            lower.contains("quiz start") || lower.contains("quiz shuru") ||
            lower.contains("start quiz") || lower.contains("mcq solve") ||
            lower.contains("questions solve")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.PRACTICE_QUIZ,
                isDeterministicFastPath = false,
                proposedAction = NovaActionType.START_QUIZ
            )
        }

        // 7. Spaced Revision / Weak Topics
        if (lower.contains("kya revise") || lower.contains("revision due") ||
            lower.contains("weak topic") || lower.contains("weak topics") ||
            lower.contains("flashcard") || lower.contains("flashcards") ||
            lower.contains("spaced repetition") || lower.contains("mistakes revise")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.REVISION_DUE,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.OPEN_SMART_NOTES
            )
        }

        // 8. Study Plan & Daily Timetable
        if (lower.contains("aaj ka plan") || lower.contains("timetable") ||
            lower.contains("schedule") || lower.contains("study planner") ||
            lower.contains("plan day") || lower.contains("study plan")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.PLAN_DAY,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.OPEN_STUDY_PLAN
            )
        }

        // 9. Fact Verification / Check Claim
        if (lower.startsWith("verify") || lower.startsWith("check if true") ||
            lower.contains("kya ye sach hai") || lower.contains("fact check") ||
            lower.contains("is this true")
        ) {
            val query = raw.replace(Regex("(?i)^(verify|check if true|kya ye sach hai|fact check|is this true)\\s*:?"), "").trim()
            return NovaIntentClassificationResult(
                intent = NovaIntent.FACT_CHECK,
                extractedQuery = query.ifBlank { raw },
                isDeterministicFastPath = false,
                proposedAction = NovaActionType.VERIFY_FACT
            )
        }

        // 10. Why Study This / Exam Weightage
        if (lower.startsWith("why should i study") || lower.contains("kyun padhun") ||
            lower.contains("exam me aayega kya") || lower.contains("weightage kya hai") ||
            lower.contains("why is this important")
        ) {
            val topic = raw.replace(Regex("(?i)^(why should i study this|why study this|kyun padhun|why is this important)\\s*:?"), "").trim()
            return NovaIntentClassificationResult(
                intent = NovaIntent.WHY_STUDY,
                extractedTopic = topic.ifBlank { "Core Topic" },
                isDeterministicFastPath = false,
                proposedAction = NovaActionType.WHY_STUDY_THIS
            )
        }

        // 11. Simplify Explanation / ELI5
        if (lower.contains("aur aasan") || lower.contains("aasaan bhasha me") ||
            lower.contains("simple words") || lower.contains("eli5") ||
            lower.contains("explain easier") || lower.contains("easy language")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.EXPLAIN_EASIER,
                isDeterministicFastPath = false
            )
        }

        // 12. Remember Preference
        if (lower.startsWith("remember that") || lower.contains("yaad rakhna") ||
            lower.contains("save preference") || lower.contains("meri habit")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.REMEMBER_PREFERENCE,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.OPEN_MEMORY
            )
        }

        // 13. Notification Inquiry ("Ye kaunsi vacancy thi?", "Kaunsa notification aaya tha?", "Recent alerts")
        if (lower.contains("kaunsi vacancy thi") || lower.contains("konsi vacancy") ||
            lower.contains("kaunsa notification") || lower.contains("konsa notification") ||
            lower.contains("recent notification") || lower.contains("recent alert") ||
            lower.contains("last notification") || lower.contains("kya notification aaya") ||
            lower.contains("notification dikhao") || lower.contains("open notification")
        ) {
            return NovaIntentClassificationResult(
                intent = NovaIntent.NOTIFICATION_INFO,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.OPEN_NOTIFICATIONS
            )
        }

        // 14. Notification Settings Update ("Railway ke notifications band kar do", "Motivation alert band karo", "Quiet hours on karo")
        if (lower.contains("notification band") || lower.contains("notifications band") ||
            lower.contains("notification off") || lower.contains("notifications off") ||
            lower.contains("notification on") || lower.contains("notifications on") ||
            lower.contains("mute notification") || lower.contains("unmute notification") ||
            lower.contains("quiet hours") || lower.contains("alert settings")
        ) {
            val categoryToMute = when {
                lower.contains("railway") -> "RAILWAY"
                lower.contains("ssc") -> "SSC"
                lower.contains("bank") -> "BANKING"
                lower.contains("upsc") -> "UPSC"
                lower.contains("motivation") -> "MOTIVATION"
                lower.contains("deadline") -> "DEADLINE"
                lower.contains("result") -> "RESULTS"
                lower.contains("admit card") -> "ADMIT_CARD"
                else -> "GENERAL"
            }
            return NovaIntentClassificationResult(
                intent = NovaIntent.NOTIFICATION_SETTINGS,
                isDeterministicFastPath = true,
                proposedAction = NovaActionType.UPDATE_NOTIFICATION_SETTINGS,
                actionPayload = """{"targetCategory":"$categoryToMute","enable":${!lower.contains("band") && !lower.contains("off") && !lower.contains("mute")}}"""
            )
        }

        // 15. Academic Doubt Solving (Default for subject questions)
        return NovaIntentClassificationResult(
            intent = NovaIntent.SOLVE_DOUBT,
            isDeterministicFastPath = false
        )
    }
}
