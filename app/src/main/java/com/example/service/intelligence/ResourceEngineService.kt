package com.example.service.intelligence

import android.content.Context
import com.example.data.local.StudyMateDatabase
import com.example.data.model.ReadingStatus
import com.example.data.model.ResourceBookmarkEntity
import com.example.data.model.ResourceSearchResult
import com.example.data.model.ResourceSource
import com.example.data.model.ResourceStatus
import com.example.data.model.ResourceType
import com.example.data.model.StudyResourceEntity
import com.example.data.remote.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

class ResourceEngineService(
    private val database: StudyMateDatabase,
    private val geminiRepository: GeminiRepository = GeminiRepository()
) {
    private val resourceDao = database.resourceDao()

    suspend fun seedDefaultResourcesIfEmpty() = withContext(Dispatchers.IO) {
        if (resourceDao.getResourceCount() > 0) return@withContext

        val defaults = listOf(
            StudyResourceEntity(
                resourceId = "res_cgl_math_percentage",
                title = "SSC CGL Mathematics: Percentage Formula Sheet & Shortcuts",
                description = "Comprehensive formula sheet covering Percentage change, Successive discount, Population problems, and Exam scoring formulas with solved examples.",
                resourceType = ResourceType.PDF.name,
                examId = "exam_ssc_cgl",
                examName = "SSC CGL",
                subjectId = "sub_maths",
                subjectName = "Mathematics",
                topicId = "top_percentage",
                topicName = "Percentage",
                language = "English",
                source = ResourceSource.STUDYMATE.name,
                fileUrl = "asset://sample_math_percentage.pdf",
                readingStatus = ReadingStatus.NOT_STARTED.name,
                totalPages = 12,
                fileSizeBytes = 2_450_000L,
                contentText = """
                    Percentage Formulas & Shortcuts:
                    1. Fraction to Percentage: Multiply fraction by 100. Example: 3/5 * 100 = 60%.
                    2. Percentage to Fraction: Divide by 100. Example: 45% = 45/100 = 9/20.
                    3. Percentage Change = [(Final Value - Initial Value) / Initial Value] * 100.
                    4. Successive Percentage Change = [A + B + (A*B)/100]%.
                    5. If A's income is x% more than B, then B's income is [x / (100 + x)] * 100% less than A.
                    6. Population formula: P_n = P_0 * (1 + R/100)^n.
                    Page 1: Fundamental Concept & Conversion Tables.
                    Page 2: Income & Expense Problems.
                    Page 3: Election & Voting Percentage.
                    Page 4: Exam Pass/Fail Marks Problems.
                    Page 5-12: SSC CGL Previous Year Solved Questions (2020-2025).
                """.trimIndent(),
                aiRecommendationReason = "Matches SSC CGL Maths curriculum for Percentage."
            ),
            StudyResourceEntity(
                resourceId = "res_railway_ca_monthly",
                title = "Railway Current Affairs Monthly Capsule (Hindi & English)",
                description = "Latest national developments, railway budgets, appointment list, science & technology updates tailored for RRB NTPC & Group D.",
                resourceType = ResourceType.CURRENT_AFFAIRS.name,
                examId = "exam_rrb_ntpc",
                examName = "RRB NTPC",
                subjectId = "sub_ga",
                subjectName = "General Awareness",
                topicId = "top_current_affairs",
                topicName = "Current Affairs",
                language = "Hindi",
                source = ResourceSource.OFFICIAL.name,
                fileUrl = "asset://current_affairs_monthly.pdf",
                readingStatus = ReadingStatus.IN_PROGRESS.name,
                lastViewedPage = 4,
                totalPages = 24,
                fileSizeBytes = 4_100_000L,
                contentText = """
                    Railway Current Affairs Highlights:
                    Page 1: Cabinet Approvals & Infrastructure Budgets.
                    Page 2: Vande Bharat & Bullet Train Project Milestones.
                    Page 3: National & International Appointments.
                    Page 4: Sports Honours & Major Awards 2026.
                    Page 5-24: Category-wise MCQs & Rapid Revision Notes.
                """.trimIndent(),
                aiRecommendationReason = "High yield for RRB NTPC General Awareness section."
            ),
            StudyResourceEntity(
                resourceId = "res_reasoning_coding_decoding",
                title = "Reasoning Tricks: Coding Decoding & Alphabet Series",
                description = "Quick short tricks for EJOTY, reverse letter positions, number coding, and substitution matrix for Bank & SSC.",
                resourceType = ResourceType.NOTES.name,
                examId = "exam_ssc_cgl",
                examName = "SSC CGL",
                subjectId = "sub_reasoning",
                subjectName = "Reasoning",
                topicId = "top_coding_decoding",
                topicName = "Coding Decoding",
                language = "Hinglish",
                source = ResourceSource.STUDYMATE.name,
                fileUrl = "asset://reasoning_coding.pdf",
                readingStatus = ReadingStatus.NOT_STARTED.name,
                totalPages = 8,
                fileSizeBytes = 1_800_000L,
                contentText = """
                    Reasoning Coding Decoding Quick Notes:
                    1. Forward Order: A=1, B=2 ... Z=26. Formula: EJOTY (5, 10, 15, 20, 25).
                    2. Reverse Order: Sum of Opposite Letters = 27 (e.g., A+Z = 1+26 = 27, B+Y = 2+25 = 27).
                    3. Direct Letter Substitution Code pattern recognition.
                    4. Matrix Code reading rule: Row number first, Column number second.
                    Page 1: Alphabet Position Tricks.
                    Page 2: Opposite Letter Pairs.
                    Page 3: Number & Symbol Coding.
                    Page 4-8: 50 Practice Questions with Solutions.
                """.trimIndent(),
                aiRecommendationReason = "Essential concept for Reasoning speed enhancement."
            ),
            StudyResourceEntity(
                resourceId = "res_physics_mechanics_summary",
                title = "Physics Mechanics & Motion Formulas Summary",
                description = "Equations of motion, Newton's laws, Work Energy Theorem, and Conservation of Momentum notes for Competitive Exams.",
                resourceType = ResourceType.REVISION.name,
                examId = "exam_upsc",
                examName = "UPSC Prelims",
                subjectId = "sub_physics",
                subjectName = "Physics",
                topicId = "top_mechanics",
                topicName = "Mechanics",
                language = "English",
                source = ResourceSource.STUDYMATE.name,
                fileUrl = "asset://physics_mechanics.pdf",
                readingStatus = ReadingStatus.NOT_STARTED.name,
                totalPages = 10,
                fileSizeBytes = 2_100_000L,
                contentText = """
                    Physics Mechanics Equations:
                    1. v = u + at, s = ut + 0.5*a*t^2, v^2 = u^2 + 2as.
                    2. Newton's 2nd Law: F = ma. Momentum p = mv.
                    3. Work W = F * d * cos(theta). Power P = W/t.
                    4. Kinetic Energy = 0.5 * m * v^2. Potential Energy = m * g * h.
                    Page 1: Linear Kinematics.
                    Page 2: Laws of Motion & Friction.
                    Page 3: Work, Power & Energy.
                    Page 4-10: Important Derivations & Numerical Problems.
                """.trimIndent(),
                aiRecommendationReason = "Directly matches Physics Mechanics syllabus."
            ),
            StudyResourceEntity(
                resourceId = "res_ai_generated_notes_percentage",
                title = "AI Generated: Master Percentage Concept Notes & Solved MCQs",
                description = "Automatically structured smart study sheet generated by StudyMate AI with step-by-step explanations.",
                resourceType = ResourceType.NOTES.name,
                examId = "exam_ssc_cgl",
                examName = "SSC CGL",
                subjectId = "sub_maths",
                subjectName = "Mathematics",
                topicId = "top_percentage",
                topicName = "Percentage",
                language = "Hinglish",
                source = ResourceSource.AI_GENERATED.name,
                fileUrl = "asset://ai_percentage_notes.pdf",
                readingStatus = ReadingStatus.COMPLETED.name,
                lastViewedPage = 6,
                totalPages = 6,
                fileSizeBytes = 1_200_000L,
                contentText = """
                    AI Generated Percentage Notes (StudyMate AI):
                    - Topic: Percentage Application in Profit Loss & Data Interpretation.
                    - Key Concept: Base value shifting formula.
                    - Example: If A is 25% higher than B, B is 20% lower than A [25/125 * 100 = 20%].
                    Page 1: Core Definitions & Fast Calculations.
                    Page 2: Application in Mixture & Alligation.
                    Page 3: Practice MCQs with AI Hints.
                """.trimIndent(),
                aiRecommendationReason = "AI generated study notes for fast recall."
            ),
            StudyResourceEntity(
                resourceId = "res_ssc_science_question_set",
                title = "SSC General Science Practice Question Set - 100 High-Yield MCQs",
                description = "Handpicked Biology, Chemistry, and Physics questions with detailed explanations for SSC CGL Tier 1.",
                resourceType = ResourceType.QUESTION_SET.name,
                examId = "exam_ssc_cgl",
                examName = "SSC CGL",
                subjectId = "sub_science",
                subjectName = "General Science",
                topicId = "top_general_science",
                topicName = "General Science",
                language = "English",
                source = ResourceSource.STUDYMATE.name,
                fileUrl = "asset://ssc_science_100mcqs.pdf",
                readingStatus = ReadingStatus.NOT_STARTED.name,
                totalPages = 15,
                fileSizeBytes = 3_200_000L,
                contentText = """
                    SSC Science Question Bank:
                    Q1. Which vitamin is known as Ascorbic Acid? Ans: Vitamin C.
                    Q2. What is the SI unit of electric current? Ans: Ampere.
                    Q3. Name the element with atomic number 1. Ans: Hydrogen.
                    Page 1-15: 100 MCQs covering Human Physiology, Chemical Elements, and Light/Sound Physics.
                """.trimIndent(),
                aiRecommendationReason = "Great question bank for daily practice."
            )
        )

        resourceDao.insertResources(defaults)
    }

    fun getAllResources(): Flow<List<StudyResourceEntity>> = resourceDao.getAllResources()

    suspend fun getResourceById(resourceId: String): StudyResourceEntity? = resourceDao.getResourceById(resourceId)

    suspend fun searchAndRankResources(
        query: String = "",
        selectedType: String = ResourceType.ALL.name,
        examId: String = "",
        subjectName: String = "",
        topicName: String = "",
        language: String = ""
    ): List<ResourceSearchResult> = withContext(Dispatchers.IO) {
        seedDefaultResourcesIfEmpty()
        val all = resourceDao.getAllResourcesOnce()

        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        val normalizedSubject = subjectName.trim().lowercase(Locale.ROOT)
        val normalizedTopic = topicName.trim().lowercase(Locale.ROOT)
        val normalizedExam = examId.trim().lowercase(Locale.ROOT)

        val filtered = all.filter { res ->
            val matchesType = when (selectedType) {
                ResourceType.ALL.name -> true
                ResourceType.SAVED_RESOURCE.name -> res.isSaved
                else -> res.resourceType.equals(selectedType, ignoreCase = true)
            }

            val matchesLang = language.isBlank() || res.language.equals(language, ignoreCase = true)

            matchesType && matchesLang
        }

        val results = filtered.mapNotNull { res ->
            var score = 0
            val matchReasons = mutableListOf<String>()

            val titleLower = res.title.lowercase(Locale.ROOT)
            val descLower = res.description.lowercase(Locale.ROOT)
            val topicLower = res.topicName.lowercase(Locale.ROOT)
            val subjectLower = res.subjectName.lowercase(Locale.ROOT)
            val examLower = res.examName.lowercase(Locale.ROOT)

            // Topic context match
            if (normalizedTopic.isNotEmpty() && (topicLower.contains(normalizedTopic) || normalizedTopic.contains(topicLower))) {
                score += 100
                matchReasons.add("Direct Topic match: ${res.topicName}")
            }

            // Subject context match
            if (normalizedSubject.isNotEmpty() && (subjectLower.contains(normalizedSubject) || normalizedSubject.contains(subjectLower))) {
                score += 50
                matchReasons.add("Subject match: ${res.subjectName}")
            }

            // Exam context match
            if (normalizedExam.isNotEmpty() && (examLower.contains(normalizedExam) || res.examId.lowercase(Locale.ROOT).contains(normalizedExam))) {
                score += 30
                matchReasons.add("Exam match: ${res.examName}")
            }

            // Text Query Search
            if (normalizedQuery.isNotEmpty()) {
                when {
                    titleLower.contains(normalizedQuery) -> {
                        score += 80
                        matchReasons.add("Title query match")
                    }
                    topicLower.contains(normalizedQuery) -> {
                        score += 60
                        matchReasons.add("Topic query match")
                    }
                    subjectLower.contains(normalizedQuery) -> {
                        score += 40
                        matchReasons.add("Subject query match")
                    }
                    descLower.contains(normalizedQuery) -> {
                        score += 20
                        matchReasons.add("Description match")
                    }
                    res.contentText.lowercase(Locale.ROOT).contains(normalizedQuery) -> {
                        score += 15
                        matchReasons.add("Content keyword match")
                    }
                    else -> {
                        // If user typed a query and this resource didn't match at all, filter out unless zero query
                        return@mapNotNull null
                    }
                }
            } else {
                // Default baseline score for general browsing
                score += 10
            }

            if (res.isSaved) {
                score += 5
            }

            ResourceSearchResult(
                resource = res,
                relevanceScore = score,
                matchReason = if (matchReasons.isNotEmpty()) matchReasons.joinToString(" • ") else "General study resource"
            )
        }

        results.sortedByDescending { it.relevanceScore }
    }

    suspend fun getNovaResourceAnswer(
        userQuery: String,
        currentExam: String = "",
        currentSubject: String = "",
        currentTopic: String = ""
    ): NovaResourceQueryResponse = withContext(Dispatchers.IO) {
        val q = userQuery.trim().lowercase(Locale.ROOT)

        // Resolve "iska", "is topic ka", "this topic"
        val isContextualRequest = q.contains("iska") || q.contains("is topic") || q.contains("iss topic") || q.contains("this topic")
        val targetTopic = if (isContextualRequest && currentTopic.isNotBlank()) currentTopic else ""
        val targetSubject = if (isContextualRequest && currentSubject.isNotBlank()) currentSubject else ""

        // Map query keywords to resource type
        val requestedType = when {
            q.contains("pdf") -> ResourceType.PDF.name
            q.contains("current affairs") || q.contains("ca") -> ResourceType.CURRENT_AFFAIRS.name
            q.contains("notes") || q.contains("note") -> ResourceType.NOTES.name
            q.contains("revision") -> ResourceType.REVISION.name
            q.contains("question") || q.contains("mcq") -> ResourceType.QUESTION_SET.name
            else -> ResourceType.ALL.name
        }

        val searchResults = searchAndRankResources(
            query = if (isContextualRequest) "" else q,
            selectedType = requestedType,
            examId = currentExam,
            subjectName = targetSubject.ifBlank { extractSubjectFromQuery(q) },
            topicName = targetTopic.ifBlank { extractTopicFromQuery(q) }
        )

        val topMatch = searchResults.firstOrNull()

        if (topMatch != null && topMatch.relevanceScore > 15) {
            val res = topMatch.resource
            val reason = res.aiRecommendationReason.ifBlank { "Matches your topic '${res.topicName}' (${res.subjectName}) for ${res.examName}." }
            val answerText = """
                📚 **Found Resource**: **${res.title}**
                
                • **Type**: ${res.resourceType} (${res.source})
                • **Subject / Topic**: ${res.subjectName} • ${res.topicName}
                • **Language**: ${res.language}
                
                💡 *Why recommended*: $reason
            """.trimIndent()

            NovaResourceQueryResponse(
                found = true,
                answerText = answerText,
                topResource = res,
                recommendationReason = reason
            )
        } else {
            val subjectRef = if (currentTopic.isNotBlank()) currentTopic else if (currentSubject.isNotBlank()) currentSubject else "is topic"
            val responseText = "Abhi $subjectRef ke liye koi verified study resource available nahi hai. Aap apina PDF ya notes upload kar sakte hain."
            NovaResourceQueryResponse(
                found = false,
                answerText = responseText,
                topResource = null,
                recommendationReason = "No matching resource found in database."
            )
        }
    }

    private fun extractSubjectFromQuery(q: String): String {
        return when {
            q.contains("math") || q.contains("maths") || q.contains("ganit") -> "Mathematics"
            q.contains("physics") -> "Physics"
            q.contains("chemistry") -> "Chemistry"
            q.contains("reasoning") -> "Reasoning"
            q.contains("science") -> "General Science"
            q.contains("history") -> "History"
            q.contains("current affairs") || q.contains("ca") -> "General Awareness"
            else -> ""
        }
    }

    private fun extractTopicFromQuery(q: String): String {
        return when {
            q.contains("percentage") || q.contains("pratishat") -> "Percentage"
            q.contains("coding") -> "Coding Decoding"
            q.contains("mechanics") || q.contains("motion") -> "Mechanics"
            else -> ""
        }
    }

    suspend fun answerDocumentQuestion(
        resourceId: String,
        userQuestion: String
    ): DocumentQAResponse = withContext(Dispatchers.IO) {
        val resource = resourceDao.getResourceById(resourceId)
            ?: return@withContext DocumentQAResponse("Resource not found.", isFromDocument = false, pageNumber = null)

        val docText = resource.contentText
        val isSummaryRequest = userQuestion.contains("summary", ignoreCase = true) || userQuestion.contains("saar", ignoreCase = true)

        if (isSummaryRequest && resource.aiSummaryCache.isNotBlank()) {
            return@withContext DocumentQAResponse(
                answerText = "📖 **Document Summary (Cached)**:\n${resource.aiSummaryCache}",
                isFromDocument = true,
                pageNumber = 1
            )
        }

        if (docText.isBlank()) {
            return@withContext DocumentQAResponse(
                answerText = "Is document ka extracted text available nahi hai. Main General Knowledge se jawab de raha hoon:\n\n${resource.description}",
                isFromDocument = false,
                pageNumber = null
            )
        }

        // Search for relevant page in extracted text
        val pages = docText.split("Page ").filter { it.isNotBlank() }
        var matchedPage: Int? = null
        var matchedSnippet = ""

        val queryKeywords = userQuestion.lowercase(Locale.ROOT).split(" ").filter { it.length > 3 }

        for (p in pages) {
            val pageNum = p.takeWhile { it.isDigit() }.toIntOrNull()
            val lowerP = p.lowercase(Locale.ROOT)
            if (queryKeywords.any { lowerP.contains(it) }) {
                matchedPage = pageNum
                matchedSnippet = p.take(300)
                break
            }
        }

        if (matchedSnippet.isNotBlank()) {
            val pageRefText = if (matchedPage != null) " (Page $matchedPage)" else ""
            val answer = "✓ **Found in Document$pageRefText**:\n\"$matchedSnippet...\""
            
            if (isSummaryRequest && resource.aiSummaryCache.isBlank()) {
                resourceDao.updateAiSummary(resourceId, answer)
            }
            
            DocumentQAResponse(
                answerText = answer,
                isFromDocument = true,
                pageNumber = matchedPage
            )
        } else {
            // Gemini Grounded Answer fallback
            val prompt = """
                You are StudyMate AI analyzing the document "${resource.title}".
                Document Snippet:
                $docText
                
                User Question: $userQuestion
                
                Answer the question concisely based on the document if information exists. Explicitly state "Found in document" or "General knowledge".
            """.trimIndent()

            val aiAnswer = try {
                geminiRepository.askNova(prompt).getOrNull()?.replyMarkdown
                    ?: "Is document me exact topic nahi mila. Resource description: ${resource.description}"
            } catch (e: Exception) {
                "Is document me exact topic nahi mila. Resource description: ${resource.description}"
            }

            DocumentQAResponse(
                answerText = aiAnswer,
                isFromDocument = true,
                pageNumber = 1
            )
        }
    }

    suspend fun saveUserUploadedResource(
        context: Context,
        userId: String,
        fileTitle: String,
        fileDescription: String,
        examName: String,
        subjectName: String,
        topicName: String,
        rawContentText: String,
        fileSizeBytes: Long = 1_500_000L
    ): StudyResourceEntity = withContext(Dispatchers.IO) {
        val resourceId = "usr_res_${UUID.randomUUID().toString().take(8)}"
        val storageDir = File(context.filesDir, "study_resources").apply { if (!exists()) mkdirs() }
        val targetFile = File(storageDir, "$resourceId.txt")

        targetFile.writeText(rawContentText)

        val entity = StudyResourceEntity(
            resourceId = resourceId,
            ownerScope = userId,
            title = fileTitle,
            description = fileDescription.ifBlank { "User uploaded study material for $subjectName." },
            resourceType = ResourceType.PDF.name,
            examId = "usr_exam",
            examName = examName.ifBlank { "General Exam" },
            subjectId = "usr_sub",
            subjectName = subjectName.ifBlank { "General Subject" },
            topicId = "usr_top",
            topicName = topicName.ifBlank { "General Notes" },
            language = "English",
            source = ResourceSource.USER_UPLOADED.name,
            fileUrl = targetFile.absolutePath,
            readingStatus = ReadingStatus.NOT_STARTED.name,
            totalPages = (rawContentText.length / 500).coerceAtLeast(1),
            fileSizeBytes = fileSizeBytes,
            contentText = rawContentText,
            aiRecommendationReason = "Private study material uploaded by user."
        )

        resourceDao.insertOrUpdateResource(entity)
        entity
    }

    suspend fun updateReadingProgress(resourceId: String, page: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val res = resourceDao.getResourceById(resourceId) ?: return@withContext
        val status = if (page >= totalPages && totalPages > 0) {
            ReadingStatus.COMPLETED.name
        } else if (page > 1) {
            ReadingStatus.IN_PROGRESS.name
        } else {
            res.readingStatus
        }

        resourceDao.updateReadingProgress(resourceId, status, page, totalPages)
    }

    suspend fun toggleSaveState(resourceId: String): Boolean = withContext(Dispatchers.IO) {
        val res = resourceDao.getResourceById(resourceId) ?: return@withContext false
        val newState = !res.isSaved
        resourceDao.updateSavedState(resourceId, newState)
        newState
    }

    suspend fun addBookmark(resourceId: String, pageNumber: Int, noteSnippet: String) = withContext(Dispatchers.IO) {
        val bookmark = ResourceBookmarkEntity(
            resourceId = resourceId,
            pageNumber = pageNumber,
            noteSnippet = noteSnippet
        )
        resourceDao.insertBookmark(bookmark)
    }

    suspend fun getBookmarks(resourceId: String): List<ResourceBookmarkEntity> = withContext(Dispatchers.IO) {
        resourceDao.getBookmarksForResource(resourceId)
    }

    suspend fun deleteResource(resourceId: String) = withContext(Dispatchers.IO) {
        val res = resourceDao.getResourceById(resourceId)
        if (res?.offlineFilePath?.isNotBlank() == true) {
            try { File(res.offlineFilePath).delete() } catch (_: Exception) {}
        }
        resourceDao.deleteResource(resourceId)
    }
}

data class NovaResourceQueryResponse(
    val found: Boolean,
    val answerText: String,
    val topResource: StudyResourceEntity?,
    val recommendationReason: String
)

data class DocumentQAResponse(
    val answerText: String,
    val isFromDocument: Boolean,
    val pageNumber: Int?
)
