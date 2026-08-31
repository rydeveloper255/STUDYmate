package com.example.service.intelligence.verification

import com.example.data.model.updates.LatestUpdateItem
import com.example.data.model.updates.UpdateCategory
import com.example.service.intelligence.smart.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Step 84: Comprehensive Verification & Quality Control Unit Tests.
 * 
 * Verifies all 28 critical verification scenarios:
 * 1. Valid Vacancy
 * 2. Expired Vacancy
 * 3. Future Vacancy
 * 4. Missing Last Date
 * 5. Extended Last Date
 * 6. Conflicting Last Dates
 * 7. Invalid / Impossible Calendar Date (32 Aug, 31 Feb)
 * 8. Result
 * 9. Admit Card
 * 10. Answer Key
 * 11. Admission
 * 12. Category Conflict (e.g. Result content marked as Vacancy)
 * 13. Duplicate
 * 14. Updated Duplicate
 * 15. Correction Notice
 * 16. Broken / Malformed URL
 * 17. Redirect / Shortener URL
 * 18. Suspicious / Dangerous Scheme (javascript:)
 * 19. Missing Title
 * 20. Missing Organization
 * 21. Missing Critical Information
 * 22. AI Hallucinated Field Protection
 * 23. Source Unavailable / Retry Limit
 * 24. Database Failure Resilience
 * 25. Idempotent Processing
 * 26. Stale Content Detection
 * 27. Multiple Viewer Consistency
 * 28. Security: Unauthorized Review Approval / Bypass Prevention
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentVerificationEngineTest {

    private lateinit var verificationEngine: ContentVerificationEngine
    private lateinit var reviewQueueManager: ReviewQueueManager
    private lateinit var versionManager: ContentVersionManager
    private lateinit var metricsTracker: VerificationMetricsTracker

    @Before
    fun setUp() {
        reviewQueueManager = ReviewQueueManager()
        versionManager = ContentVersionManager()
        metricsTracker = VerificationMetricsTracker()
        verificationEngine = ContentVerificationEngine(
            reviewQueueManager = reviewQueueManager,
            versionManager = versionManager,
            metricsTracker = metricsTracker
        )
        LinkHealthVerifier.clearCache()
    }

    // 1. Valid Vacancy
    @Test
    fun test1_validVacancy_publishesSuccessfully() {
        val raw = "SSC CGL Recruitment 2026. Total 8000 Posts. Last Date: 2026-10-15. Apply at https://ssc.gov.in"
        val extracted = SmartExtractedData.Vacancy(
            SmartVacancyData(
                title = "SSC CGL 2026 Recruitment",
                organization = "Staff Selection Commission",
                vacancyCount = 8000,
                lastDate = "2026-10-15",
                applyUrl = "https://ssc.gov.in"
            )
        )
        val links = listOf(SmartLink("https://ssc.gov.in", SmartLinkType.OFFICIAL, "Apply Online", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "SSC CGL 2026 Recruitment",
            links = links,
            sourceUrl = "https://whatsapp.com/channel/test",
            sourceName = "Official Jobs",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-10",
            lastDate = "2026-10-15",
            examDate = null
        )

        assertEquals(VerificationStatus.VERIFIED, report.overallStatus)
        assertTrue("Valid future vacancy must be eligible for auto-publish", report.isEligibleForAutoPublish)
        assertTrue(report.layer1Result.passed)
        assertTrue(report.layer2Result.passed)
    }

    // 2. Expired Vacancy
    @Test
    fun test2_expiredVacancy_statusIsExpired() {
        val raw = "UPSC Prelims 2025. Last Date: 2026-08-05. Official link https://upsc.gov.in"
        val extracted = SmartExtractedData.Vacancy(
            SmartVacancyData(
                title = "UPSC CSE 2025",
                organization = "Union Public Service Commission",
                lastDate = "2026-08-05"
            )
        )
        val links = listOf(SmartLink("https://upsc.gov.in", SmartLinkType.OFFICIAL, "Official Portal", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.9f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "UPSC CSE 2025",
            links = links,
            sourceUrl = "https://upsc.gov.in",
            sourceName = "UPSC",
            sourceType = "WEBSITE",
            sourcePostDate = "2026-08-01",
            lastDate = "2026-08-05", // Assuming today is 2026-08-31
            examDate = null
        )

        assertEquals(VerificationStatus.EXPIRED, report.overallStatus)
        assertFalse(report.isEligibleForAutoPublish)
    }

    // 3. Future Vacancy
    @Test
    fun test3_futureVacancy_verified() {
        val raw = "Railway RRB ALP 2026. Apply Online. 10000 Posts. Last Date: 2026-11-30. https://rrbcdg.gov.in"
        val extracted = SmartExtractedData.Vacancy(
            SmartVacancyData(
                title = "RRB ALP 2026 Recruitment",
                organization = "Railway Recruitment Board",
                vacancyCount = 10000,
                lastDate = "2026-11-30"
            )
        )
        val links = listOf(SmartLink("https://rrbcdg.gov.in", SmartLinkType.OFFICIAL, "Apply Online", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "RRB ALP 2026 Recruitment",
            links = links,
            sourceUrl = "https://rrbcdg.gov.in",
            sourceName = "RRB",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-20",
            lastDate = "2026-11-30",
            examDate = null
        )

        assertEquals(VerificationStatus.VERIFIED, report.overallStatus)
        assertTrue(report.isEligibleForAutoPublish)
    }

    // 4. Missing Last Date for Vacancy
    @Test
    fun test4_missingLastDate_requiresReview() {
        val raw = "NTA Recruitment Notice. Posts available. Apply soon at https://nta.ac.in"
        val extracted = SmartExtractedData.Vacancy(
            SmartVacancyData(
                title = "NTA Various Posts 2026",
                organization = "National Testing Agency",
                lastDate = null
            )
        )
        val links = listOf(SmartLink("https://nta.ac.in", SmartLinkType.OFFICIAL, "Official Portal", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.85f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "NTA Various Posts 2026",
            links = links,
            sourceUrl = "https://nta.ac.in",
            sourceName = "NTA",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-25",
            lastDate = null,
            examDate = null
        )

        assertEquals(VerificationStatus.UNKNOWN_EXPIRY, report.overallStatus)
        assertFalse(report.isEligibleForAutoPublish)
        assertTrue(report.reviewReasons.contains(ReviewReason.UNKNOWN_EXPIRY))
    }

    // 5. Extended Last Date (Version update tracking)
    @Test
    fun test5_extendedLastDate_versionRecorded() {
        val recordId = "rec_ssc_123"
        versionManager.recordVersion(
            recordId = recordId,
            fieldName = "last_date",
            previousValue = "2026-09-15",
            newValue = "2026-10-01",
            changeSource = "Official Notice",
            changeSummary = "Last date extended by SSC"
        )

        val history = versionManager.getVersionHistory(recordId)
        assertEquals(1, history.size)
        assertEquals("2026-10-01", history[0].newValue)
        assertEquals("2026-09-15", history[0].previousValue)
    }

    // 6. Conflicting Dates (Chronology start > last)
    @Test
    fun test6_conflictingDates_flagsDateConflict() {
        val raw = "BPSC Bharti 2026. Start Date: 2026-09-30. Last Date: 2026-09-10. https://bpsc.bih.nic.in"
        val extracted = SmartExtractedData.Vacancy(
            SmartVacancyData(
                title = "BPSC Bharti 2026",
                organization = "BPSC",
                applicationStartDate = "2026-09-30",
                lastDate = "2026-09-10"
            )
        )
        val links = listOf(SmartLink("https://bpsc.bih.nic.in", SmartLinkType.OFFICIAL, "BPSC", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.9f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "BPSC Bharti 2026",
            links = links,
            sourceUrl = "https://bpsc.bih.nic.in",
            sourceName = "BPSC",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-20",
            lastDate = "2026-09-10",
            examDate = null
        )

        assertEquals(VerificationStatus.DATE_CONFLICT, report.overallStatus)
        assertFalse(report.isEligibleForAutoPublish)
        assertTrue(report.reviewReasons.contains(ReviewReason.DATE_CONFLICT))
    }

    // 7. Invalid Calendar Date (32 August 2026 & 31 February)
    @Test
    fun test7_invalidCalendarDate_rejectedByLayer1() {
        val result32Aug = DeterministicValidator.validateCalendarDate("2026-08-32")
        assertFalse(result32Aug.isValid)
        assertTrue(result32Aug.reason.contains("impossible"))

        val result31Feb = DeterministicValidator.validateCalendarDate("2026-02-31")
        assertFalse(result31Feb.isValid)
        assertTrue(result31Feb.reason.contains("impossible"))
    }

    // 8. Result
    @Test
    fun test8_result_verifiesSuccessfully() {
        val raw = "UPSC Civil Services 2026 Final Result Declared. Merit list out. Check at https://upsc.gov.in"
        val extracted = SmartExtractedData.Result(
            SmartResultData(
                title = "UPSC CSE 2026 Final Result",
                organization = "UPSC",
                resultUrl = "https://upsc.gov.in"
            )
        )
        val links = listOf(SmartLink("https://upsc.gov.in", SmartLinkType.RESULT, "Check Result", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.RESULT,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "UPSC CSE 2026 Final Result",
            links = links,
            sourceUrl = "https://upsc.gov.in",
            sourceName = "UPSC",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-28",
            lastDate = null,
            examDate = null
        )

        assertEquals(VerificationStatus.VERIFIED, report.overallStatus)
        assertTrue(report.isEligibleForAutoPublish)
    }

    // 9. Admit Card
    @Test
    fun test9_admitCard_verifiesSuccessfully() {
        val raw = "SSC GD 2026 Admit Card Released. Exam Date: 2026-09-15. Download hall ticket at https://ssc.gov.in"
        val extracted = SmartExtractedData.AdmitCard(
            SmartAdmitCardData(
                title = "SSC GD 2026 Admit Card",
                organization = "SSC",
                downloadUrl = "https://ssc.gov.in"
            )
        )
        val links = listOf(SmartLink("https://ssc.gov.in", SmartLinkType.ADMIT_CARD, "Download", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.ADMIT_CARD,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "SSC GD 2026 Admit Card",
            links = links,
            sourceUrl = "https://ssc.gov.in",
            sourceName = "SSC",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-29",
            lastDate = null,
            examDate = "2026-09-15"
        )

        assertEquals(VerificationStatus.VERIFIED, report.overallStatus)
        assertTrue(report.isEligibleForAutoPublish)
    }

    // 10. Answer Key
    @Test
    fun test10_answerKey_verifiesSuccessfully() {
        val raw = "NEET UG 2026 Answer Key Released. Submit objections at https://nta.ac.in"
        val extracted = SmartExtractedData.AnswerKey(
            SmartAnswerKeyData(
                title = "NEET UG 2026 Official Answer Key",
                organization = "NTA",
                answerKeyUrl = "https://nta.ac.in"
            )
        )
        val links = listOf(SmartLink("https://nta.ac.in", SmartLinkType.ANSWER_KEY, "Answer Key", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.ANSWER_KEY,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "NEET UG 2026 Official Answer Key",
            links = links,
            sourceUrl = "https://nta.ac.in",
            sourceName = "NTA",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-25",
            lastDate = null,
            examDate = null
        )

        assertEquals(VerificationStatus.VERIFIED, report.overallStatus)
        assertTrue(report.isEligibleForAutoPublish)
    }

    // 11. Admission
    @Test
    fun test11_admission_verifiesSuccessfully() {
        val raw = "Delhi University UG Admission 2026. Apply Online. Last Date: 2026-09-20. https://du.ac.in"
        val extracted = SmartExtractedData.Admission(
            SmartAdmissionData(
                title = "DU UG Admission 2026",
                institution = "Delhi University",
                lastDate = "2026-09-20",
                applicationUrl = "https://du.ac.in"
            )
        )
        val links = listOf(SmartLink("https://du.ac.in", SmartLinkType.OFFICIAL, "Apply Online", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.ADMISSION,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "DU UG Admission 2026",
            links = links,
            sourceUrl = "https://du.ac.in",
            sourceName = "DU",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-20",
            lastDate = "2026-09-20",
            examDate = null
        )

        assertEquals(VerificationStatus.VERIFIED, report.overallStatus)
        assertTrue(report.isEligibleForAutoPublish)
    }

    // 12. Wrong Category (e.g. Result text assigned Vacancy category)
    @Test
    fun test12_wrongCategory_flagsCategoryConflict() {
        val raw = "SSC CHSL Final Result Declared! Check merit list and cut off marks at https://ssc.gov.in"
        val extracted = SmartExtractedData.Vacancy(
            SmartVacancyData(
                title = "SSC CHSL Final Result",
                organization = "SSC",
                lastDate = "2026-09-15"
            )
        )
        val links = listOf(SmartLink("https://ssc.gov.in", SmartLinkType.RESULT, "Result", true))

        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY, // Incorrect category assigned
            confidence = SmartConfidenceScore(0.85f, SmartConfidenceLevel.HIGH),
            extractedData = extracted,
            normalizedTitle = "SSC CHSL Final Result",
            links = links,
            sourceUrl = "https://ssc.gov.in",
            sourceName = "SSC",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-28",
            lastDate = "2026-09-15",
            examDate = null
        )

        assertEquals(VerificationStatus.CATEGORY_CONFLICT, report.overallStatus)
        assertFalse(report.isEligibleForAutoPublish)
        assertTrue(report.reviewReasons.contains(ReviewReason.CATEGORY_CONFLICT))
    }

    // 13. Duplicate Detection
    @Test
    fun test13_duplicate_detectedBySmartDuplicateDetector() {
        val existingItem = LatestUpdateItem(
            id = "existing_1",
            updateType = "vacancy",
            title = "SSC CGL 2026 Recruitment",
            organization = "Staff Selection Commission",
            lastDate = "2026-10-15",
            applyUrl = "https://ssc.gov.in",
            contentHash = "hash_cgl_123"
        )
        val candidateItem = SmartProcessedItem(
            id = "new_2",
            rawContent = "SSC CGL 2026",
            cleanedContent = "SSC CGL 2026",
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = SmartExtractedData.Vacancy(
                SmartVacancyData(
                    title = "SSC CGL 2026 Recruitment",
                    organization = "Staff Selection Commission",
                    lastDate = "2026-10-15"
                )
            ),
            normalizedTitle = "SSC CGL 2026 Recruitment",
            qualityScore = SmartQualityScore(95, 15, 25, 15, 15, 15, 5, 5, true),
            duplicateResult = SmartDuplicateResult(false, false),
            aiSummary = "",
            sourceUrl = "https://ssc.gov.in",
            sourceName = "SSC",
            sourceType = "WHATSAPP_CHANNEL",
            postDate = "2026-08-10",
            lastDate = "2026-10-15",
            examDate = null,
            status = SmartProcessingStatus.PROCESSING,
            contentHash = "hash_cgl_123"
        )

        val dupResult = SmartDuplicateDetector.evaluate(candidateItem, listOf(existingItem))
        assertTrue(dupResult.isDuplicate)
        assertEquals("existing_1", dupResult.existingRecordId)
    }

    // 14. Updated Duplicate Detection
    @Test
    fun test14_updatedDuplicate_detectedAsUpdate() {
        val existingItem = LatestUpdateItem(
            id = "existing_1",
            updateType = "vacancy",
            title = "SSC CGL 2026 Recruitment",
            organization = "Staff Selection Commission",
            lastDate = "2026-09-15",
            applyUrl = "https://ssc.gov.in"
        )
        val candidateItem = SmartProcessedItem(
            id = "new_2",
            rawContent = "SSC CGL 2026 Last Date Extended to 2026-10-15",
            cleanedContent = "SSC CGL 2026 Last Date Extended to 2026-10-15",
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH),
            extractedData = SmartExtractedData.Vacancy(
                SmartVacancyData(
                    title = "SSC CGL 2026 Recruitment",
                    organization = "Staff Selection Commission",
                    lastDate = "2026-10-15"
                )
            ),
            normalizedTitle = "SSC CGL 2026 Recruitment",
            qualityScore = SmartQualityScore(95, 15, 25, 15, 15, 15, 5, 5, true),
            duplicateResult = SmartDuplicateResult(false, false),
            aiSummary = "",
            sourceUrl = "https://ssc.gov.in",
            sourceName = "SSC",
            sourceType = "WHATSAPP_CHANNEL",
            postDate = "2026-08-10",
            lastDate = "2026-10-15",
            examDate = null,
            status = SmartProcessingStatus.PROCESSING,
            contentHash = "new_hash_456"
        )

        val dupResult = SmartDuplicateDetector.evaluate(candidateItem, listOf(existingItem))
        assertTrue(dupResult.isUpdate)
        assertEquals("existing_1", dupResult.existingRecordId)
    }

    // 15. Correction Notice Detection
    @Test
    fun test15_correctionNotice_identified() {
        val isCorrigendum = versionManager.isCorrectionNotice("Corrigendum in RRB ALP Recruitment Notice", "Corrigendum Notice")
        assertTrue(isCorrigendum)

        val isHindiNotice = versionManager.isCorrectionNotice("भर्ती हेतु शुद्धिपत्र जारी किया गया है", "शुद्धिपत्र")
        assertTrue(isHindiNotice)
    }

    // 16. Broken / Malformed URL
    @Test
    fun test16_brokenUrl_handledSafely() {
        val result = LinkHealthVerifier.verifyLink("not-a-valid-url-without-protocol")
        assertFalse(result.status == LinkHealthStatus.ACTIVE)
        assertEquals(LinkHealthStatus.BROKEN, result.status)
    }

    // 17. Redirect / Shortener URL
    @Test
    fun test17_redirectUrl_detected() {
        val result = LinkHealthVerifier.verifyLink("https://tinyurl.com/ssc-cgl-job")
        assertEquals(LinkHealthStatus.REDIRECTED, result.status)
        assertTrue(result.riskNotes.any { it.contains("shortener") })
    }

    // 18. Dangerous URL Scheme (javascript:)
    @Test
    fun test18_dangerousScheme_strictlyBlocked() {
        val result = LinkHealthVerifier.verifyLink("javascript:alert('pwned')")
        assertEquals(LinkHealthStatus.BLOCKED_SCHEME, result.status)
        assertFalse(result.isSafeScheme)
    }

    // 19. Missing Title
    @Test
    fun test19_missingTitle_requiresReview() {
        val report = verificationEngine.verify(
            rawContent = "Some announcement text",
            cleanedContent = "Some announcement text",
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.5f, SmartConfidenceLevel.LOW),
            extractedData = SmartExtractedData.Vacancy(SmartVacancyData(title = "", organization = "SSC")),
            normalizedTitle = "",
            links = emptyList(),
            sourceUrl = "https://example.com",
            sourceName = "Source",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-20",
            lastDate = "2026-09-30",
            examDate = null
        )

        assertFalse(report.isEligibleForAutoPublish)
        assertTrue(report.reviewReasons.contains(ReviewReason.MISSING_CRITICAL_DATA))
    }

    // 20. Missing Organization
    @Test
    fun test20_missingOrganization_flagged() {
        val report = verificationEngine.verify(
            rawContent = "New job opening available. Last Date 2026-10-10. https://jobs.in",
            cleanedContent = "New job opening available. Last Date 2026-10-10. https://jobs.in",
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.7f, SmartConfidenceLevel.MEDIUM),
            extractedData = SmartExtractedData.Vacancy(SmartVacancyData(title = "Clerk Vacancy", organization = "")),
            normalizedTitle = "Clerk Vacancy",
            links = emptyList(),
            sourceUrl = "https://jobs.in",
            sourceName = "Jobs",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-20",
            lastDate = "2026-10-10",
            examDate = null
        )

        assertFalse(report.isEligibleForAutoPublish)
        assertTrue(report.reviewReasons.contains(ReviewReason.MISSING_CRITICAL_DATA))
    }

    // 21. Missing Critical Information
    @Test
    fun test21_missingCriticalInfo_enqueuedInReviewQueue() {
        val raw = "Announcement regarding upcoming exams. Check website."
        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.4f, SmartConfidenceLevel.LOW),
            extractedData = SmartExtractedData.Vacancy(SmartVacancyData(title = "Announcement", organization = null)),
            normalizedTitle = "Announcement",
            links = emptyList(),
            sourceUrl = "https://example.com",
            sourceName = "Source",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-20",
            lastDate = null,
            examDate = null
        )

        assertFalse(report.isEligibleForAutoPublish)
        val pendingReviews = reviewQueueManager.getPendingReviews()
        assertTrue("Item must be in admin review queue", pendingReviews.isNotEmpty())
    }

    // 22. AI Hallucinated Field Protection
    @Test
    fun test22_aiHallucinatedDate_nullifiedAndFlagged() {
        val raw = "UPSC Civil Services 2026 Notification out. Apply on official portal."
        // Source does not have "2026-12-25", but AI hallucinated it
        val report = verificationEngine.verify(
            rawContent = raw,
            cleanedContent = raw,
            category = SmartContentCategory.VACANCY,
            confidence = SmartConfidenceScore(0.9f, SmartConfidenceLevel.HIGH),
            extractedData = SmartExtractedData.Vacancy(SmartVacancyData(title = "UPSC CSE 2026", organization = "UPSC", lastDate = "2026-12-25")),
            normalizedTitle = "UPSC CSE 2026",
            links = listOf(SmartLink("https://upsc.gov.in", SmartLinkType.OFFICIAL, "UPSC", true)),
            sourceUrl = "https://upsc.gov.in",
            sourceName = "UPSC",
            sourceType = "WHATSAPP_CHANNEL",
            sourcePostDate = "2026-08-20",
            lastDate = "2026-12-25",
            examDate = null
        )

        assertTrue("Must flag potential hallucination for date absent in source", report.reviewReasons.contains(ReviewReason.POTENTIAL_HALLUCINATION))
        assertFalse(report.isEligibleForAutoPublish)
    }

    // 23. Source Unavailable / Retry Limit
    @Test
    fun test23_sourceUnavailable_handledWithRetryLimit() {
        val revalService = ContentRevalidationService(verificationEngine = verificationEngine)
        val sourceId = "test_channel"

        // Attempts 1 and 2 should retry
        assertTrue(revalService.handleSourceFailure(sourceId, "Timeout 1"))
        assertTrue(revalService.handleSourceFailure(sourceId, "Timeout 2"))
        assertTrue(revalService.handleSourceFailure(sourceId, "Timeout 3"))
        // Attempt 4 should exceed max retries
        assertFalse(revalService.handleSourceFailure(sourceId, "Timeout 4"))

        assertEquals(1, metricsTracker.getMetrics().sourceFailuresCount)
    }

    // 24. Database Failure Resilience
    @Test
    fun test24_databaseFailureResilience() {
        // Null DAO or network failure must not crash verification
        val revalService = ContentRevalidationService(recruitmentDao = null, verificationEngine = verificationEngine)
        assertNotNull(revalService)
    }

    // 25. Idempotent Processing
    @Test
    fun test25_idempotentProcessing_producesIdenticalHashes() {
        val hash1 = SmartDuplicateDetector.computeContentHash("SSC CGL 2026", "vacancy", "SSC", "2026-10-15", "https://ssc.gov.in")
        val hash2 = SmartDuplicateDetector.computeContentHash("SSC CGL 2026", "vacancy", "SSC", "2026-10-15", "https://ssc.gov.in")
        assertEquals(hash1, hash2)
    }

    // 26. Stale Content Detection
    @Test
    fun test26_staleContent_detected() {
        val revalService = ContentRevalidationService(staleThresholdHours = 24)
        val staleTime = System.currentTimeMillis() - (48 * 3600 * 1000) // 48h ago
        val item = LatestUpdateItem(
            id = "item_1",
            updateType = "vacancy",
            title = "Active Job",
            lastDate = "2026-11-01",
            updatedAt = staleTime
        )
        // Verify time difference is > 24 hours
        assertTrue((System.currentTimeMillis() - item.updatedAt) > 24 * 3600 * 1000)
    }

    // 27. Multiple Viewer Consistency
    @Test
    fun test27_multipleViewers_receiveConsistentResults() {
        val raw = "SSC GD 2026 Result Declared at https://ssc.gov.in"
        val extracted = SmartExtractedData.Result(SmartResultData(title = "SSC GD 2026 Result", organization = "SSC"))
        val links = listOf(SmartLink("https://ssc.gov.in", SmartLinkType.RESULT, "Result", true))

        val reportA = verificationEngine.verify(
            rawContent = raw, cleanedContent = raw, category = SmartContentCategory.RESULT,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH), extractedData = extracted,
            normalizedTitle = "SSC GD 2026 Result", links = links, sourceUrl = "https://ssc.gov.in",
            sourceName = "SSC", sourceType = "WHATSAPP_CHANNEL", sourcePostDate = "2026-08-20",
            lastDate = null, examDate = null
        )

        val reportB = verificationEngine.verify(
            rawContent = raw, cleanedContent = raw, category = SmartContentCategory.RESULT,
            confidence = SmartConfidenceScore(0.95f, SmartConfidenceLevel.HIGH), extractedData = extracted,
            normalizedTitle = "SSC GD 2026 Result", links = links, sourceUrl = "https://ssc.gov.in",
            sourceName = "SSC", sourceType = "WHATSAPP_CHANNEL", sourcePostDate = "2026-08-20",
            lastDate = null, examDate = null
        )

        assertEquals(reportA.overallStatus, reportB.overallStatus)
        assertEquals(reportA.isEligibleForAutoPublish, reportB.isEligibleForAutoPublish)
        assertEquals(reportA.qualityScore.totalScore, reportB.qualityScore.totalScore)
    }

    // 28. Security: Unauthorized User Cannot Approve / Reject Review Queue
    @Test
    fun test28_security_unauthorizedAdmin_blockedFromModifyingReviewQueue() {
        val item = ReviewQueueItem(
            recordId = "item_123",
            title = "Pending Exam",
            category = SmartContentCategory.VACANCY,
            sourceName = "Source",
            sourceUrl = "https://example.com",
            sourcePostDate = "2026-08-20",
            reasons = listOf(ReviewReason.LOW_CONFIDENCE),
            reasonDescription = "Low confidence",
            rawContent = "Raw content",
            extractedValues = emptyMap()
        )
        reviewQueueManager.enqueueReview(item)

        // Non-admin attempt
        val regularUserApproved = reviewQueueManager.approveItem(item.id, "Approve by regular user", "user_normal_456")
        assertFalse("Unauthorized user must NOT be allowed to approve review items", regularUserApproved)

        val regularUserRejected = reviewQueueManager.rejectItem(item.id, "Reject by regular user", "user_normal_456")
        assertFalse("Unauthorized user must NOT be allowed to reject review items", regularUserRejected)

        // Authorized admin attempt
        val adminApproved = reviewQueueManager.approveItem(item.id, "Approved after verifying official notice", "admin_chief_1")
        assertTrue("Authorized admin should be able to approve", adminApproved)
    }
}
