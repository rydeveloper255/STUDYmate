package com.example.service.intelligence

import com.example.data.model.updates.LatestUpdateItem
import com.example.service.intelligence.smart.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Step 83: Smart Content Intelligence Comprehensive Unit Test Suite.
 * Covers:
 * 1. Context-based Category Classification (Vacancy vs Admit Card vs Result vs Answer Key vs Admission vs Other)
 * 2. Confidence Level & Threshold Scoring
 * 3. Multi-format Date Parsing & ISO Normalization
 * 4. Impossible / Invalid Calendar Date Validation
 * 5. Last Date Extension Recognition
 * 6. Vacancy Expiry Intelligence
 * 7. URL Sanitization & Tracking Parameter Stripping
 * 8. Contextual Link Classification & Verified PDF Detection
 * 9. Title Normalization & Safe Similarity Measurement
 * 10. Multi-Level Duplicate & Update Detection Hierarchy
 * 11. Cross-Source Factual Conflict Detection
 * 12. Quality Scoring & Auto-Publish Gating
 * 13. Fact-Only 1-Line Structured Summary Generation
 * 14. Full 12-Stage Pipeline End-to-End Execution
 * 15. Pipeline Idempotency Guarantee
 * 16. Strict Zero Fake Data Invariant
 */
@RunWith(RobolectricTestRunner::class)
class SmartIntelligenceTest {

    // 1. Context-based Category Classification: Admit Card overrides recruitment mention in title
    @Test
    fun testClassification_AdmitCardOverridesRecruitment() {
        val title = "SSC CGL 2026 Recruitment Exam Admit Card Released"
        val text = "Staff Selection Commission has uploaded the admit card and exam city slip for CGL 2026 Tier 1 exam."
        val result = SmartCategoryClassifier.classify(title, text)

        assertEquals(SmartContentCategory.ADMIT_CARD, result.category)
        assertTrue("Confidence should be high for clear admit card signals", result.confidence.score >= 0.70f)
        assertNotEquals(SmartConfidenceLevel.LOW, result.confidence.level)
    }

    // 2. Context-based Category Classification: Result & Merit List
    @Test
    fun testClassification_ResultDeclared() {
        val title = "UPSC Civil Services Examination 2026 Prelims Result Declared"
        val text = "Union Public Service Commission has published the list of qualified candidates for CSE 2026 Mains."
        val result = SmartCategoryClassifier.classify(title, text)

        assertEquals(SmartContentCategory.RESULT, result.category)
        assertTrue(result.confidence.score >= 0.70f)
    }

    // 3. Context-based Category Classification: Answer Key & Objection Link
    @Test
    fun testClassification_AnswerKeyAndObjection() {
        val title = "RRB ALP 2026 Provisional Answer Key & Objection Link Active"
        val text = "Railway Recruitment Boards have released the response sheet and tentative answer key."
        val result = SmartCategoryClassifier.classify(title, text)

        assertEquals(SmartContentCategory.ANSWER_KEY, result.category)
        assertTrue(result.confidence.score >= 0.70f)
    }

    // 4. Context-based Category Classification: Admission & Counseling
    @Test
    fun testClassification_AdmissionAndCounseling() {
        val title = "Delhi University CUET UG 2026 Admission Counseling Round 1"
        val text = "University of Delhi has commenced the seat allotment and admission form process for UG courses."
        val result = SmartCategoryClassifier.classify(title, text)

        assertEquals(SmartContentCategory.ADMISSION, result.category)
        assertTrue(result.confidence.score >= 0.70f)
    }

    // 5. Context-based Category Classification: Direct Vacancy
    @Test
    fun testClassification_DirectVacancy() {
        val title = "IBPS PO 2026 Notification Out for 4500 Probationary Officer Posts"
        val text = "Apply online for IBPS PO Recruitment 2026. Last Date to apply is 2026-09-25."
        val result = SmartCategoryClassifier.classify(title, text)

        assertEquals(SmartContentCategory.VACANCY, result.category)
        assertTrue(result.confidence.score >= 0.70f)
    }

