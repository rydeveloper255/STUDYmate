package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "current_user",
    val uid: String = "",
    val name: String = "Student",
    val email: String = "",
    val photoUrl: String? = null,
    val grade: String = "Class 12",
    val educationLevel: String = "Senior Secondary (11th-12th)",
    val languagePreference: String = "English",
    
    // Step 2 - Exam Selection & Goals
    val examCategory: String = "Competitive / Entrance",
    val examName: String = "JEE / NEET / Board Exam",
    val examDateMillis: Long = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000, // 60 days default
    val targetScore: String = "Top 500 AIR / 99%ile",
    val goal: String = "Competitive Exam",
    
    // Step 3 - Subjects & Preparation
    val subjects: List<String> = listOf("Mathematics", "Physics", "Chemistry"),
    val highPrioritySubjects: List<String> = listOf("Physics", "Mathematics"),
    val mediumPrioritySubjects: List<String> = listOf("Chemistry"),
    val lowPrioritySubjects: List<String> = emptyList(),
    val strongSubjects: List<String> = listOf("Physics"),
    val preparationLevel: String = "Intermediate (Practicing questions & concepts)",
    
    // Step 4 - Study Schedule, Times & Breaks
    val dailyTargetMinutes: Int = 180, // 3 hours
    val availableStudyHours: Float = 4.0f,
    val preferredStudyStartTime: String = "06:00 PM",
    val preferredStudyEndTime: String = "10:00 PM",
    val preferredStudyDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    val breakDurationMinutes: Int = 15,
    val preferredStudyTime: String = "Evening",
    val morningNightPreference: String = "Balanced",
    val revisionFrequency: String = "Spaced Repetition",
    val mockTestFrequency: String = "Weekly",
    
    // Step 5 - Weak Areas & Daily Goals
    val weakSubjects: List<String> = listOf("Chemistry"),
    val weakTopics: List<String> = listOf("Organic Reaction Mechanisms", "Rotational Dynamics"),
    val dailyStudyGoal: String = "Complete daily scheduled topics & 20 active recall flashcards",
    val shortTermGoal: String = "Master high-yield concepts and achieve 85%+ mock accuracy",
    val longTermGoal: String = "Achieve top rank in target exam and secure admission",
    
    val notificationsEnabled: Boolean = true,
    val isGuest: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val xp: Int = 350,
    val level: Int = 2,
    val streakDays: Int = 4,
    val totalFocusMinutes: Int = 135,
    val totalQuestionsSolved: Int = 48,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_plan_items")
data class StudyPlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val chapter: String,
    val topic: String,
    val targetMinutes: Int,
    val isCompleted: Boolean = false,
    val scheduledDateMillis: Long = System.currentTimeMillis(),
    val priority: PlanPriority = PlanPriority.HIGH,
    val notes: String = ""
)

enum class PlanPriority {
    HIGH, MEDIUM, LOW
}

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topic: String,
    val durationMinutes: Int,
    val actualMinutesSpent: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val xpEarned: Int = 50,
    val isCompleted: Boolean = true
)

enum class QuestionSource {
    PREVIOUS_YEAR,
    PRACTICE,
    AI_GENERATED
}

enum class QuestionSourceFilter {
    BALANCED_MIX,
    PREVIOUS_YEAR_ONLY,
    AI_GENERATED_ONLY,
    PRACTICE_ONLY
}

data class MockTestConfig(
    val exam: String = "JEE Main & Advanced",
    val subject: String = "Physics",
    val topic: String = "All Topics",
    val difficulty: String = "Medium",
    val questionCount: Int = 10,
    val timeLimitMinutes: Int = 15,
    val sourceFilter: QuestionSourceFilter = QuestionSourceFilter.BALANCED_MIX,
    val customMaterialId: Long? = null
)

@Entity(tableName = "user_question_materials")
data class UserQuestionMaterial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val exam: String = "General",
    val subject: String = "Physics",
    val topic: String = "General",
    val rawText: String = "",
    val questionCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_test_attempts")
data class MockTestAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val score: Int,
    val totalQuestions: Int,
    val accuracyPercent: Float,
    val timeSpentSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val weakTopics: List<String> = emptyList(),
    val strongTopics: List<String> = emptyList(),
    val aiRecommendation: String = "",
    val examName: String = "JEE / NEET / Board Exam",
    val topic: String = "All Topics",
    val difficulty: String = "Medium",
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val skippedCount: Int = 0,
    val avgTimePerQuestionSeconds: Float = 0f,
    val markingScheme: String = "+4 / -1 (Standard)",
    val totalTimeAllowedSeconds: Int = 600
)

