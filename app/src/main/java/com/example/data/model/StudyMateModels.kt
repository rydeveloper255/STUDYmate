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
    val appLanguage: String = "English",
    val contentLanguage: String = "Hindi",
    
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
    val subjectTimeAllocationJson: String = "{}",
    val preferredStudyStartTime: String = "06:00 PM",
    val preferredStudyEndTime: String = "10:00 PM",
    val preferredStudyDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    val breakDurationMinutes: Int = 15,
    val preferredSessionDurationMinutes: Int = 25,
    val customStudyBlocksJson: String = "[]",
    val mockTestLanguage: String = "English",
    val defaultMockTestQuestionCount: Int = 25,
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

data class StudyTimeBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startTime: String = "06:30 AM",
    val subject: String = "Mathematics",
    val durationMinutes: Int = 60,
    val topic: String = ""
)

@Entity(tableName = "study_plan_items")
data class StudyPlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "current_user",
    val examId: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val topicId: String = "",
    val subject: String,
    val chapter: String,
    val topic: String,
    val targetMinutes: Int,
    val actualMinutesSpent: Int = 0,
    val isCompleted: Boolean = false,
    val scheduledDateMillis: Long = System.currentTimeMillis(),
    val startTimeFormatted: String = "",
    val endTimeFormatted: String = "",
    val sessionType: String = "PRACTICE", // LEARNING, PRACTICE, REVISION, MOCK_TEST, WEAK_TOPIC, REVIEW, BREAK
    val sessionState: String = "PLANNED", // PLANNED, STARTED, PAUSED, COMPLETED, SKIPPED, MISSED, CANCELLED
    val priority: PlanPriority = PlanPriority.HIGH,
    val aiRecommendationReason: String = "",
    val isAiGenerated: Boolean = false,
    val notes: String = "",
    val completedTimestamp: Long = 0L
)

enum class PlanPriority {
    HIGH, MEDIUM, LOW
}