    // 6. Context-based Category Classification: Ambiguous / Low signal content
    @Test
    fun testClassification_AmbiguousLowSignal() {
        val title = "General Announcement 2026"
        val text = "Important discussion will happen on the weekend."
        val result = SmartCategoryClassifier.classify(title, text)

        assertEquals(SmartContentCategory.OTHER, result.category)
        assertEquals(SmartConfidenceLevel.LOW, result.confidence.level)
    }

    // 7. Multi-Format Date Parsing & ISO Normalization
    @Test
    fun testDateNormalization_MultipleFormats() {
        assertEquals("2026-08-15", SmartDateIntelligence.normalizeToIso("2026-08-15"))
        assertEquals("2026-08-15", SmartDateIntelligence.normalizeToIso("15/08/2026"))
        assertEquals("2026-08-15", SmartDateIntelligence.normalizeToIso("15-08-2026"))
        assertEquals("2026-08-15", SmartDateIntelligence.normalizeToIso("15 August 2026"))
        assertEquals("2026-08-15", SmartDateIntelligence.normalizeToIso("August 15, 2026"))
        assertEquals("2026-08-15", SmartDateIntelligence.normalizeToIso("15.08.2026"))
    }

    // 8. Impossible / Invalid Calendar Date Validation
    @Test
    fun testDateValidation_ImpossibleDatesRejected() {
        assertNull("32 August is impossible and must be rejected", SmartDateIntelligence.normalizeToIso("32-08-2026"))
        assertNull("30 February is impossible and must be rejected", SmartDateIntelligence.normalizeToIso("30-02-2026"))
        assertNull("Month 13 is impossible and must be rejected", SmartDateIntelligence.normalizeToIso("15-13-2026"))
        assertNull("Malformed string must be rejected", SmartDateIntelligence.normalizeToIso("invalid-date-string"))
    }

    // 9. Last Date Extension Recognition
    @Test
    fun testDateIntelligence_LastDateExtended() {
        val text = """
            SSC CGL 2026 Recruitment Notice
            Original Last Date: 15-09-2026
            Last Date Extended to 30-09-2026
            Exam Date: 15-10-2026
        """.trimIndent()

        val dates = SmartDateIntelligence.extractAllDates(text, "2026-08-15")
        assertTrue("Should detect that last date was extended", dates.isLastDateExtended)
        assertEquals("2026-09-30", dates.lastDate)
        assertEquals("2026-10-15", dates.examDate)
    }

    // 10. Vacancy Expiry Intelligence
    @Test
    fun testVacancyExpiry_PastAndFutureDates() {
        val todayIso = "2026-08-31"
        assertTrue("Past date should be expired", SmartDateIntelligence.isDateExpired("2026-08-20", todayIso))
        assertFalse("Future date should not be expired", SmartDateIntelligence.isDateExpired("2026-09-15", todayIso))
        assertFalse("Today should not be expired", SmartDateIntelligence.isDateExpired("2026-08-31", todayIso))
    }

    // 11. URL Sanitization & Tracking Parameter Stripping
    @Test
    fun testUrlSanitization_TrackingParametersStripped() {
        val rawUrl = "https://ssc.gov.in/apply?utm_source=whatsapp&utm_medium=channel&post_id=123&fbclid=abcdef"
        val cleanUrl = SmartContentCleaner.sanitizeUrl(rawUrl)

        assertFalse("utm_source must be stripped", cleanUrl.contains("utm_source"))
        assertFalse("utm_medium must be stripped", cleanUrl.contains("utm_medium"))
        assertFalse("fbclid must be stripped", cleanUrl.contains("fbclid"))
        assertTrue("Functional query parameter post_id must be preserved", cleanUrl.contains("post_id=123"))
    }

