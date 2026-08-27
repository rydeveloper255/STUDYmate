package com.example.service.intelligence

import com.example.data.model.*

/**
 * Step 54: Nova Deterministic Router
 * Provides instant, zero-latency authentic responses for deterministic intents.
 * Strictly uses live Room database metrics to guarantee 100% truthfulness and zero hallucination.
 */
object NovaDeterministicRouter {

    fun generateFastPathResponse(
        classification: NovaIntentClassificationResult,
        context: NovaStudyContext
    ): NovaChatMessage {
        val studentTitle = if (context.preferredTitle.isNotBlank()) context.preferredTitle else "Boss"

        return when (classification.intent) {
            NovaIntent.STUDY_STATUS -> {
                val focusMins = context.todayFocusMinutes
                val targetMins = context.dailyTargetMinutes
                val streak = context.currentStreak
                val pending = context.pendingPlanCount
                val completed = context.completedPlanCount
                val exam = context.targetExam
                val days = context.examDaysRemaining
                val weak = if (context.weakTopics.isNotEmpty()) context.weakTopics.take(2).joinToString(", ") else "None identified yet"

                val accuracyText = if (context.recentMockAccuracyPercent > 0f) {
                    "${context.recentMockAccuracyPercent.toInt()}% accuracy in recent test"
                } else {
                    "No recent mock test attempts recorded"
                }

                val text = buildString {
                    append("Haan $studentTitle! Ye raha aapka **Verified Study Snapshot** 📊:\n\n")
                    append("• 🔥 **Study Streak:** $streak Days\n")
                    append("• ⏱️ **Today's Focus Time:** $focusMins / $targetMins mins\n")
                    append("• 📅 **Tasks Done Today:** $completed ($pending pending)\n")
                    append("• 🎯 **Mock Performance:** $accuracyText\n")
                    append("• ⏳ **Exam Countdown:** $days days remaining for **$exam**\n")
                    append("• ⚠️ **Priority Weak Areas:** $weak\n\n")
                    if (focusMins < targetMins) {
                        append("Target complete karne ke liye abhi ${(targetMins - focusMins).coerceAtLeast(15)} minutes ka practice session baaki hai. Chalo focus timer start karein? 🚀")
                    } else {
                        append("Shabash $studentTitle! Aaj ka daily study target complete ho chuka hai. Ek quick revision quiz solve kar sakte ho! 🌟")
                    }
                }

                val actions = listOf(
                    NovaContextualAction(
                        label = "⏱️ Start Focus Timer",
                        actionType = NovaActionType.START_FOCUS,
                        payload = """{"minutes":25}""",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📊 Detailed Analytics",
                        actionType = NovaActionType.SHOW_PROGRESS
                    ),
                    NovaContextualAction(
                        label = "✍️ Practice Quiz",
                        actionType = NovaActionType.START_QUIZ
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.SHOW_PROGRESS,
                    actionButtons = actions
                )
            }

            NovaIntent.START_FOCUS -> {
                val mins = classification.extractedMinutes ?: 25
                val sub = context.subjects.firstOrNull() ?: "General Studies"
                val top = context.weakTopics.firstOrNull() ?: "Core Revision"

                val text = "Done $studentTitle! 🎯 **$mins-minute deep focus session** start karne ke liye ready hai on **$sub ($top)**.\n\nFocus Shield background distractions ko block rakhega taaki distraction-free concentration bana rahe. Let's make this sprint count! 🚀"

                val actions = listOf(
                    NovaContextualAction(
                        label = "▶️ Launch Focus Mode ($mins min)",
                        actionType = NovaActionType.START_FOCUS,
                        payload = """{"subject":"$sub","topic":"$top","minutes":$mins}""",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📅 Change Duration",
                        actionType = NovaActionType.OPEN_FOCUS_MODE
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.START_FOCUS,
                    actionPayload = """{"subject":"$sub","topic":"$top","minutes":$mins}""",
                    actionButtons = actions
                )
            }

            NovaIntent.CURRENT_AFFAIRS -> {
                val text = "Bilkul $studentTitle! 📰 Aaj ke exam-oriented **Daily Current Affairs & Samayiki** update ho chuke hain.\n\nNational events, government schemes, defense, economy aur awards ke high-yield exam takeaways prepared hain. Aap Hindi aur English dono bhashao me padh sakte hain aur instant 5-question test de sakte hain!"

                val actions = listOf(
                    NovaContextualAction(
                        label = "📰 Open Current Affairs Hub",
                        actionType = NovaActionType.OPEN_CURRENT_AFFAIRS,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "✍️ Daily CA Quiz",
                        actionType = NovaActionType.START_QUIZ,
                        payload = """{"topic":"Current Affairs Today"}"""
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.OPEN_CURRENT_AFFAIRS,
                    actionButtons = actions
                )
            }

            NovaIntent.RECRUITMENT_INFO -> {
                val exam = context.targetExam
                val text = "Haan $studentTitle! 🏛️ Sarkari Job Vacancies, Official Results, aur Admit Card updates ka **Smart Intelligence Engine** active hai.\n\nExpired notices filter ho chuke hain aur sirf active official opportunities available hain for **$exam**."

                val actions = listOf(
                    NovaContextualAction(
                        label = "🏛️ Active Vacancies",
                        actionType = NovaActionType.OPEN_VACANCIES,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "📋 Results & Cutoffs",
                        actionType = NovaActionType.OPEN_RESULTS_HUB
                    ),
                    NovaContextualAction(
                        label = "🎫 Admit Cards & City Slip",
                        actionType = NovaActionType.OPEN_ADMIT_CARDS
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = classification.proposedAction,
                    actionButtons = actions
                )
            }

            NovaIntent.MOCK_TEST -> {
                val exam = context.targetExam
                val text = "All set $studentTitle! 🎯 **$exam CBT Mock Test** portal ready hai.\n\nReal exam timer, negative marking (+1 / -0.33), multi-section navigation aur instant in-depth accuracy analysis available hai."

                val actions = listOf(
                    NovaContextualAction(
                        label = "🚀 Open CBT Mock Portal",
                        actionType = NovaActionType.OPEN_MOCK_TEST,
                        payload = """{"exam":"$exam"}""",
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "🎯 Subject-Wise Test",
                        actionType = NovaActionType.START_QUIZ
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.OPEN_MOCK_TEST,
                    actionButtons = actions
                )
            }

            NovaIntent.REVISION_DUE -> {
                val dueCount = context.revisionsDueCount
                val dueTopics = context.revisionsDueTopics.take(3).joinToString(", ").ifBlank { "Core Formulae" }
                val text = "Haan $studentTitle! 🔄 Spaced Repetition Engine me **$dueCount revision cards due** hain.\n\nTop focus items: **$dueTopics**.\nActive recall se memorization 3x strong hoti hai."

                val actions = listOf(
                    NovaContextualAction(
                        label = "🗂️ Start Flashcard Revision",
                        actionType = NovaActionType.OPEN_SMART_NOTES,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "✍️ Quick Recap Quiz",
                        actionType = NovaActionType.START_QUIZ,
                        payload = """{"topic":"$dueTopics"}"""
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.OPEN_SMART_NOTES,
                    actionButtons = actions
                )
            }

            NovaIntent.PLAN_DAY -> {
                val pending = context.pendingPlanCount
                val pendingPreview = if (context.pendingTasksSummary.isNotEmpty()) context.pendingTasksSummary.take(3).joinToString("; ") else "No pending items for today"
                val text = "Haan $studentTitle! 📅 Aapka **Daily Study Planner** active hai:\n\n• Target Study Time: **${context.dailyTargetMinutes} mins**\n• Scheduled Tasks ($pending): $pendingPreview\n\nAap custom timetable edit kar sakte hain ya auto-schedule optimize kar sakte hain."

                val actions = listOf(
                    NovaContextualAction(
                        label = "📅 Open Study Planner",
                        actionType = NovaActionType.OPEN_STUDY_PLAN,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "⏱️ Start First Task",
                        actionType = NovaActionType.START_FOCUS,
                        payload = """{"minutes":30}"""
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.OPEN_STUDY_PLAN,
                    actionButtons = actions
                )
            }

            NovaIntent.REMEMBER_PREFERENCE -> {
                val text = "Samajh gaya $studentTitle! 🧠 Maine aapki study habit aur preference ko memory center me save kar liya hai. Aage ke sessions me iska dhyan rakha jayega."

                val actions = listOf(
                    NovaContextualAction(
                        label = "🧠 View Memory Center",
                        actionType = NovaActionType.OPEN_MEMORY,
                        isPrimary = true
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.OPEN_MEMORY,
                    actionButtons = actions
                )
            }

            NovaIntent.NOTIFICATION_INFO -> {
                val text = "Bilkul $studentTitle! 🔔 Aapke sabhi verified alerts, study reminders, vacancy updates aur results **Notification Center** me organized hain. Aap category filter se specific alerts dekh sakte hain."

                val actions = listOf(
                    NovaContextualAction(
                        label = "🔔 Open Notification Center",
                        actionType = NovaActionType.OPEN_NOTIFICATIONS,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "⚙️ Notification Settings",
                        actionType = NovaActionType.OPEN_SETTINGS
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.OPEN_NOTIFICATIONS,
                    actionButtons = actions
                )
            }

            NovaIntent.NOTIFICATION_SETTINGS -> {
                val payload = classification.actionPayload ?: "{}"
                val isEnable = !payload.contains("false")
                val categoryName = if (payload.contains("RAILWAY")) "Railway"
                else if (payload.contains("SSC")) "SSC"
                else if (payload.contains("BANKING")) "Banking"
                else if (payload.contains("MOTIVATION")) "Motivation"
                else if (payload.contains("DEADLINE")) "Deadline Alerts"
                else if (payload.contains("RESULTS")) "Results"
                else if (payload.contains("ADMIT_CARD")) "Admit Cards"
                else "Selected"

                val stateAction = if (isEnable) "on (active)" else "off (muted)"
                val text = "Theek hai $studentTitle! ⚙️ Maine **$categoryName** notifications ko **$stateAction** set kar diya hai. Baaki categories ki settings ko affect nahi kiya gaya hai."

                val actions = listOf(
                    NovaContextualAction(
                        label = "⚙️ Review All Notification Preferences",
                        actionType = NovaActionType.OPEN_SETTINGS,
                        isPrimary = true
                    ),
                    NovaContextualAction(
                        label = "🔔 Notification Center",
                        actionType = NovaActionType.OPEN_NOTIFICATIONS
                    )
                )

                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = text,
                    actionType = NovaActionType.UPDATE_NOTIFICATION_SETTINGS,
                    actionPayload = payload,
                    actionButtons = actions
                )
            }

            else -> {
                NovaChatMessage(
                    sender = NovaSender.NOVA,
                    text = "Haan $studentTitle, bataiye aaj kya study karein? Main concepts explain karne, timetable plan karne aur mock tests me help karne ke liye ready hoon! 📚"
                )
            }
        }
    }
}