@Entity(tableName = "user_study_preferences")
data class UserStudyPreferences(
    @PrimaryKey val userId: String = "current_user",
    val examId: String = "",
    val dailyAvailableMinutes: Int = 180,
    val preferredStudyWindow: String = "MORNING_EVENING", // MORNING, AFTERNOON, EVENING, NIGHT, CUSTOM
    val windowStartHour: Int = 8,
    val windowEndHour: Int = 22,
    val preferredSessionMinutes: Int = 30,
    val breakMinutes: Int = 5,
    val breakFrequencyMinutes: Int = 25,
    val subjectPrioritiesJson: String = "{}", // e.g. {"Physics":"HIGH","Mathematics":"MEDIUM"}
    val topicPrioritiesJson: String = "{}",
    val personalizationEnabled: Boolean = true,
    val dailyQuestionGoal: Int = 30,
    val dailyStudyMinutesGoal: Int = 45,
    val weeklyTestsGoal: Int = 3,
    val studyTimeAvailableOption: String = "30 min",
    val caRemindersEnabled: Boolean = true,
    val revisionRemindersEnabled: Boolean = true,
    val studyRemindersEnabled: Boolean = true,
    val testRemindersEnabled: Boolean = true,
    val goalRemindersEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class GeneratedPlanResult(
    val sessions: List<StudyPlanItem>,
    val timeBudgetMinutes: Int,
    val totalScheduledMinutes: Int,
    val deadlineWarningMessage: String? = null,
    val summaryAdvice: String = ""
)

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

enum class QuestionSource(val displayName: String, val badgeIcon: String) {
    PREVIOUS_YEAR("Verified Previous-Year Question", "🏷️"),
    VERIFIED_PREVIOUS_YEAR("Verified Previous-Year Question", "🏷️"),
    CHAPTER_PRACTICE("AI Practice Question", "✨"),
    EXAM_PATTERN("Official Pattern Question", "📐"),
    CURRENT_AFFAIRS("Current Affairs Question", "📰"),
    USER_PROVIDED("User-Provided Question", "👤"),
    AI_GENERATED("AI-Generated Practice Question", "✨"),
    PRACTICE("Practice Bank Question", "📚"),
    MIXED("Mixed Question", "🔀")
}

enum class QuestionSourceType(val displayName: String, val badge: String, val description: String) {
    PYQ("Previous-Year Questions (PYQs)", "🏷️", "Authentic previous year exam questions with verified year & shift"),
    CHAPTER_PRACTICE("Chapter Practice", "✨", "Syllabus-grounded practice questions for selected subject/chapter"),
    EXAM_PATTERN("Official Exam Pattern Mock", "📐", "Full exam blueprint simulation with realistic subject distribution"),
    CURRENT_AFFAIRS("Current Affairs Practice", "📰", "Questions generated strictly from verified recent Current Affairs"),
    AI_PRACTICE("AI-Generated Practice", "🤖", "Clearly labeled AI-generated practice questions"),
    MIXED("Balanced Mixed Practice", "🔀", "Curated combination of verified PYQs and AI practice questions")
}

enum class QuestionCbtState(val displayName: String, val labelHindi: String) {
    NOT_VISITED("Not Visited", "देखा नहीं"),
    NOT_ANSWERED("Not Answered", "उत्तर नहीं दिया"),
    ANSWERED("Answered", "उत्तर दिया"),
    MARKED_FOR_REVIEW("Marked for Review", "समीक्षा हेतु चिह्नित"),
    ANSWERED_AND_MARKED("Answered & Marked for Review", "उत्तर दिया और समीक्षा हेतु")
}

data class TestGenerationError(
    val stage: String,
    val userMessage: String,
    val technicalDetails: String = "",
    val canRetry: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class InsufficientPyqNotice(
    val availableCount: Int,
    val requestedCount: Int,
    val examName: String,
    val subject: String,
    val availableQuestions: List<Question>
)

data class TimeAnalysisResult(
    val totalTimeSpentSeconds: Int,
    val avgTimePerQuestionSeconds: Float,
    val fastestQuestionIndex: Int,
    val fastestTimeSeconds: Int,
    val longestQuestionIndex: Int,
    val longestTimeSeconds: Int
)

enum class QuestionSourceFilter {
    BALANCED_MIX,
    PREVIOUS_YEAR_ONLY,
    AI_GENERATED_ONLY,
    PRACTICE_ONLY
}

enum class MockTestType(val displayName: String, val description: String, val iconName: String) {
    PYQ("PYQ Practice", "Only verified previous-year questions from official exams", "History"),
    SUBJECT_PRACTICE("Subject Practice", "Deep dive into a specific subject of the selected exam", "MenuBook"),
    CHAPTER_PRACTICE("Chapter Practice", "Master specific chapters or topics from your syllabus", "AutoStories"),
    SMART_PRACTICE("Smart Practice", "Performance-driven practice targeting weak areas and revision", "Psychology"),
    AI_PRACTICE("AI Practice", "Clearly labeled AI-generated practice questions", "AutoAwesome"),
    MOCK_TEST("Mock Test", "Full official exam simulation following pattern and timing", "Assignment"),
    MIXED_PRACTICE("Mixed Practice", "Combines verified PYQs, curated, and AI practice questions", "Tune"),
    FULL_MOCK("Full Mock Test", "Comprehensive multi-subject test following official exam pattern", "Assignment"),
    SUBJECT_TEST("Subject Test", "Deep dive into a specific subject of the selected exam", "MenuBook"),
    CHAPTER_TEST("Chapter Test", "Master specific chapters from your syllabus", "AutoStories"),
    TOPIC_TEST("Topic Test", "Focused mastery on a specific chapter and topic", "Topic"),
    WEAK_AREAS("Weak Areas Test", "Targeted practice on topics where accuracy is lowest", "ReportProblem"),
    REVISION_TEST("Revision Test", "Reinforce due topics and spaced learning queue", "Psychology"),
    PREVIOUS_MISTAKES("Previous Mistakes Test", "Practice questions derived from past incorrect answers", "HistoryEdu"),
    ADAPTIVE_PRACTICE("Adaptive Practice", "Dynamic difficulty adjusts live based on your accuracy", "Tune"),
    CUSTOM_TEST("Custom Test", "Custom question counts, duration, and subject selection", "Build"),
    TIMED_TEST("Timed Speed Test", "High-speed timed practice test for speed building", "Timer")
}

data class MockTestConfig(
    val examId: String = "default_exam",
    val exam: String = "RRB NTPC (Railway)",
    val testType: MockTestType = MockTestType.FULL_MOCK,
    val questionSource: QuestionSourceType = QuestionSourceType.MIXED,
    val subject: String = "All Subjects",
    val chapter: String = "All Chapters",
    val topic: String = "All Topics",
    val pyqYear: String = "All Available Years",
    val pyqShift: String = "All Shifts",
    val difficulty: String = "Medium",
    val language: String = "English",
    val questionCount: Int = 25,
    val timeLimitMinutes: Int = 30,
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
    val totalTimeAllowedSeconds: Int = 600,
    val examId: String = "default_exam",
    val language: String = "English",
    val attemptState: String = "SUBMITTED",
    val rawScoreEarned: Float = 0f
)

data class Question(
    val id: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val subject: String,
    val topic: String,
    val chapter: String = "",
    val examName: String = "",
    val year: String = "",
    val shift: String = "",
    val sourceReference: String = "",
    val difficulty: String = "Medium",
    val source: QuestionSource = QuestionSource.AI_GENERATED,
    val sourceLabel: String = "AI Practice",
    val yearOrTag: String = "",
    val examId: String = "",
    val chapterId: String = "",
    val language: String = "English",
    val questionType: String = "MCQ",
    val generationModel: String = "",
    val generationTimestamp: Long = 0L,
    val tags: List<String> = emptyList(),
    val status: String = "ACTIVE"
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
    val userId: String = "current_user",
    val examId: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val topicId: String = "",
    val chapter: String = "",
    val questionText: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val subject: String,
    val topic: String,
    val mistakeCategory: String = "Conceptual", // Conceptual, Calculation, Careless, Time Pressure, Guess, Unknown
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMastered: Boolean = false,
    val aiClassified: Boolean = false
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

enum class NotificationCategory(val displayName: String, val iconName: String) {
    STUDY("Study", "menu_book"),
    TESTS("Tests", "quiz"),
    CURRENT_AFFAIRS("Current Affairs", "newspaper"),
    EXAM_UPDATES("Exam Updates", "verified"),
    NOVA("NOVA", "auto_awesome"),
    SYSTEM("System", "info")
}

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: NotificationCategory = NotificationCategory.STUDY,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionText: String = "Open",
    val deepLink: String = "HOME", // CURRENT_AFFAIRS, MOCK_TEST, REVISION, EXAM_UPDATES, FOCUS, DAILY_BRIEFING, NOVA
    val payload: String = "",
    val eventKey: String? = null,
    val expiresAt: Long? = null
)

data class DailyBriefingData(
    val dateString: String = "",
    val greeting: String = "Good Morning 👋",
    val focusTopic: String? = null,
    val focusSubject: String? = null,
    val focusReason: String? = null,
    val currentAffairsHeadline: String? = null,
    val currentAffairsCount: Int = 0,
    val practiceSuggestion: String? = null,
    val practiceQuestionCount: Int = 10,
    val examDaysRemaining: Int? = null,
    val examName: String? = null,
    val unfinishedTestTitle: String? = null,
    val unfinishedTestProgress: String? = null,
    val revisionQuestionsCount: Int = 0,
    val language: String = "English"
)

data class NotificationPreference(
    val masterEnabled: Boolean = true,
    val studyReminders: Boolean = true,
    val currentAffairsReminders: Boolean = true,
    val testReminders: Boolean = true,
    val examCountdownAlerts: Boolean = true,
    val examUpdatesReminders: Boolean = true,
    val novaReminders: Boolean = true,
    val dailyBriefingEnabled: Boolean = true,
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
    val briefingHour: Int = 7,
    val briefingMinute: Int = 30,
    val activeDays: Set<String> = setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val motivationFrequency: String = "Once Daily (Morning)",
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 7,
    val quietEndMinute: Int = 0,
    val focusReminders: Boolean = true,
    val language: String = "English"
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

// =========================================================================
// NOVA PERSONAL AI ASSISTANT MODELS
// =========================================================================

enum class NovaMemoryCategory(val displayName: String, val iconName: String) {
    ACADEMIC("Academic & Exam", "School"),
    WEAK_AREAS("Weak Areas & Topics", "Warning"),
    STUDY_PREFERENCES("Study Habits & Times", "Schedule"),
    GOALS("Targets & Aspirations", "Flag"),
    USER_NOTES("Personal Notes & Memory", "Note"),
    CONVERSATION("Preferences & Interactions", "Chat")
}

@Entity(tableName = "nova_memory")
data class NovaMemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: NovaMemoryCategory = NovaMemoryCategory.ACADEMIC,
    val key: String,
    val value: String,
    val source: String = "User Profile", // "Onboarding", "Chat Conversation", "Focus Session", "User Added"
    val timestamp: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)

@Entity(tableName = "nova_reminders")
data class NovaReminderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "General",
    val topic: String = "",
    val timeMillis: Long,
    val timeFormatted: String = "",
    val isCompleted: Boolean = false,
    val isSnoozed: Boolean = false,
    val reminderType: String = "Study Session", // "Study Session", "Mock Test", "Revision", "Custom"
    val createdAt: Long = System.currentTimeMillis()
)

enum class NovaSender {
    USER, NOVA
}

enum class NovaActionType {
    NONE,
    START_FOCUS,
    STOP_FOCUS,
    CREATE_PLAN,
    START_QUIZ,
    CREATE_REMINDER,
    OPEN_MEMORY,
    OPEN_SETTINGS,
    OPEN_APP_BLOCKING,
    OPEN_DOCUMENT_SUMMARIZER,
    RECOVER_MISSED_SESSION,
    SHOW_DAILY_BRIEF,
    SHOW_DAILY_REVIEW,
    // Step 6 Required Nova Actions
    CREATE_STUDY_TASK,
    UPDATE_STUDY_TASK,
    ADD_REVISION_ITEM,
    START_STUDY_SESSION,
    OPEN_MOCK_TEST,
    OPEN_SUBJECT,
    OPEN_TOPIC,
    OPEN_STUDY_PLAN,
    OPEN_FOCUS_MODE,
    SHOW_PROGRESS,
    SHOW_TEST_RESULT,
    // Step 18 & 20 Human & Action-Oriented Nova Actions
    OPEN_CURRENT_AFFAIRS,
    OPEN_SMART_NOTES,
    OPEN_SMART_SEARCH,
    OPEN_SAVED_QUESTIONS,
    EXPORT_CURRENT_AFFAIRS_PDF,
    EXPORT_ANSWER_PDF,
    SAVE_NOTE,
    OPEN_FULL_NOVA,
    OPEN_ANALYTICS,
    // Step 21 Live Exam Intelligence Actions
    OPEN_LIVE_EXAM_INTELLIGENCE,
    OPEN_EXAM_RADAR,
    OPEN_OFFICIAL_NOTICE,
    OPEN_TRENDING_TOPICS,
    SAVE_EXAM_UPDATE,
    // Step 22 Nova Web Intelligence & Smart Search Actions
    EXPLAIN_NEWS,
    VERIFY_FACT,
    WHY_STUDY_THIS,
    OPEN_WEB_SOURCE,
    SEARCH_WEB,
    SAVE_WEB_CONTENT,
    MAKE_QUIZ_FOR_TOPIC,
    ADD_TOPIC_TO_REVISION,
    LEARN_TOPIC,
    // Step 23 Smart Learning System Actions
    GENERATE_FRESH_MCQ,
    START_SMART_REVISION,
    SHOW_DAILY_EXAM_BRIEF,
    VERIFY_SOURCE_TRUST,
    // Step 33 Nova 2.0 Controlled Action System
    OPEN_HOME,
    OPEN_CURRENT_AFFAIRS_QUIZ,
    OPEN_CHAPTER,
    START_PRACTICE,
    START_SMART_PRACTICE,
    START_PYQ,
    RESUME_TEST,
    OPEN_REVISION,
    OPEN_MISTAKES,
    OPEN_BOOKMARKS,
    OPEN_RESULTS,
    OPEN_PROFILE,
    OPEN_NOTIFICATIONS,
    CONFIRM_HIGH_RISK_ACTION,
    // Step 40 Smart Vacancy, Results & Admit Card Intelligence Actions
    OPEN_VACANCIES,
    OPEN_VACANCY_DETAILS,
    OPEN_RECRUITMENT_DETAIL,
    OPEN_RESULTS_HUB,
    OPEN_ADMIT_CARDS,
    OPEN_SAVED_JOBS,
    OPEN_RECRUITMENT_NOTICES,
    SET_DEADLINE_REMINDER
}

enum class NovaSearchIntent {
    STUDY,          // Educational explanation (direct explanation, no web search needed unless requested)
    CURRENT,        // Latest / current information (needs Serper search)
    SOURCE,         // User wants references / sources
    VERIFY,         // User wants fact verification (needs multi-source comparison)
    EXAM,           // Exam-specific relevance / notifications
    DISCOVERY,      // User wants resources / websites
    NAVIGATION,     // User wants to open app section
    WHY_STUDY,      // User asks why they should study this
    EXPLAIN_NEWS,   // User asks to explain a news / event
    FRESH_MCQ,      // Step 23: User asks to generate fresh practice questions from web / news
    SMART_REVISION, // Step 23: User asks to revise saved / recent / weak topics
    DAILY_BRIEF,    // Step 23: User asks for today's exam briefing
    TRUST_SOURCE    // Step 23: User asks if a source can be trusted / verified
}

enum class VerificationStatus(val label: String, val badge: String, val icon: String) {
    SUPPORTED("Supported", "✓ Supported", "✓"),
    PARTIALLY_SUPPORTED("Partially Supported", "⚠ Partially Supported", "⚠"),
    UNCLEAR("Unclear", "? Unclear", "?"),
    CONTRADICTED("Contradicted", "✕ Contradicted", "✕")
}

data class VerificationResult(
    val claim: String,
    val status: VerificationStatus,
    val statusSummary: String,
    val explanation: String,
    val sources: List<WebSearchSource> = emptyList(),
    val sourcesDisagree: Boolean = false,
    val disagreementDetails: String? = null
)

data class NewsExplanationResult(
    val title: String,
    val whatHappened: String,
    val whyImportant: String,
    val keyFacts: List<String> = emptyList(),
    val examRelevance: String = "",
    val practiceQuestion: Question? = null,
    val sources: List<WebSearchSource> = emptyList(),
    val sourceUrl: String = "",
    val category: String = "Current Affairs"
)

data class WhyStudyThisResult(
    val topic: String,
    val subject: String = "",
    val targetExam: String = "",
    val priority: String = "HIGH", // "HIGH" (🔴), "MEDIUM" (🟡), "LOW" (🟢)
    val priorityRationale: String = "",
    val examRelevance: String = "",
    val isPersonalized: Boolean = false,
    val personalizationContext: String = "",
    val studyRecommendations: List<String> = emptyList()
)

enum class NovaWebSearchMode(val displayName: String, val icon: String) {
    ALL_WEB("All Web", "🌐"),
    STUDY("Study", "📚"),
    CURRENT_AFFAIRS("Current Affairs", "📰"),
    EXAM("Exam", "🎯"),
    OFFICIAL("Official", "🏛️")
}

data class NovaContextualAction(
    val label: String,
    val iconName: String? = null,
    val actionType: NovaActionType,
    val payload: String? = null,
    val isPrimary: Boolean = false
)

data class NovaChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: NovaSender = NovaSender.NOVA,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: NovaActionType = NovaActionType.NONE,
    val actionPayload: String? = null,
    val isActionCompleted: Boolean = false,
    val attachedImageUri: String? = null,
    val isThinking: Boolean = false,
    val actionButtons: List<NovaContextualAction> = emptyList(),
    val currentAffairsPreview: List<CurrentAffairsItem> = emptyList(),
    val searchStatusText: String? = null,
    val webSources: List<WebSearchSource> = emptyList(),
    val verificationResult: VerificationResult? = null,
    val newsExplanation: NewsExplanationResult? = null,
    val whyStudyResult: WhyStudyThisResult? = null,
    val userFeedback: String? = null, // null, "HELPFUL", "NOT_HELPFUL"
    val sourceCategory: String? = null, // "APP_DATA", "WEB_INFO", "AI_EXPLANATION"
    val isPdfGenerating: Boolean = false,
    val pdfDownloadedUri: String? = null
)