    // 12. Link Classification & Verified PDF Detection
    @Test
    fun testLinkClassifier_PdfAndActionLinks() {
        val text = """
            Official Portal: https://ssc.gov.in
            Apply Online: https://ssc.gov.in/apply
            Download Notification PDF: https://ssc.gov.in/notices/cgl2026.pdf
            Result Link: https://ssc.gov.in/results
        """.trimIndent()

        val links = SmartLinkClassifier.extractAndClassifyLinks(text, SmartContentCategory.VACANCY)
        assertEquals(4, links.size)

        val pdfLink = links.find { it.isVerifiedPdf }
        assertNotNull("PDF link must be verified", pdfLink)
        assertEquals(SmartLinkType.PDF, pdfLink?.linkType)

        val applyLink = links.find { it.linkType == SmartLinkType.APPLY }
        assertNotNull("Apply link must be classified", applyLink)

        val resultLink = links.find { it.linkType == SmartLinkType.RESULT }
        assertNotNull("Result link must be classified", resultLink)
    }

    // 13. Title Normalization & Similarity
    @Test
    fun testTitleNormalization_OrderInvariantAndDistinctExams() {
        val titleA = "SSC CGL 2026 Recruitment Notification Out"
        val titleB = "SSC CGL Recruitment 2026 Apply Online"
        val titleC = "SSC CHSL 2026 Recruitment Notification Out"

        val simAB = SmartTitleNormalizer.calculateTitleSimilarity(titleA, titleB)
        val simAC = SmartTitleNormalizer.calculateTitleSimilarity(titleA, titleC)

        assertTrue("Identical recruitments with different word order should have high similarity", simAB >= 0.80f)
        assertTrue("Distinct exams (CGL vs CHSL) must have lower similarity", simAC < 0.80f)
    }

    // 14. 5-Level Duplicate & Update Detection
    @Test
    fun testDuplicateDetector_UpdateVsExactDuplicate() {
        val existingItem = LatestUpdateItem(
            id = "rec_001",
            title = "SSC CGL 2026 Recruitment",
            organization = "Staff Selection Commission (SSC)",
            lastDate = "2026-09-15",
            examDate = null,
            applyUrl = "https://ssc.gov.in/apply"
        )

        // Scenario A: Exact duplicate with same dates
        val duplicateItem = SmartProcessedItem(
            id = "wa_new_1",
            rawContent = "SSC CGL 2026 Recruitment",
            cleanedContent = "SSC CGL 2026 Recruitment",
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = SmartExtractedData.Vacancy(
                SmartVacancyData(
                    title = "SSC CGL 2026 Recruitment",
                    organization = "Staff Selection Commission (SSC)",
                    lastDate = "2026-09-15",
                    applyUrl = "https://ssc.gov.in/apply"
                )
            ),
            normalizedTitle = SmartTitleNormalizer.normalizeTitle("SSC CGL 2026 Recruitment"),
            qualityScore = SmartQualityScore(90, 15, 25, 15, 15, 10, 10, 0, true),
            duplicateResult = SmartDuplicateResult(false, false),
            aiSummary = "",
            sourceUrl = "https://whatsapp.com/channel/0029VaAbQf01NCrYADMLt00L",
            sourceName = "StudyMate WhatsApp Channel",
            sourceType = "whatsapp_channel",
            lastDate = "2026-09-15",
            status = SmartProcessingStatus.PROCESSING
        )

        val dupResult = SmartDuplicateDetector.evaluate(duplicateItem, listOf(existingItem))
        assertTrue("Should detect duplicate", dupResult.isDuplicate)
        assertFalse("Should NOT be marked as update when dates are identical", dupResult.isUpdate)

        // Scenario B: Updated post with extended last date
        val updatedItem = duplicateItem.copy(
            lastDate = "2026-09-30",
            extractedData = SmartExtractedData.Vacancy(
                SmartVacancyData(
                    title = "SSC CGL 2026 Recruitment",
                    organization = "Staff Selection Commission (SSC)",
                    lastDate = "2026-09-30",
                    isLastDateExtended = true
                )
            )
        )

        val updateResult = SmartDuplicateDetector.evaluate(updatedItem, listOf(existingItem))
        assertTrue("Should detect match", updateResult.isDuplicate)
        assertTrue("Should recognize as an UPDATE due to changed last date", updateResult.isUpdate)
        assertEquals("rec_001", updateResult.existingRecordId)
    }

