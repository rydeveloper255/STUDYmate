package com.example.service.whatsapp

import com.example.service.content.whatsapp.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.*

/**
 * Step 82: Comprehensive Unit Test Suite covering all 22 required verification scenarios:
 * 1. Valid post ingestion
 * 2. Post date before 1 August 2026 (cutoff rejection)
 * 3. Post date on 1 August 2026 (cutoff inclusion)
 * 4. Post date after 1 August 2026 (cutoff inclusion)
 * 5. Vacancy with future last date (Published / Active)
 * 6. Vacancy with passed last date (Expired / Inactive in feed)
 * 7. Result post category classification
 * 8. Admit Card post category classification
 * 9. Answer Key post category classification
 * 10. Admission post category classification
 * 11. Other/General post category classification
 * 12. Duplicate post detection
 * 13. Updated post handling (date modification)
 * 14. Missing title handling (Review Required)
 * 15. Missing date handling (Date Unavailable / Review Required)
 * 16. Valid link extraction & classification (Official, Apply, PDF)
 * 17. Invalid URL handling without crash
 * 18. Post with no links
 * 19. Network / Fetch failure handling (SOURCE_UNAVAILABLE)
 * 20. Corrupted payload / parsing safety
 * 21. Empty source payload handling
 * 22. Strict Zero Fake Data invariant check
 */
@RunWith(RobolectricTestRunner::class)
class WhatsAppIntegrationTest {

    private lateinit var ingestionService: WhatsAppChannelIngestionService

    @Before
    fun setUp() {
        WhatsAppSourceConfig.configuredChannelUrl = WhatsAppSourceConfig.DEFAULT_CHANNEL_URL
        WhatsAppSourceConfig.historicalCutoffDate = "2026-08-01"
        ingestionService = WhatsAppChannelIngestionService()
    }