enum class NovaVoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

data class NovaSettings(
    val novaName: String = "NOVA",
    val personality: String = "Friendly & Professional", // "Friendly & Professional", "Concise & Analytical", "Empathetic Mentor"
    val language: String = "Hinglish (Auto)", // "Hinglish (Auto)", "English", "Hindi"
    val useBossGreeting: Boolean = true,
    
    // Voice Controls
    val voiceEnabled: Boolean = true,
    val voiceNotifications: Boolean = false,
    val ttsAutoSpeak: Boolean = false,
    val speechSpeed: Float = 1.0f,
    val voiceVolume: Float = 1.0f,
    val voicePitch: Float = 1.12f,
    val voiceLanguage: String = "Auto (Context-Aware)", // "Auto (Context-Aware)", "Hinglish", "Hindi", "English"
    val selectedVoicePersona: String = "NOVA Neural Voice (Warm & Intelligent)",
    val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM",
    val elevenLabsModelId: String = "eleven_multilingual_v2",
    
    // Smart Coach Controls
    val memoryEnabled: Boolean = true,
    val smartCoachEnabled: Boolean = true,
    val studyRemindersEnabled: Boolean = true,
    val missedSessionRecoveryEnabled: Boolean = true,
    val dailyBriefingEnabled: Boolean = true,
    val dailyBriefingHour: Int = 8,
    val dailyBriefingMinute: Int = 0,
    val dailyReviewEnabled: Boolean = true,
    val dailyReviewHour: Int = 21,
    val dailyReviewMinute: Int = 0,
    val appUsageAwarenessEnabled: Boolean = true,
    val breakCoachEnabled: Boolean = true,
    val motivationalMessagesEnabled: Boolean = true,
    
    // Quiet Hours & Notification Limits
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 7,
    val quietEndMinute: Int = 0,

    // Home Widget Settings (STEP 20)
    val showNovaOnHome: Boolean = true,
    val homeWidgetSize: String = "Compact" // "Compact", "Standard", "Expanded"
)