    // 15. Cross-Source Factual Conflict Detection
    @Test
    fun testConflictDetector_ConflictingLastDates() {
        val existingOfficialRecord = LatestUpdateItem(
            id = "off_001",
            title = "UPSC Civil Services Examination 2026",
            organization = "Union Public Service Commission (UPSC)",
            lastDate = "2026-09-15",
            sourceName = "Official Portal",
            sourceType = "official_website"
        )

        val conflictingWhatsAppPost = SmartProcessedItem(
            id = "wa_conflict",
            rawContent = "UPSC CSE 2026",
            cleanedContent = "UPSC CSE 2026",
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.90f, SmartConfidenceLevel.HIGH),
            extractedData = SmartExtractedData.Vacancy(
                SmartVacancyData(
                    title = "UPSC Civil Services Examination 2026",
                    organization = "Union Public Service Commission (UPSC)",
                    lastDate = "2026-09-30", // Conflicting without extension mention!
                    isLastDateExtended = false
                )
            ),
            normalizedTitle = SmartTitleNormalizer.normalizeTitle("UPSC Civil Services Examination 2026"),
            qualityScore = SmartQualityScore(85, 15, 25, 15, 10, 10, 10, 0, true),
            duplicateResult = SmartDuplicateResult(false, false),
            aiSummary = "",
            sourceUrl = "https://whatsapp.com/channel/0029VaAbQf01NCrYADMLt00L",
            sourceName = "StudyMate WhatsApp Channel",
            sourceType = "whatsapp_channel",
            lastDate = "2026-09-30",
            status = SmartProcessingStatus.PROCESSING
        )