data class Question(
    val id: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val subject: String,
    val topic: String,
    val difficulty: String = "Medium",
    val source: QuestionSource = QuestionSource.AI_GENERATED,
    val sourceLabel: String = "AI Practice",
    val yearOrTag: String = ""
)

data class QuestionAttemptDetail(
    val question: Question,
    val selectedIndex: Int? = null,
    val isCorrect: Boolean = false,
    val isMarkedForReview: Boolean = false,
    val timeSpentSeconds: Int = 0
)

@Entity(tableName = "mistakes")
data class MistakeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionText: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val subject: String,
    val topic: String,
    val mistakeCategory: String = "Conceptual",
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMastered: Boolean = false
)

@Entity(tableName = "flashcards")
data class FlashcardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topic: String,
    val front: String,
    val back: String,
    val hint: String = "",
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val status: RevisionCategory = RevisionCategory.PRACTICE_SOON,
    val confidence: Int = 3, // 1 to 5
    val reviewCount: Int = 0,
    val lastReviewed: Long = System.currentTimeMillis(),
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val sourceDocTitle: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class RevisionCategory {
    REVISE_NOW, PRACTICE_SOON, STRONG
}

data class DailyMission(
    val id: String,
    val title: String,
    val target: Int,
    val current: Int,
    val unit: String,
    val xpReward: Int,
    val isCompleted: Boolean = false
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val progress: Int,
    val maxProgress: Int,
    val xpReward: Int
)

data class NotificationPreference(
    val masterEnabled: Boolean = true,
    val studyReminders: Boolean = true,
    val examCountdownAlerts: Boolean = true,
    val dailyGoalReminders: Boolean = true,
    val missedStudyReminders: Boolean = true,
    val breakReminders: Boolean = true,
    val focusStartedAlerts: Boolean = true,
    val focusCompletedAlerts: Boolean = true,
    val motivationalQuotes: Boolean = true,
    val weeklyReport: Boolean = true,
    val streakAlerts: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val dailyGoalHour: Int = 20,
    val dailyGoalMinute: Int = 30,
    val motivationHour: Int = 8,
    val motivationMinute: Int = 0,
    val activeDays: Set<String> = setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val motivationFrequency: String = "Once Daily (Morning)",
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 7,
    val quietEndMinute: Int = 0,
    val focusReminders: Boolean = true
)

data class DocumentAnalysisResult(
    val fileName: String = "",
    val fileSize: String = "",
    val charCount: Int = 0,
    val summaryBullets: List<String> = emptyList(),
    val keyTerms: List<String> = emptyList(),
    val studyQuestions: List<StudyQuestion> = emptyList()
)

data class StudyQuestion(
    val question: String,
    val answer: String,
    val type: String = "Conceptual" // Conceptual, Key Formula, Application, Short Answer
)

// --- 1. AI Study Coach Model ---
data class AiCoachRecommendation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "AI Study Coach",
    val message: String,
    val whyThisExplanation: String,
    val subject: String = "Physics",
    val topic: String = "Current Electricity",
    val recommendedMinutes: Int = 25,
    val questionCount: Int = 10,
    val tag: String = "High Yield"
)

// --- 2. What Should I Study Now Model ---
data class StudyNowRecommendation(
    val subject: String,
    val topic: String,
    val targetMinutes: Int = 30,
    val reasoning: String,
    val actionType: String = "Focus Session", // Focus Session, Spaced Recall, Targeted Practice
    val urgencyLabel: String = "High Priority"
)

// --- 3. Adaptive Quiz Models ---
data class AdaptiveQuizState(
    val subject: String = "Physics",
    val topic: String = "Current Electricity",
    val currentDifficulty: String = "Easy", // Easy -> Medium -> Hard
    val stage: Int = 1,
    val consecutiveCorrect: Int = 0,
    val consecutiveWrong: Int = 0,
    val isRemediationActive: Boolean = false,
    val conceptExplanation: String = "",
    val totalSolved: Int = 0,
    val totalCorrect: Int = 0,
    val topicMasteryMap: Map<String, Int> = emptyMap()
)

data class TopicMasteryRecord(
    val topic: String,
    val subject: String,
    val masteryScore: Int, // 0 to 100
    val accuracy: Float,
    val easySolved: Int,
    val medSolved: Int,
    val hardSolved: Int,
    val lastPracticed: Long = System.currentTimeMillis()
)