// =========================================================================
// NOVA UNIVERSAL WIDGET & CONTEXT-AWARE AI MODELS (STEP 20)
// =========================================================================

enum class NovaWidgetSize(val displayName: String) {
    COMPACT("Compact"),
    STANDARD("Standard"),
    EXPANDED("Expanded")
}

enum class HomeWidgetDisplayState {
    COLLAPSED,
    THINKING,
    EXPANDED
}

data class NovaAppContext(
    val screenName: String = "Home",
    val subject: String? = null,
    val topic: String? = null,
    val currentAffairsItem: CurrentAffairsItem? = null,
    val activeTestId: String? = null,
    val isTestActive: Boolean = false,
    val currentQuestionText: String? = null,
    val targetExam: String? = null
)

data class NovaStudyContext(
    val studentName: String = "Scholar",
    val preferredTitle: String = "Boss",
    val targetExam: String = "JEE / NEET / Board Exam",
    val selectedSubject: String = "All Subjects",
    val selectedTopic: String = "All Topics",
    val targetScore: String = "Top 500 AIR",
    val studyGoal: String = "Excellence in Target Exam",
    val examDaysRemaining: Int = 30,
    val examDateMillis: Long = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
    val subjects: List<String> = listOf("Physics", "Mathematics", "Chemistry"),
    val subjectPriorities: List<String> = listOf("Physics", "Mathematics"),
    val weakTopics: List<String> = listOf("Rotational Dynamics", "Organic Reactions"),
    val strongTopics: List<String> = emptyList(),
    val dailyTargetMinutes: Int = 180,
    val todayFocusMinutes: Int = 45,
    val currentStreak: Int = 4,
    val weeklyConsistencyPercent: Int = 85,
    val pendingPlanCount: Int = 2,
    val completedPlanCount: Int = 1,
    val todayTasks: List<String> = emptyList(),
    val pendingTasksSummary: List<String> = emptyList(),
    val revisionsDueCount: Int = 0,
    val revisionsDueTopics: List<String> = emptyList(),
    val recentStudySessionsSummary: List<String> = emptyList(),
    val recentTestResultsSummary: List<String> = emptyList(),
    val recentMockAccuracyPercent: Float = 0f,
    val recentMockScoreSummary: String? = null,
    val missedSessionsCount: Int = 0,
    val nextScheduledSession: String? = "Physics (7:00 PM)",
    val topDistractingAppUsageMins: Int = 0,
    val topDistractingAppName: String? = null,
    val isFocusModeActive: Boolean = false,
    val preferredLanguage: String = "English",
    val preferredStudyDurationMins: Int = 25,
    val memories: List<NovaMemoryItem> = emptyList()
)