        val conflict = SmartConflictDetector.detectConflict(conflictingWhatsAppPost, listOf(existingOfficialRecord))
        assertTrue("Conflicting last date without extension must be detected as conflict", conflict.hasConflict)
        assertEquals("last_date", conflict.conflictingField)
    }

    // 16. Quality Score & Auto-Publish Gating
    @Test
    fun testQualityScorer_Gating() {
        // High quality complete item
        val scoreGood = SmartQualityScorer.calculateScore(
            title = "SSC CGL 2026 Recruitment Notification",
            confidence = SmartConfidenceScore(0.92f, SmartConfidenceLevel.HIGH),
            postDate = "2026-08-15",
            lastDate = "2026-09-30",
            examDate = "2026-10-15",
            organization = "Staff Selection Commission (SSC)",
            links = listOf(SmartLink("https://ssc.gov.in/apply", SmartLinkType.APPLY, "Apply")),
            hasConflict = false,
            hasInvalidDate = false
        )
        assertTrue("Good item score should be >= 80", scoreGood.totalScore >= 80)
        assertTrue("Good item should be eligible for auto publish", scoreGood.isEligibleForAutoPublish)

        // Low quality / missing data item
        val scoreBad = SmartQualityScorer.calculateScore(
            title = "",
            confidence = SmartConfidenceScore(0.30f, SmartConfidenceLevel.LOW),
            postDate = null,
            lastDate = null,
            examDate = null,
            organization = null,
            links = emptyList(),
            hasConflict = false,
            hasInvalidDate = false
        )
        assertTrue("Poor item score should be < 50", scoreBad.totalScore < 50)
        assertFalse("Poor item must NOT be auto-published", scoreBad.isEligibleForAutoPublish)
    }

    // 17. Fact-Only 1-Line Structured Summary Generation
    @Test
    fun testSummaryGenerator_CleanFactSummary() {
        val vacancyData = SmartExtractedData.Vacancy(
            SmartVacancyData(
                title = "SSC CGL 2026 Recruitment",
                organization = "Staff Selection Commission (SSC)",
                vacancyCount = 14500,
                lastDate = "2026-09-30"
            )
        )
        val summary = SmartSummaryGenerator.generateSummary(
            SmartContentCategory.VACANCY,
            vacancyData,
            "2026-09-30",
            null
        )
        assertTrue(summary.contains("Staff Selection Commission (SSC)"))
        assertTrue(summary.contains("14,500 Posts"))
        assertTrue(summary.contains("2026-09-30"))
    }

    // 18. Full End-to-End Pipeline Execution & Zero Fake Data
    @Test
    fun testEndToEndPipeline_CompleteExecution() = runBlocking {
        val pipeline = SmartContentIntelligencePipeline()
        val rawText = """
            Staff Selection Commission (SSC)
            SSC CGL 2026 Recruitment Notification Released
            Total Vacancies: 14,500
            Last Date: 2026-09-30
            Exam Date: 2026-10-20
            Official Website: https://ssc.gov.in
            Apply Online: https://ssc.gov.in/apply
            Download PDF: https://ssc.gov.in/notices/cgl2026.pdf
        """.trimIndent()

        val processed = pipeline.processContent(
            rawText = rawText,
            sourceMessageId = "msg_step83_test_1",
            sourcePostDate = "2026-08-15",
            forceLocalOnly = true
        )

        assertEquals(SmartContentCategory.VACANCY, processed.category)
        assertEquals("Staff Selection Commission (SSC)", processed.extractedData.organization)
        assertEquals("2026-09-30", processed.lastDate)
        assertEquals("2026-10-20", processed.examDate)
        assertEquals("https://ssc.gov.in/notices/cgl2026.pdf", processed.pdfUrl)
        assertTrue("Quality score should be high", processed.qualityScore.totalScore >= 75)
        assertEquals(SmartProcessingStatus.PUBLISHED, processed.status)
        assertTrue(processed.isActive)

        // Zero fake data verification: unmentioned salary or fee must remain null or empty
        val vacData = (processed.extractedData as SmartExtractedData.Vacancy).data
        assertNull("Unmentioned fee must be null", vacData.applicationFee)
        assertNull("Unmentioned age limit must be null", vacData.ageLimit)
    }

    // 19. Pipeline Idempotency Guarantee
    @Test
    fun testPipeline_Idempotency() = runBlocking {
        val pipeline = SmartContentIntelligencePipeline()
        val rawText = "BPSC 70th Recruitment 2026 Notification\nLast Date: 2026-09-10\nApply: https://bpsc.bih.nic.in"

        val firstRun = pipeline.processContent(
            rawText = rawText,
            sourceMessageId = "msg_idemp_1",
            sourcePostDate = "2026-08-10",
            forceLocalOnly = true
        )

        val existingItem = LatestUpdateItem(
            id = firstRun.id,
            title = firstRun.extractedData.title,
            organization = firstRun.extractedData.organization ?: "",
            lastDate = firstRun.lastDate,
            externalId = "msg_idemp_1",
            applyUrl = "https://bpsc.bih.nic.in"
        )

        val secondRun = pipeline.processContent(
            rawText = rawText,
            sourceMessageId = "msg_idemp_1",
            sourcePostDate = "2026-08-10",
            existingRecords = listOf(existingItem),
            forceLocalOnly = true
        )

        assertEquals(SmartProcessingStatus.DUPLICATE, secondRun.status)
        assertTrue(secondRun.duplicateResult.isDuplicate)
        assertFalse(secondRun.duplicateResult.isUpdate)
        assertEquals(firstRun.id, secondRun.id)
    }
}
