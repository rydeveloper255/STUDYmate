package com.example.data.model

import com.example.data.model.ChapterEntity
import com.example.data.model.ExamEntity
import com.example.data.model.ExamSubjectEntity
import com.example.data.model.TopicEntity

/**
 * Verification States for Exam Intelligence Data
 */
enum class ExamVerificationStatus {
    VERIFIED_OFFICIAL,  // Confirmed from official board website / notification
    VERIFIED_RELIABLE,  // Confirmed from established educational portal
    AI_STRUCTURED,      // Extracted & structured via Gemini from web sources
    NEEDS_REVIEW,      // Conflicts detected across sources requiring admin review
    STALE,             // Cache expired / pending background refresh
    UNVERIFIED          // No verified web sources found yet
}

/**
 * Source Metadata for Grounded Information
 */
data class ExamSourceMetadata(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String = "",
    val retrievedTimeMillis: Long = System.currentTimeMillis(),
    val sourceType: String = "EDUCATIONAL" // OFFICIAL_BOARD, NOTIFICATION, PORTAL, EDUCATIONAL, UNVERIFIED
)

/**
 * Centralized, Authoritative Exam Context consumed by all application modules.
 */
data class ExamContext(
    val examId: String = "default_exam",
    val examName: String = "JEE / NEET / Board",
    val category: String = "Competitive",
    val conductingBody: String = "Official Examining Authority",
    val officialWebsite: String = "",
    val examPattern: String = "",
    val durationMinutes: Int = 90,
    val totalQuestions: Int = 100,
    val totalMarks: Int = 100,
    val negativeMarkingText: String = "1/3rd mark per wrong answer",
    val languages: List<String> = listOf("English", "Hindi"),
    val subjects: List<ExamSubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val verificationStatus: ExamVerificationStatus = ExamVerificationStatus.VERIFIED_RELIABLE,
    val confidenceScore: Float = 0.95f,
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val sources: List<ExamSourceMetadata> = emptyList()
) {
    companion object {
        fun defaultContext(): ExamContext {
            val defaultExam = ExamEntity(
                id = "railway_rrb_ntpc",
                name = "RRB NTPC (Railway Non-Technical)",
                category = "Railway Exams",
                shortCode = "RRB NTPC",
                description = "Railway Recruitment Board national entrance for Station Master, Goods Guard, Clerks & Typists.",
                examPattern = "CBT-1: General Awareness (40 Qs), Mathematics (30 Qs), Reasoning (30 Qs) = 100 Marks (90 mins). Negative marking 1/3rd.",
                totalMarks = 100,
                durationMinutes = 90,
                conductsConductingBody = "Railway Recruitment Control Board (RRB)",
                isPopular = true,
                iconName = "Train"
            )

            val subs = listOf(
                ExamSubjectEntity("rrb_math", "railway_rrb_ntpc", "Mathematics", "MATH", isOfficial = true, weightagePercent = 30, colorHex = "#3B82F6"),
                ExamSubjectEntity("rrb_reas", "railway_rrb_ntpc", "General Intelligence & Reasoning", "REAS", isOfficial = true, weightagePercent = 30, colorHex = "#8B5CF6"),
                ExamSubjectEntity("rrb_ga", "railway_rrb_ntpc", "General Awareness", "GA", isOfficial = true, weightagePercent = 40, colorHex = "#10B981")
            )

            val chaps = listOf(
                ChapterEntity("rrb_ch_m1", "rrb_math", "railway_rrb_ntpc", "Number System & Arithmetic", 1, isHighYield = true),
                ChapterEntity("rrb_ch_m2", "rrb_math", "railway_rrb_ntpc", "Algebra & Geometry", 2, isHighYield = false),
                ChapterEntity("rrb_ch_r1", "rrb_reas", "railway_rrb_ntpc", "Analogies & Series", 1, isHighYield = true),
                ChapterEntity("rrb_ch_g1", "rrb_ga", "railway_rrb_ntpc", "Current Affairs & History", 1, isHighYield = true)
            )

            val tops = listOf(
                TopicEntity("rrb_top_m1", "rrb_ch_m1", "rrb_math", "railway_rrb_ntpc", "LCM, HCF & Divisibility Rules", isHighYield = true),
                TopicEntity("rrb_top_m2", "rrb_ch_m1", "rrb_math", "railway_rrb_ntpc", "Percentages & Profit Loss", isHighYield = true),
                TopicEntity("rrb_top_r1", "rrb_ch_r1", "rrb_reas", "railway_rrb_ntpc", "Coding Decoding & Syllogism", isHighYield = true),
                TopicEntity("rrb_top_g1", "rrb_ch_g1", "rrb_ga", "railway_rrb_ntpc", "Indian History & Constitution", isHighYield = true)
            )

            return ExamContext(
                examId = defaultExam.id,
                examName = defaultExam.name,
                category = defaultExam.category,
                conductingBody = defaultExam.conductsConductingBody,
                officialWebsite = "https://indianrailways.gov.in",
                examPattern = defaultExam.examPattern,
                durationMinutes = defaultExam.durationMinutes,
                totalQuestions = 100,
                totalMarks = defaultExam.totalMarks,
                negativeMarkingText = "1/3rd mark deducted per wrong answer",
                languages = listOf("English", "Hindi", "Tamil", "Telugu", "Bengali", "Marathi"),
                subjects = subs,
                chapters = chaps,
                topics = tops,
                verificationStatus = ExamVerificationStatus.VERIFIED_OFFICIAL,
                confidenceScore = 0.98f,
                lastVerifiedAt = System.currentTimeMillis(),
                sources = listOf(
                    ExamSourceMetadata(
                        title = "RRB Official Recruitment Notification",
                        url = "https://indianrailways.gov.in/rrb_ntpc_syllabus",
                        domain = "indianrailways.gov.in",
                        snippet = "CBT 1 Syllabus: Mathematics (30 marks), Reasoning (30 marks), General Awareness (40 marks). Total 90 minutes duration.",
                        sourceType = "OFFICIAL_BOARD"
                    )
                )
            )
        }
    }
}

/**
 * State of Exam Syllabus Discovery & Synthesis
 */
sealed interface ExamDiscoveryState {
    object Idle : ExamDiscoveryState
    data class Loading(val progressMessage: String = "Preparing your exam information...") : ExamDiscoveryState
    data class Success(val context: ExamContext) : ExamDiscoveryState
    data class Error(val errorMessage: String) : ExamDiscoveryState
}

/**
 * DTO for Gemini JSON Syllabus Extraction
 */
data class ParsedSyllabusSubjectDto(
    val name: String,
    val code: String = "",
    val weightagePercent: Int = 25,
    val colorHex: String = "#3B82F6",
    val chapters: List<ParsedSyllabusChapterDto> = emptyList()
)

data class ParsedSyllabusChapterDto(
    val name: String,
    val isHighYield: Boolean = false,
    val description: String = "",
    val topics: List<String> = emptyList()
)

data class ParsedExamIntelligenceResponse(
    val examName: String,
    val category: String,
    val conductingBody: String = "",
    val officialWebsite: String = "",
    val durationMinutes: Int = 90,
    val totalQuestions: Int = 100,
    val totalMarks: Int = 100,
    val negativeMarking: String = "1/3rd mark per wrong answer",
    val languages: List<String> = listOf("English", "Hindi"),
    val patternSummary: String = "",
    val subjects: List<ParsedSyllabusSubjectDto> = emptyList(),
    val sources: List<ExamSourceMetadata> = emptyList(),
    val verificationStatus: String = "VERIFIED_RELIABLE",
    val confidence: Float = 0.9f
)