data class NovaAssistantResponse(
    val replyMarkdown: String,
    val actionType: NovaActionType = NovaActionType.NONE,
    val actionPayload: String? = null,
    val memoryToSave: NovaMemoryItem? = null,
    val quickSuggestions: List<String> = emptyList()
)

// =========================================================================
// AUDIO RECORDING, VOICE NOTES & LECTURE TRANSCRIBER MODELS
// =========================================================================

enum class VoiceNoteType(val displayName: String, val icon: String) {
    LECTURE("Lecture Recording", "🎓"),
    QUICK_REMINDER("Quick Reminder", "⏰"),
    CONCEPT_DOUBT("Concept / Doubt", "💡"),
    REVISION_NOTE("Audio Revision", "📝")
}

@Entity(tableName = "voice_notes")
data class VoiceNoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val audioFilePath: String,
    val durationMillis: Long = 0L,
    val subject: String = "General",
    val noteType: VoiceNoteType = VoiceNoteType.LECTURE,
    val transcription: String = "",
    val summary: String = "",
    val keyPoints: List<String> = emptyList(),
    val extractedReminders: List<String> = emptyList(),
    val isTranscribing: Boolean = false,
    val isBookmarked: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class VoiceNoteAiAnalysis(
    val title: String,
    val transcription: String,
    val summary: String,
    val keyPoints: List<String> = emptyList(),
    val extractedReminders: List<String> = emptyList(),
    val flashcards: List<VoiceNoteFlashcard> = emptyList()
)

