package com.example.service.intelligence

import android.util.Log
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SmartLearningEngine(
    private val database: StudyMateDatabase,
    private val geminiRepository: GeminiRepository
) {
    private val TAG = "SmartLearningEngine"
    private val contentDao = database.learningTopicContentDao()
    private val bookmarkDao = database.userLearningBookmarkDao()
    private val smartNoteDao = database.smartNoteDao()
    private val mistakeDao = database.mistakeDao()

    /**
     * Resolves or generates exam-accurate learning content for a specific Topic under active ExamContext.
     */
    suspend fun loadTopicContent(
        examContext: ExamContext,
        subject: String,
        chapter: String,
        topic: String,
        masteryScore: Int = 50,
        languagePreference: String = "English",
        forceRefresh: Boolean = false
    ): LearningTopicContent = withContext(Dispatchers.IO) {
        val safeExamId = if (examContext.examId.isNotBlank()) examContext.examId else "default_exam"
        val contentId = "${safeExamId}_${subject.lowercase().replace(" ", "_")}_${topic.lowercase().replace(" ", "_")}"

        if (!forceRefresh) {
            val cached = contentDao.getContentById(contentId)
            if (cached != null && System.currentTimeMillis() - cached.lastUpdatedMillis < 7 * 24 * 60 * 60 * 1000L) {
                Log.d(TAG, "Loaded cached learning content for: $topic")
                return@withContext cached
            }
        }

        // Fetch user mistakes for this topic to customize "Your Common Mistakes"
        val userMistakes = try {
            mistakeDao.getUnmasteredMistakesForTopic(safeExamId, topic)
        } catch (e: Exception) {
            emptyList()
        }

        Log.d(TAG, "Generating AI learning content for: $topic ($subject / $chapter) in $languagePreference...")
        val generated = generateAiLearningContent(
            examContext = examContext,
            subject = subject,
            chapter = chapter,
            topic = topic,
            masteryScore = masteryScore,
            languagePreference = languagePreference,
            userMistakes = userMistakes,
            contentId = contentId
        )

        contentDao.insertOrUpdateContent(generated)
        generated
    }

    /**
     * Generates AI Learning Content via Gemini API with structured prompts.
     */
    private suspend fun generateAiLearningContent(
        examContext: ExamContext,
        subject: String,
        chapter: String,
        topic: String,
        masteryScore: Int,
        languagePreference: String,
        userMistakes: List<MistakeItem>,
        contentId: String
    ): LearningTopicContent {
        val userMistakeSummary = if (userMistakes.isNotEmpty()) {
            "User's past recorded mistakes on this topic: " + userMistakes.take(3).joinToString("; ") { m -> m.explanation.ifBlank { m.questionText } }
        } else {
            "No prior personal user mistakes recorded yet."
        }


        val prompt = """
            Generate structured high-yield study learning content for the topic: "$topic" in Chapter "$chapter", Subject "$subject" for the target exam "${examContext.examName}".
            Student Mastery Level: $masteryScore/100.
            Language Preference: $languagePreference (If Hinglish: write natural Hindi in Roman script combined with English academic terms).
            $userMistakeSummary

            Respond STRICTLY with a valid JSON object matching this structure (no extra outer markdown fence text):
            {
              "conceptSummary": "A concise 2-sentence intuitive core concept summary.",
              "explanationQuick": "• Key Point 1\n• Key Point 2\n• Key Point 3 (2-4 quick bullet points)",
              "explanationNormal": "Balanced 3-paragraph explanation with clear intuition, real-world analogy, and core theory.",
              "explanationDetailed": "Comprehensive in-depth explanation with edge cases, exam tricks, derivations, and common traps.",
              "keyPoints": ["Key takeaway 1", "Key takeaway 2", "Key takeaway 3", "Key takeaway 4"],
              "formulas": ["Formula / Rule 1 with explanation", "Formula / Rule 2 with shortcut"],
              "workedExamples": [
                {
                  "question": "Example problem statement",
                  "approach": "Recommended strategy to solve",
                  "steps": ["Step 1: Write given values", "Step 2: Apply formula", "Step 3: Calculate answer"],
                  "finalAnswer": "Final computed result",
                  "shortcutTip": "Pro-tip or shortcut trick"
                }
              ],
              "commonMistakes": [
                "Common mistake 1 student often makes and how to avoid it",
                "Common mistake 2 regarding units or signs"
              ],
              "practiceQuestions": [
                {
                  "questionText": "Practice question statement",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctOptionIndex": 0,
                  "hints": ["Hint 1: Look at formula X", "Hint 2: Simplify step Y"],
                  "fullExplanation": "Detailed step-by-step solution",
                  "difficulty": "Medium",
                  "sourceBadge": "✨ AI Practice"
                }
              ],
              "quickTestQuestions": [
                {
                  "questionText": "Quick check question 1",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctOptionIndex": 1,
                  "explanation": "Why B is correct"
                }
              ]
            }
        """.trimIndent()

        return try {
            val res = geminiRepository.askNova(
                userPrompt = prompt,
                studyContext = NovaStudyContext(
                    targetExam = examContext.examName,
                    preferredLanguage = languagePreference
                )
            )

            val rawJson = res.getOrNull()?.replyMarkdown ?: ""
            parseLearningContentJson(rawJson, contentId, examContext.examId, subject, chapter, topic, languagePreference)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI content for $topic", e)
            buildFallbackLearningContent(contentId, examContext.examId, subject, chapter, topic, languagePreference)
        }
    }

    private fun parseLearningContentJson(
        rawText: String,
        contentId: String,
        examId: String,
        subject: String,
        chapter: String,
        topic: String,
        language: String
    ): LearningTopicContent {
        return try {
            val cleaned = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleaned)
            LearningTopicContent(
                id = contentId,
                examId = examId,
                subject = subject,
                chapter = chapter,
                topic = topic,
                conceptSummary = json.optString("conceptSummary", "Core concept overview for $topic."),
                explanationQuick = json.optString("explanationQuick", "• Core principle of $topic\n• Key application in $subject"),
                explanationNormal = json.optString("explanationNormal", "$topic forms an essential foundation in $subject for $examId. Understanding its fundamentals enables quick problem solving."),
                explanationDetailed = json.optString("explanationDetailed", "In-depth study of $topic covers primary definitions, mathematical relationships, standard edge cases, and high-yield exam applications."),
                keyPointsJson = json.optJSONArray("keyPoints")?.toString() ?: "[\"Master fundamental definitions\", \"Practice standard formulas\"]",
                formulasJson = json.optJSONArray("formulas")?.toString() ?: "[\"Standard Formula: Express relations clearly\", \"Shortcut Rule: Apply mental math where applicable\"]",
                workedExamplesJson = json.optJSONArray("workedExamples")?.toString() ?: "[]",
                commonMistakesJson = json.optJSONArray("commonMistakes")?.toString() ?: "[\"Avoid calculation errors in signs and units\"]",
                practiceQuestionsJson = json.optJSONArray("practiceQuestions")?.toString() ?: "[]",
                quickTestQuestionsJson = json.optJSONArray("quickTestQuestions")?.toString() ?: "[]",
                isAiGenerated = true,
                language = language,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AI JSON, building fallback structure", e)
            buildFallbackLearningContent(contentId, examId, subject, chapter, topic, language)
        }
    }

    private fun buildFallbackLearningContent(
        contentId: String,
        examId: String,
        subject: String,
        chapter: String,
        topic: String,
        language: String
    ): LearningTopicContent {
        val sampleExamples = JSONArray().apply {
            put(JSONObject().apply {
                put("question", "How is $topic applied in solving $subject problems for $examId?")
                put("approach", "Identify given quantities, apply core formulas, and verify units.")
                put("steps", JSONArray(listOf("Step 1: List all given variables.", "Step 2: Substitute values into the standard relation.", "Step 3: Solve for the required variable.")))
                put("finalAnswer", "Correct calculated output with proper units.")
                put("shortcutTip", "Use mental estimation to eliminate wrong options quickly.")
            })
        }

        val samplePractice = JSONArray().apply {
            put(JSONObject().apply {
                put("questionText", "Which of the following statements is TRUE regarding $topic?")
                put("options", JSONArray(listOf("Statement A is accurate for standard conditions", "Statement B is always incorrect", "Statement C applies only in rare cases", "None of the above")))
                put("correctOptionIndex", 0)
                put("hints", JSONArray(listOf("Hint 1: Review the fundamental definition.", "Hint 2: Check standard boundary conditions.")))
                put("fullExplanation", "Statement A correctly reflects the core principles of $topic under standard exam conditions.")
                put("difficulty", "Medium")
                put("sourceBadge", "✨ AI Practice")
            })
        }

        val sampleQuickTest = JSONArray().apply {
            put(JSONObject().apply {
                put("questionText", "Quick Check: What is the primary objective when tackling a question on $topic?")
                put("options", JSONArray(listOf("Apply fundamental relations directly", "Guess without calculation", "Ignore boundary conditions", "Skip the question")))
                put("correctOptionIndex", 0)
                put("explanation", "Applying fundamental relations systematically guarantees accuracy.")
            })
        }

        return LearningTopicContent(
            id = contentId,
            examId = examId,
            subject = subject,
            chapter = chapter,
            topic = topic,
            conceptSummary = "$topic is a high-yield topic in $subject for your target exam.",
            explanationQuick = "• Core definition and formula for $topic\n• Key problem types asked in $examId\n• Standard tricks to boost accuracy and speed",
            explanationNormal = "$topic represents a critical conceptual block in $subject. Developing a clear intuition for $topic helps solve both direct formula questions and multi-concept problems.",
            explanationDetailed = "Detailed analysis of $topic involves understanding first principles, standard derivations, key edge conditions, and time-saving shortcuts specifically tailored for $examId.",
            keyPointsJson = JSONArray(listOf("Understand core formulas", "Practice step-by-step calculations", "Review common pitfalls")).toString(),
            formulasJson = JSONArray(listOf("Standard Formula: Key relationship equations", "Pro Trick: Quick ratio or percentage shortcut")).toString(),
            workedExamplesJson = sampleExamples.toString(),
            commonMistakesJson = JSONArray(listOf("Confusing signs or units", "Skipping intermediate simplification steps")).toString(),
            practiceQuestionsJson = samplePractice.toString(),
            quickTestQuestionsJson = sampleQuickTest.toString(),
            isAiGenerated = false,
            language = language,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    // --- Bookmarks & Notes Operations ---

    fun getAllBookmarks(): Flow<List<UserLearningBookmark>> = bookmarkDao.getAllBookmarks()

    suspend fun toggleBookmark(
        subject: String,
        topic: String,
        title: String,
        snippet: String,
        contentType: String = "TOPIC"
    ) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarksForTopic(subject, topic)
        bookmarkDao.insertBookmark(
            UserLearningBookmark(
                subject = subject,
                topic = topic,
                title = title,
                snippet = snippet,
                contentType = contentType
            )
        )
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(id)
    }

    suspend fun saveTopicNote(
        subject: String,
        topic: String,
        contentMarkdown: String
    ): Long = withContext(Dispatchers.IO) {
        val note = SmartNoteItem(
            title = "Notes: $topic",
            subject = subject,
            topic = topic,
            contentMarkdown = contentMarkdown,
            isRevised = false
        )
        smartNoteDao.insertSmartNote(note)
    }
}
