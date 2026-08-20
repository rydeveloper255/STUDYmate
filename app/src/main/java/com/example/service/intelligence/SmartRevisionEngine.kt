package com.example.service.intelligence

import com.example.data.model.*
import java.util.concurrent.TimeUnit

enum class RevisionPriority(val displayName: String, val level: Int) {
    URGENT("URGENT 🚨", 4),
    HIGH("HIGH ⚡", 3),
    MEDIUM("MEDIUM 📌", 2),
    LOW("LOW 💤", 1)
}

enum class RevisionState(val displayName: String) {
    NOT_DUE("Not Due Yet"),
    DUE("Due Today"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    REPEAT_SOON("Repeat Soon")
}

data class SmartRevisionItem(
    val id: Long = 0,
    val subject: String,
    val topic: String,
    val priority: RevisionPriority,
    val revisionState: RevisionState,
    val masteryScore: Int,
    val reason: String,
    val lastStudiedMillis: Long,
    val lastTestedMillis: Long,
    val recommendedDurationMins: Int,
    val relevantMistakeCount: Int,
    val intervalDays: Int,
    val recommendedReviewDateMillis: Long,
    val recommendedLadderStep: String = "Standard Practice"
)

/**
 * Smart Spaced Revision & Queue Engine.
 * Implements adaptive spaced learning, error-weighted scheduling, and revision ladders.
 */
object SmartRevisionEngine {

    /**
     * Builds the prioritized revision queue using real mastery, mistakes, and exam date.
     */
    fun buildRevisionQueue(
        topicMasteries: List<TopicMastery>,
        mistakes: List<MistakeItem>,
        mockAttempts: List<MockTestAttempt>,
        examDateMillis: Long
    ): List<SmartRevisionItem> {
        val now = System.currentTimeMillis()
        val daysUntilExam = TimeUnit.MILLISECONDS.toDays(examDateMillis - now).coerceAtLeast(1)

        return topicMasteries.mapIndexed { idx, m ->
            val unmasteredMistakes = mistakes.count { it.topic.equals(m.topic, ignoreCase = true) && !it.isMastered }
            val daysSinceStudied = TimeUnit.MILLISECONDS.toDays(now - m.lastStudiedMillis).coerceAtLeast(0)
            val daysSinceTested = TimeUnit.MILLISECONDS.toDays(now - m.lastTestedMillis).coerceAtLeast(0)

            // Priority Score Calculation
            var priorityScore = 0
            val reasons = mutableListOf<String>()

            if (m.masteryScore < 50) {
                priorityScore += 35
                reasons.add("Low mastery (${m.masteryScore}%)")
            } else if (m.masteryScore < 65) {
                priorityScore += 20
                reasons.add("Sub-optimal mastery (${m.masteryScore}%)")
            }

            if (unmasteredMistakes >= 2) {
                priorityScore += 30
                reasons.add("$unmasteredMistakes repeated mistakes")
            } else if (unmasteredMistakes == 1) {
                priorityScore += 15
                reasons.add("1 active mistake logged")
            }

            if (now >= m.recommendedReviewDateMillis && m.recommendedReviewDateMillis > 0) {
                priorityScore += 25
                reasons.add("Reached forgetting curve threshold")
            }

            if (daysUntilExam in 1..30) {
                priorityScore += 15
            }

            val priority = when {
                priorityScore >= 55 -> RevisionPriority.URGENT
                priorityScore >= 35 -> RevisionPriority.HIGH
                priorityScore >= 20 -> RevisionPriority.MEDIUM
                else -> RevisionPriority.LOW
            }

            val state = when {
                m.masteryScore >= 85 && unmasteredMistakes == 0 && now < m.recommendedReviewDateMillis -> RevisionState.COMPLETED
                now > m.recommendedReviewDateMillis + TimeUnit.DAYS.toMillis(3) && m.recommendedReviewDateMillis > 0 -> RevisionState.OVERDUE
                now >= m.recommendedReviewDateMillis -> RevisionState.DUE
                m.masteryScore < 50 -> RevisionState.REPEAT_SOON
                else -> RevisionState.NOT_DUE
            }

            val duration = when (priority) {
                RevisionPriority.URGENT -> 25
                RevisionPriority.HIGH -> 20
                RevisionPriority.MEDIUM -> 15
                RevisionPriority.LOW -> 10
            }

            val ladderStep = getSmartReviewLadder(m.masteryScore, unmasteredMistakes)

            val computedIntervalDays = TimeUnit.MILLISECONDS.toDays(
                (m.recommendedReviewDateMillis - m.lastStudiedMillis).coerceAtLeast(TimeUnit.DAYS.toMillis(1))
            ).toInt().coerceIn(1, 30)

            SmartRevisionItem(
                id = m.id,
                subject = m.subject,
                topic = m.topic,
                priority = priority,
                revisionState = state,
                masteryScore = m.masteryScore,
                reason = if (reasons.isNotEmpty()) reasons.joinToString(" • ") else "Scheduled review",
                lastStudiedMillis = m.lastStudiedMillis,
                lastTestedMillis = m.lastTestedMillis,
                recommendedDurationMins = duration,
                relevantMistakeCount = unmasteredMistakes,
                intervalDays = computedIntervalDays,
                recommendedReviewDateMillis = m.recommendedReviewDateMillis,
                recommendedLadderStep = ladderStep
            )
        }.sortedWith(compareByDescending<SmartRevisionItem> { it.priority.level }.thenBy { it.recommendedReviewDateMillis })
    }

    /**
     * Updates topic's spaced repetition schedule following a practice or revision session.
     */
    fun updateScheduleAfterSession(
        currentMastery: TopicMastery,
        sessionAccuracyPercent: Float
    ): TopicMastery {
        val now = System.currentTimeMillis()
        val currentInterval = TimeUnit.MILLISECONDS.toDays(
            (currentMastery.recommendedReviewDateMillis - currentMastery.lastStudiedMillis).coerceAtLeast(TimeUnit.DAYS.toMillis(1))
        ).toInt().coerceIn(1, 30)

        val newIntervalDays = when {
            sessionAccuracyPercent >= 80f -> (currentInterval * 2).coerceAtMost(30) // Double spacing interval
            sessionAccuracyPercent >= 60f -> (currentInterval * 1.5).toInt().coerceAtMost(21)
            else -> 1 // Reset interval on failure
        }

        val newReviewDateMillis = now + TimeUnit.DAYS.toMillis(newIntervalDays.toLong())
        val updatedScore = ((currentMastery.masteryScore * 0.6f) + (sessionAccuracyPercent * 0.4f)).toInt().coerceIn(0, 100)

        val newState = when {
            updatedScore >= 80 -> "MASTERED"
            updatedScore < 55 -> "WEAK"
            else -> "INTERMEDIATE"
        }

        return currentMastery.copy(
            masteryScore = updatedScore,
            masteryState = newState,
            recommendedReviewDateMillis = newReviewDateMillis,
            lastTestedMillis = now,
            lastStudiedMillis = now
        )
    }

    /**
     * Returns progressive learning ladder step when user struggles with a topic.
     */
    fun getSmartReviewLadder(masteryScore: Int, mistakeCount: Int): String {
        return when {
            masteryScore < 40 || mistakeCount >= 3 -> "Step 1: Concept Review & Formula Sheet"
            masteryScore < 60 -> "Step 2: Simple Guided Practice (5 questions)"
            masteryScore < 75 -> "Step 3: Standard Practice & Speed Drills"
            else -> "Step 4: Full Timed Test Mode"
        }
    }
}