data class VoiceNoteFlashcard(
    val question: String,
    val answer: String
)

// =========================================================================
// SMART SEARCH, SMART NOTES & EXAM INTELLIGENCE MODELS
// =========================================================================

enum class WebSourceType {
    OFFICIAL_GOVERNMENT,
    OFFICIAL_EXAM_BOARD,
    REPUTABLE_EDUCATIONAL,
    GENERAL_WEB
}

data class WebSearchSource(
    val title: String,
    val snippet: String,
    val url: String,
    val domain: String,
    val sourceType: WebSourceType = WebSourceType.GENERAL_WEB,
    val isOfficial: Boolean = false,
    val publishedDate: String = ""
)

data class NovaSearchHistoryItem(
    val query: String,
    val subject: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

data class SmartSearchResult(
    val query: String,
    val studentFriendlyAnswer: String,
    val keyPoints: List<String> = emptyList(),
    val formulasAndDefinitions: List<String> = emptyList(),
    val sources: List<WebSearchSource> = emptyList(),
    val sourcesDisagree: Boolean = false,
    val disagreementDetails: String = "",
    val suggestedQuestions: List<String> = emptyList(),
    val generatedPracticeQuestions: List<Question> = emptyList(),
    val examRelevance: String = "",
    val intentType: String = "Concept Explanation",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "smart_notes")
data class SmartNoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "General",
    val topic: String = "General",
    val contentMarkdown: String,
    val keyPoints: List<String> = emptyList(),
    val formulas: List<String> = emptyList(),
    val importantFacts: List<String> = emptyList(),
    val sourceUrl: String = "",
    val sourceTitle: String = "",
    val isRevised: Boolean = false,
    val isBookmarked: Boolean = false,
    val revisionCategory: RevisionCategory = RevisionCategory.PRACTICE_SOON,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "current_affairs")
data class CurrentAffairsItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String,
    val examRelevance: String,
    val category: String, // "National", "International", "Science & Tech", "Economy", "Environment", "Polity", "Banking", "Defence", etc.
    val targetExams: List<String> = listOf("UPSC", "SSC", "State PSC", "General"),
    val subject: String = "Current Affairs",
    val sourceName: String,
    val sourceUrl: String,
    val publishedDate: String,
    val mcqs: List<Question> = emptyList(),
    val isSavedForRevision: Boolean = false,
    val keyPoints: List<String> = emptyList(),
    val whyItMatters: String = "",
    val isImportant: Boolean = false,
    val language: String = "en",
    val sourcesCount: Int = 1,
    val fetchedDate: String = "",
    val canonicalUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_updates")
data class ExamUpdateItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String,
    val title: String,
    val noticeType: String, // "Official Notice", "Admit Card", "Exam Date", "Syllabus Update", "Results"
    val summary: String,
    val officialLink: String,
    val publishDate: String,
    val isVerifiedOfficial: Boolean = true,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Next Best Action & Goal Radar Engine Models ---

enum class NextBestActionType(val icon: String, val label: String) {
    FOCUS_SESSION("🎯", "Focus Session"),
    SPACED_REVISION("🔄", "Spaced Recall"),
    MISTAKE_REMEDIATION("📝", "Review Mistakes"),
    MOCK_TEST("🧪", "Adaptive Mock Test"),
    QUICK_PRACTICE("✍️", "Concept Practice"),
    VOICE_LECTURE("🎙️", "Audio Lecture"),
    DAILY_PLAN_TASK("📅", "Scheduled Goal")
}