// --- 4. Notes -> Complete Study Kit Model ---
data class CompleteStudyKit(
    val kitTitle: String,
    val subject: String,
    val sourceSummary: String,
    val importantConcepts: List<String> = emptyList(),
    val flashcards: List<FlashcardItem> = emptyList(),
    val mcqs: List<Question> = emptyList(),
    val shortAnswerQuestions: List<StudyQuestion> = emptyList(),
    val practiceQuestions: List<StudyQuestion> = emptyList(),
    val revisionChecklist: List<String> = emptyList(),
    val quickRevisionSheet: String = "",
    val sourceDocTitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// --- 5. Smart Revision Radar Model ---
data class RevisionRadarTopic(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subject: String,
    val topic: String,
    val category: RevisionCategory, // REVISE_NOW (Red), PRACTICE_SOON (Yellow), STRONG (Green)
    val urgencyText: String,
    val lastReviewedDaysAgo: Int,
    val accuracyPercentage: Int,
    val mistakeCount: Int,
    val recommendedAction: String
)

// --- 6. AI Mistake Intelligence Model ---
data class MistakePatternInsight(
    val patternType: String, // "Conceptual Misunderstanding", "Calculation Mistake", "Sign Error", "Formula Confusion", "Question Interpretation", "Careless Mistake"
    val iconName: String,
    val title: String,
    val description: String,
    val affectedSubject: String,
    val frequency: Int,
    val targetedAdvice: String,
    val targetedPracticeQuestions: List<Question> = emptyList()
)

// --- 7. Weekly AI Report Model ---
data class WeeklyProgressReport(
    val weekRange: String = "Past 7 Days",
    val totalFocusMinutes: Int,
    val sessionsCompleted: Int,
    val questionsSolved: Int,
    val averageAccuracy: Float,
    val strongestSubject: String,
    val weakestTopic: String,
    val currentStreak: Int,
    val consistencyDeltaPercent: Int = 18,
    val nextWeekAdvice: String,
    val subjectTimeMap: Map<String, Int> = emptyMap()
)

// --- 19. Focus Analytics Summary Model ---
data class FocusAnalyticsSummary(
    val totalSessions: Int = 6,
    val totalFocusedMinutes: Int = 265, // 4h 25m
    val completedSessions: Int = 5,
    val interruptedSessions: Int = 1,
    val currentStreak: Int = 4,
    val completionRatePercent: Int = 83
)

// --- AI Tutor Upgraded Models ---
enum class TutorActionType(val displayName: String, val icon: String, val description: String) {
    GENERAL_CHAT("Ask a Question", "❓", "Ask anything about concepts, homework or doubts"),
    EXPLAIN_CONCEPT("Explain Concept", "📖", "Deep step-by-step conceptual breakdown with formulas"),
    SIMPLIFY_EXPLANATION("Simplify Explanation", "🐣", "Intuitive ELI5 explanation with real-world analogies"),
    GIVE_EXAMPLES("Give Examples", "💡", "Worked-out numerical & practical application examples"),
    PRACTICE_QUESTIONS("Practice Questions", "✍️", "Generate high-yield MCQs with explanations"),
    GENERATE_FLASHCARDS("Generate Flashcards", "🗂️", "Create active recall spaced-repetition flashcards"),
    SUMMARIZE_MATERIAL("Summarize Notes", "📄", "Extract key points, formulas & summary bullets"),
    REVISION_PLAN("Revision Plan", "🔄", "Create a spaced revision schedule for weak areas"),
    IDENTIFY_WEAK_AREAS("Identify Weak Areas", "🎯", "Diagnose test mistakes & recommend remediation"),
    DAILY_STUDY_PLAN("Daily Study Plan", "📅", "Generate optimal daily time-blocked schedule")
}

data class TutorStudentContext(
    val studentName: String = "Student",
    val grade: String = "Class 12",
    val targetExam: String = "JEE / NEET / Board Exam",
    val examDaysRemaining: Int = 30,
    val selectedSubject: String = "Physics",
    val selectedTopic: String = "Current Electricity",
    val weakTopics: List<String> = emptyList(),
    val recentMistakes: List<String> = emptyList(),
    val dailyTargetMinutes: Int = 180,
    val totalFocusMinutes: Int = 135,
    val streakDays: Int = 4,
    val learningStyle: String = "Step-by-step with practical examples"
)

data class TutorResponseResult(
    val replyMarkdown: String,
    val actionType: TutorActionType = TutorActionType.GENERAL_CHAT,
    val generatedFlashcards: List<FlashcardItem>? = null,
    val generatedPlanItems: List<StudyPlanItem>? = null,
    val generatedQuestions: List<Question>? = null,
    val isOfflineFallback: Boolean = false
)