    // 1. Valid post ingestion
    @Test
    fun testValidPostIngestion() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_101",
            sourcePostDate = "2026-08-15",
            rawText = """
                SSC CGL 2026 Recruitment Notification Out
                Organization: Staff Selection Commission (SSC)
                Total Posts: 14,500
                Apply Online Till: 2026-09-30
                Official Site: https://ssc.gov.in
                Apply Online: https://ssc.gov.in/apply
                Download PDF: https://ssc.gov.in/notices/cgl2026.pdf
            """.trimIndent()
        )

        val result = ingestionService.processRawPosts(listOf(rawPost))
        assertTrue("Processing should succeed", result.success)
        assertEquals(1, result.publishedCount)
        assertEquals(1, result.processedItems.size)

        val item = result.processedItems.first()
        assertEquals(WhatsAppCategory.VACANCY, item.category)
        assertEquals("Staff Selection Commission (SSC)", item.organization)
        assertEquals(14500, item.totalVacancies)
        assertEquals("2026-09-30", item.lastDate)
        assertEquals(WhatsAppIngestionStatus.PUBLISHED, item.status)
        assertTrue(item.isActive)
    }

    // 2. Post date before 1 August 2026 (cutoff rejection)
    @Test
    fun testPostDateBeforeCutoff_Ignored() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_old_1",
            sourcePostDate = "2026-07-31",
            rawText = "UPSC Civil Services 2026 Old Update\nLast Date: 2026-07-31"
        )

        val result = ingestionService.processRawPosts(listOf(rawPost))
        assertEquals(1, result.ignoredBeforeCutoffCount)
        assertEquals(0, result.publishedCount)
        assertEquals(0, result.processedItems.size)
    }

    // 3. Post date on 1 August 2026 (cutoff inclusion)
    @Test
    fun testPostDateOnCutoff_Included() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_cutoff_exact",
            sourcePostDate = "2026-08-01",
            rawText = "BPSC 70th Recruitment 2026 Notification\nLast Date: 2026-09-01"
        )

        val result = ingestionService.processRawPosts(listOf(rawPost))
        assertEquals(0, result.ignoredBeforeCutoffCount)
        assertEquals(1, result.publishedCount)
    }

    // 4. Post date after 1 August 2026 (cutoff inclusion)
    @Test
    fun testPostDateAfterCutoff_Included() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_future_post",
            sourcePostDate = "2026-09-10",
            rawText = "IBPS PO 2026 Recruitment\nLast Date: 2026-10-15"
        )

        val result = ingestionService.processRawPosts(listOf(rawPost))
        assertEquals(0, result.ignoredBeforeCutoffCount)
        assertEquals(1, result.publishedCount)
    }

    // 5. Vacancy with future last date (Published / Active)
    @Test
    fun testVacancyWithFutureLastDate_Active() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_active_vac",
            sourcePostDate = "2026-08-10",
            rawText = "RRB ALP 2026 Recruitment Online Form\nLast Date: 2026-12-31"
        )

        val result = ingestionService.processRawPosts(listOf(rawPost))
        assertEquals(1, result.publishedCount)
        assertEquals(0, result.expiredCount)
        val item = result.processedItems.first()
        assertEquals(WhatsAppIngestionStatus.PUBLISHED, item.status)
        assertTrue(item.isActive)
    }

    // 6. Vacancy with passed last date (Expired / Inactive in feed)
    @Test
    fun testVacancyWithPassedLastDate_Expired() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_expired_vac",
            sourcePostDate = "2026-08-02",
            rawText = "UPSSSC Lekhpal Bharti 2026\nLast Date: 2026-08-05"
        )

        // If today is past 2026-08-05:
        val result = ingestionService.processRawPosts(listOf(rawPost))
        val item = result.processedItems.firstOrNull()
        assertNotNull(item)
        assertEquals(WhatsAppIngestionStatus.EXPIRED, item?.status)
        assertFalse(item?.isActive ?: true)
        assertEquals(1, result.expiredCount)
    }

    // 7. Result post category classification
    @Test
    fun testResultClassification() {
        val rawText = "SSC CHSL 2026 Tier 1 Result Declared. Check Scorecard and Cutoff Marks."
        val category = WhatsAppCategoryDetector.detectCategory("SSC CHSL Result Out", rawText)
        assertEquals(WhatsAppCategory.RESULT, category)
    }

    // 8. Admit Card post category classification
    @Test
    fun testAdmitCardClassification() {
        val rawText = "UPSC CSE Prelims 2026 E-Admit Card & City Intimation Slip Released."
        val category = WhatsAppCategoryDetector.detectCategory("UPSC CSE Admit Card", rawText)
        assertEquals(WhatsAppCategory.ADMIT_CARD, category)
    }

    // 9. Answer Key post category classification
    @Test
    fun testAnswerKeyClassification() {
        val rawText = "NTA UGC NET June 2026 Provisional Answer Key and Response Sheet Released. Submit Objections."
        val category = WhatsAppCategoryDetector.detectCategory("UGC NET Answer Key", rawText)
        assertEquals(WhatsAppCategory.ANSWER_KEY, category)
    }

    // 10. Admission post category classification
    @Test
    fun testAdmissionClassification() {
        val rawText = "CUET UG 2026 Admission Counseling and Seat Allotment Round 1."
        val category = WhatsAppCategoryDetector.detectCategory("CUET UG Admission", rawText)
        assertEquals(WhatsAppCategory.ADMISSION, category)
    }

    // 11. Other/General post category classification
    @Test
    fun testOtherClassification() {
        val rawText = "Important Notice regarding examination center guidelines and weather advisory."
        val category = WhatsAppCategoryDetector.detectCategory("General Notice", rawText)
        assertEquals(WhatsAppCategory.OTHER, category)
    }

    // 12. Duplicate post detection
    @Test
    fun testDuplicatePostDetection() = runBlocking {
        val rawPost1 = WhatsAppRawPost(
            sourceMessageId = "msg_dup_1",
            sourcePostDate = "2026-08-10",
            rawText = "DSSSB Teacher Recruitment 2026\nLast Date: 2026-09-15"
        )
        val rawPost2 = WhatsAppRawPost(
            sourceMessageId = "msg_dup_2",
            sourcePostDate = "2026-08-10",
            rawText = "DSSSB Teacher Recruitment 2026\nLast Date: 2026-09-15"
        )

        val result = ingestionService.processRawPosts(listOf(rawPost1, rawPost2))
        assertEquals(1, result.publishedCount)
        assertEquals(1, result.duplicateCount)
    }

    // 13. Updated post handling (date extension)
    @Test
    fun testUpdatedPostHandling() = runBlocking {
        val rawPost1 = WhatsAppRawPost(
            sourceMessageId = "msg_update_orig",
            sourcePostDate = "2026-08-10",
            rawText = "SBI PO 2026 Recruitment\nLast Date: 2026-09-15"
        )
        val rawPost2 = WhatsAppRawPost(
            sourceMessageId = "msg_update_orig",
            sourcePostDate = "2026-08-10",
            rawText = "SBI PO 2026 Recruitment\nLast Date Extended: 2026-09-30"
        )

        val result1 = ingestionService.processRawPosts(listOf(rawPost1))
        assertEquals(1, result1.publishedCount)

        val result2 = ingestionService.processRawPosts(listOf(rawPost2))
        assertEquals(1, result2.updatedCount)
        assertEquals("2026-09-30", result2.processedItems.first().lastDate)
    }

    // 14. Missing title handling (Review Required)
    @Test
    fun testMissingTitle_ReviewRequired() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_empty",
            sourcePostDate = "2026-08-10",
            rawText = "   "
        )

        val result = ingestionService.processRawPosts(listOf(rawPost))
        assertEquals(1, result.reviewRequiredCount)
        assertEquals(0, result.publishedCount)
    }

    // 15. Missing date handling (Date Unavailable / Review Required)
    @Test
    fun testMissingDate_DateUnavailable() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_no_date",
            sourcePostDate = null,
            rawText = "Air Force Agniveer Vayu Recruitment 2026 Notification"
        )

        val result = ingestionService.processRawPosts(listOf(rawPost))
        assertEquals(1, result.reviewRequiredCount)
        assertEquals(0, result.publishedCount)
    }

    // 16. Valid link extraction & classification
    @Test
    fun testLinkExtractionAndClassification() {
        val text = """
            Official Site: https://rrbcdg.gov.in
            Apply Online: https://rrbapply.gov.in/login
            Notice PDF: https://rrbcdg.gov.in/notices/CEN012026.pdf
            Result Link: https://rrbcdg.gov.in/results/scorecard
        """.trimIndent()

        val links = WhatsAppContentExtractor.extractAndClassifyLinks(text, WhatsAppCategory.VACANCY)
        assertEquals(4, links.size)
        assertTrue(links.any { it.linkType == ExtractedLinkType.OFFICIAL_WEBSITE })
        assertTrue(links.any { it.linkType == ExtractedLinkType.APPLY_LINK })
        assertTrue(links.any { it.linkType == ExtractedLinkType.PDF_LINK })
        assertTrue(links.any { it.linkType == ExtractedLinkType.RESULT_LINK })
    }

    // 17. Invalid URL handling without crash
    @Test
    fun testInvalidUrlGracefulHandling() {
        val text = "Check website at htp://invalid_url and www.incomplete"
        val links = WhatsAppContentExtractor.extractAndClassifyLinks(text, WhatsAppCategory.VACANCY)
        // Should not crash and safely extract 0 valid http/https links
        assertNotNull(links)
    }

    // 18. Post with no links
    @Test
    fun testPostWithNoLinks() {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_no_links",
            sourcePostDate = "2026-08-10",
            rawText = "Delhi Police Constable 2026 Exam City Slip will be issued soon."
        )
        val extracted = WhatsAppContentExtractor.extractContent(rawPost)
        assertTrue(extracted.links.isEmpty())
        assertNull(extracted.officialUrl)
        assertNull(extracted.applyUrl)
    }

    // 19. Network / Fetch failure handling (SOURCE_UNAVAILABLE)
    @Test
    fun testFetchFailure_SourceUnavailable() = runBlocking {
        val unconfiguredFetcher = WhatsAppChannelFetcher()
        val res = ingestionService.runIngestionCycle(channelUrl = "https://invalid.domain.that.does.not.exist.internal/channel")
        assertFalse(res.success)
        assertEquals(WhatsAppIngestionStatus.SOURCE_UNAVAILABLE, res.status)
        assertEquals(0, res.publishedCount)
    }

    // 20. Corrupted payload / parsing safety
    @Test
    fun testCorruptedPayloadParsingSafety() = runBlocking {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_weird_chars",
            sourcePostDate = "2026-08-10",
            rawText = "%%%###@@@***\n\n\n\n\n\nUnknown ??? >>> <script>alert(1)</script>"
        )
        val res = ingestionService.processRawPosts(listOf(rawPost))
        assertNotNull(res)
    }

    // 21. Empty source payload handling
    @Test
    fun testEmptySourcePayload() = runBlocking {
        val res = ingestionService.processRawPosts(emptyList())
        assertTrue(res.success)
        assertEquals(0, res.totalRawPosts)
        assertEquals(0, res.publishedCount)
    }

    // 22. Strict Zero Fake Data invariant check
    @Test
    fun testZeroFakeDataInvariant() {
        val rawPost = WhatsAppRawPost(
            sourceMessageId = "msg_real_sparse",
            sourcePostDate = "2026-08-20",
            rawText = "High Court Assistant Examination 2026"
        )
        val extracted = WhatsAppContentExtractor.extractContent(rawPost)

        // Strict: Missing fields must remain null, NOT populated with fake placeholders!
        assertNull("Fee details should be null when not present", extracted.feeDetails)
        assertNull("Qualification should be null when not present", extracted.qualification)
        assertNull("Age criteria should be null when not present", extracted.ageCriteria)
        assertNull("Total vacancies should be null when not present", extracted.totalVacancies)
        assertNull("Last date should be null when not present", extracted.lastDate)
    }
}