data class NextBestAction(
    val title: String,
    val subject: String,
    val topic: String,
    val durationMinutes: Int,
    val priority: PlanPriority = PlanPriority.HIGH,
    val actionType: NextBestActionType = NextBestActionType.FOCUS_SESSION,
    val reason: String,
    val whyThisHelpful: String,
    val questionsCount: Int? = null,
    val urgencyTag: String = "HIGH PRIORITY",
    val isAvailableTimeAdjusted: Boolean = false
)

data class GoalRadarStatus(
    val examName: String,
    val daysRemaining: Int,
    val syllabusCoveredPercent: Int,
    val studyPaceStatus: String, // "On Track 🚀", "Slightly Behind ⚠️", "Needs Focus 🎯"
    val weeklyHoursCompleted: Float,
    val weeklyHoursTarget: Float,
    val calmAdvice: String,
    val prioritySubject: String,
    val weakTopicNeedCare: String
)

data class StudentMasterContext(
    val userProfile: UserProfile,
    val pendingPlansCount: Int,
    val completedPlansCount: Int,
    val dueFlashcardsCount: Int,
    val unmasteredMistakesCount: Int,
    val avgTestAccuracy: Float,
    val totalFocusMinutes: Int,
    val streakDays: Int,
    val examDaysRemaining: Int,
    val nextBestAction: NextBestAction,
    val goalRadar: GoalRadarStatus
)

// --- 6. Room-Based Intelligence Entities for Student Context ---

@Entity(tableName = "exam_objectives")
data class ExamObjective(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String,
    val targetScoreOrRank: String = "Top 500 AIR",
    val examDateMillis: Long = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000,
    val category: String = "Competitive",
    val targetWeeklyStudyHours: Float = 25f,
    val totalSyllabusTopicsCount: Int = 100,
    val completedSyllabusTopicsCount: Int = 30,
    val prioritySubjects: List<String> = listOf("Physics", "Mathematics", "Chemistry"),
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "topic_masteries")
data class TopicMastery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "current_user",
    val examId: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val topicId: String = "",
    val subject: String,
    val chapter: String = "",
    val topic: String,
    val studyCompletionStatus: String = "NOT_STARTED", // NOT_STARTED, STARTED, COMPLETED
    val totalStudyMinutes: Int = 0,
    val practiceAttempts: Int = 0,
    val practiceCorrect: Int = 0,
    val practiceAccuracyPercent: Float = 0f,
    val mockAttempts: Int = 0,
    val mockCorrect: Int = 0,
    val recentAttemptsCount: Int = 0,
    val recentCorrectCount: Int = 0,
    val recentAccuracyPercent: Float = 0f,
    val repeatedMistakesCount: Int = 0,
    val confidenceLevel: String = "LOW", // LOW, MEDIUM, HIGH
    val masteryState: String = "NOT_STARTED", // NOT_STARTED, LEARNING, PRACTICING, WEAK, IMPROVING, STRONG, MASTERED, REVISION_DUE
    val masteryScore: Int = 0, // 0 - 100
    val accuracyPercent: Float = 0f,
    val totalQuestionsAttempted: Int = 0,
    val correctQuestionsCount: Int = 0,
    val incorrectQuestionsCount: Int = 0,
    val easySolved: Int = 0,
    val medSolved: Int = 0,
    val hardSolved: Int = 0,
    val userManualOverride: String = "NONE", // NONE, I_KNOW_THIS, NEED_HELP, IMPORTANT, SKIP_FOR_NOW
    val retentionDecayRate: Float = 1.0f,
    val lastStudiedMillis: Long = 0L,
    val lastTestedMillis: Long = 0L,
    val recommendedReviewDateMillis: Long = System.currentTimeMillis() + 24L * 60 * 60 * 1000,
    val masteryLevel: String = "DEVELOPING", // DEPRECATED alias for backwards compatibility
    val weakSpots: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

// --- Step 10 Models for Topic Mastery & Exam Readiness ---

data class SubjectProgressSummary(
    val subjectId: String,
    val subjectName: String,
    val totalTopicsCount: Int,
    val completedTopicsCount: Int,
    val masteredTopicsCount: Int,
    val weakTopicsCount: Int,
    val revisionDueCount: Int,
    val overallAccuracyPercent: Float,
    val averageMasteryScore: Int,
    val weakTopicsList: List<TopicMastery> = emptyList()
)

data class ChapterProgressSummary(
    val chapterId: String,
    val chapterName: String,
    val subjectName: String,
    val totalTopicsCount: Int,
    val masteredTopicsCount: Int,
    val weakTopicsCount: Int,
    val averageMasteryScore: Int
)

data class ExamReadinessScore(
    val examId: String,
    val examName: String,
    val readinessScore: Int, // 0 - 100
    val status: String, // EARLY_PREPARATION, BUILDING, ON_TRACK, NEEDS_ATTENTION, HIGH_READINESS, INSUFFICIENT_DATA
    val statusBadgeText: String,
    val syllabusCoveragePercent: Int,
    val topicMasteryPercent: Int = 0,
    val mockPerformancePercent: Int = 0,
    val revisionHealthPercent: Int = 100,
    val consistencyPercent: Int = 100,
    val subjectBalanceScore: Int = 100,
    val recentAccuracyPercent: Float = 0f,
    val totalTopicsCount: Int = 0,
    val masteredTopicsCount: Int = 0,
    val weakTopicsCount: Int = 0,
    val revisionDueCount: Int = 0,
    val actionableInsight: String = "",
    val explanation: String = "",
    val warnings: List<String> = emptyList(),
    val actionPlan: List<String> = emptyList(),
    val isNearExamMode: Boolean = false
)

data class StudyRecommendation(
    val topicId: String,
    val examId: String,
    val subjectName: String,
    val chapterName: String,
    val topicName: String,
    val recommendedAction: String, // PRACTICE, REVISE, LEARN_NEW, MISTAKE_REVIEW
    val reason: String,
    val priorityScore: Int,
    val recommendedDurationMinutes: Int,
    val masteryState: String,
    val currentMasteryScore: Int,
    val isHighYield: Boolean = false
)

data class DailyStudyPlan(
    val totalAvailableMinutes: Int,
    val targetExamName: String,
    val items: List<StudyRecommendation>,
    val summaryAdvice: String
)

@Entity(tableName = "student_session_history")
data class StudentSessionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionType: String, // FOCUS, MOCK_TEST, SPACED_REVISION, SMART_SEARCH, VOICE_TUTOR, ADAPTIVE_QUIZ
    val subject: String,
    val topic: String,
    val durationMinutes: Int,
    val actualMinutesSpent: Int,
    val xpEarned: Int = 30,
    val accuracyPercent: Float? = null,
    val questionsAttempted: Int = 0,
    val productivityRating: Int = 4, // 1 to 5
    val notesSummary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "intelligence_snapshots")
data class IntelligenceSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val examDaysRemaining: Int = 60,
    val overallMasteryScore: Int = 65,
    val syllabusCompletionPercent: Int = 40,
    val readinessIndex: Float = 72f,
    val topRecommendedActionTitle: String = "",
    val topRecommendedSubject: String = "",
    val topRecommendedTopic: String = "",
    val pacingStatus: String = "On Track 🚀",
    val insightsSummary: String = "",
    val weakTopicsCount: Int = 0,
    val masteredTopicsCount: Int = 0
)

// --- Step 14 - Smart Learning Content & Doubt Solving Models ---

@Entity(tableName = "learning_topic_contents")
data class LearningTopicContent(
    @PrimaryKey val id: String, // e.g. "railway_rrb_ntpc_mathematics_percentage"
    val examId: String,
    val subject: String,
    val chapter: String,
    val topic: String,
    val conceptSummary: String,
    val explanationQuick: String,
    val explanationNormal: String,
    val explanationDetailed: String,
    val keyPointsJson: String = "[]",
    val formulasJson: String = "[]",
    val workedExamplesJson: String = "[]",
    val commonMistakesJson: String = "[]",
    val practiceQuestionsJson: String = "[]",
    val quickTestQuestionsJson: String = "[]",
    val isAiGenerated: Boolean = true,
    val language: String = "English",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_learning_bookmarks")
data class UserLearningBookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "current_user",
    val examId: String = "",
    val subject: String,
    val chapter: String = "",
    val topic: String,
    val contentType: String, // "TOPIC", "FORMULA", "EXAMPLE", "QUESTION"
    val title: String,
    val snippet: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class WorkedExampleItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val approach: String,
    val steps: List<String>,
    val finalAnswer: String,
    val shortcutTip: String = ""
)

data class PracticeQuestionWithHints(
    val id: String = java.util.UUID.randomUUID().toString(),
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val hints: List<String> = emptyList(), // Progressive hints
    val fullExplanation: String,
    val difficulty: String = "Medium",
    val sourceBadge: String = "✨ AI Practice"
)

data class QuickTestQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

@Entity(tableName = "question_history")
data class QuestionHistoryEntity(
    @PrimaryKey val id: String, // e.g. "user1_q101"
    val userId: String = "current_user",
    val questionId: String,
    val examId: String = "",
    val subject: String = "",
    val topic: String = "",
    val attemptCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val lastAttemptedAt: Long = System.currentTimeMillis(),
    val lastResult: String = "", // "CORRECT", "INCORRECT", "SKIPPED"
    val lastResponseTimeSecs: Int = 0
)

@Entity(tableName = "question_quality_reports")
data class QuestionQualityReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "current_user",
    val questionId: String,
    val examId: String = "",
    val reason: String, // "Wrong Answer Key", "Unclear Question", "Incorrect Options", "Out of Syllabus", "Duplicate", "Other"
    val notes: String = "",
    val status: String = "UNDER_REVIEW", // ACTIVE, UNDER_REVIEW, DISABLED
    val timestamp: Long = System.currentTimeMillis()
)

data class CurrentAffairsQuizSession(
    val title: String,
    val questions: List<Question>,
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isSubmitted: Boolean = false,
    val score: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unansweredCount: Int = 0,
    val language: String = "English"
)







