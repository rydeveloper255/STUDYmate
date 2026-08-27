package com.example.notification

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Step 55: Unit Tests for Smart Notification & Personalization Engine 2.0
 */
class SmartNotificationPipelineTest {

    @Test
    fun testRelevanceScore_TargetExamAndStateMatch() {
        val userProfile = UserProfile(
            name = "Aarav",
            examName = "Railway RRB ALP"
        )
        val vacancyItem = RecruitmentEntity(
            id = "rrb_alp_2026",
            title = "RRB ALP 2026",
            organization = "Railway Recruitment Board",
            postName = "Assistant Loco Pilot",
            examCategory = "RAILWAY",
            state = "All India",
            rawStatus = VacancyStatus.OPEN.name
        )

        val score = SmartRelevanceEngine.calculateVacancyRelevance(
            item = vacancyItem,
            userProfile = userProfile,
            isTargetExamOnly = false,
            isHomeStateOnly = false,
            mutedCategories = emptyList(),
            isSavedByUser = true
        )

        // Base 30 + Category match 35 + State match 25 + Saved 20 = 100 maxed at 100
        assertTrue("Relevance score should be very high (>= 80), actual=$score", score >= 80)
    }

    @Test
    fun testRelevanceScore_MutedCategoryReturnsZero() {
        val userProfile = UserProfile(
            name = "Pooja",
            examName = "SSC CGL"
        )
        val vacancyItem = RecruitmentEntity(
            id = "ibps_po_2026",
            title = "IBPS PO 2026",
            organization = "IBPS",
            postName = "Probationary Officer",
            examCategory = "BANKING",
            state = "All India"
        )

        val score = SmartRelevanceEngine.calculateVacancyRelevance(
            item = vacancyItem,
            userProfile = userProfile,
            isTargetExamOnly = false,
            isHomeStateOnly = false,
            mutedCategories = listOf("BANKING"),
            isSavedByUser = false
        )

        assertEquals("Muted category should return relevance score 0", 0, score)
    }

    @Test
    fun testRelevanceScore_TargetExamOnlyFilter() {
        val userProfile = UserProfile(
            name = "Rohan",
            examName = "UPSC CSE"
        )
        val nonMatchingItem = RecruitmentEntity(
            id = "ssc_cgl_2026",
            title = "SSC CGL 2026",
            organization = "Staff Selection Commission",
            postName = "Combined Graduate Level Posts",
            examCategory = "SSC",
            state = "All India"
        )

        val score = SmartRelevanceEngine.calculateVacancyRelevance(
            item = nonMatchingItem,
            userProfile = userProfile,
            isTargetExamOnly = true,
            isHomeStateOnly = false,
            mutedCategories = emptyList()
        )

        assertEquals("Non-matching exam when targetExamOnly is enabled should return 0", 0, score)
    }

    @Test
    fun testSafeVacancyNotification_ApplyNowOnlyWhenActive() {
        val openVacancy = RecruitmentEntity(
            id = "rrb_technician_2026",
            title = "RRB Technician Grade III",
            organization = "Indian Railways",
            postName = "Technician Grade III",
            examCategory = "RAILWAY",
            totalVacancies = 9144,
            rawStatus = VacancyStatus.OPEN.name,
            applicationStartDate = "2026-08-01",
            applicationLastDate = "2026-09-30"
        )

        val (titleHindi, msgHindi) = SmartRelevanceEngine.formatSafeVacancyNotification(
            item = openVacancy,
            isHindi = true
        )
        val (titleEng, msgEng) = SmartRelevanceEngine.formatSafeVacancyNotification(
            item = openVacancy,
            isHindi = false
        )

        assertTrue("Hindi notification for open vacancy should mention application is active", msgHindi.contains("आवेदन शुरू") || msgHindi.contains("अप्लाई"))
        assertTrue("English notification for open vacancy should say Apply now or Applications open", msgEng.contains("Applications open") || msgEng.contains("Apply"))

        // When vacancy is UPCOMING (not yet open)
        val upcomingVacancy = openVacancy.copy(
            rawStatus = VacancyStatus.COMING_SOON.name,
            applicationStartDate = "2026-10-01",
            applicationLastDate = "2026-11-01"
        )

        val (_, upcomingMsgHindi) = SmartRelevanceEngine.formatSafeVacancyNotification(
            item = upcomingVacancy,
            isHindi = true
        )
        val (_, upcomingMsgEng) = SmartRelevanceEngine.formatSafeVacancyNotification(
            item = upcomingVacancy,
            isHindi = false
        )

        assertFalse("Upcoming vacancy must NEVER say 'Apply now'", upcomingMsgEng.contains("Apply now"))
        assertTrue("Upcoming vacancy in English should mention notice or details", upcomingMsgEng.contains("notification released") || upcomingMsgEng.contains("StudyMate"))
        assertTrue("Upcoming vacancy in Hindi should mention notice / adhisochna", upcomingMsgHindi.contains("अधिसूचना") || upcomingMsgHindi.contains("StudyMate"))
    }

    @Test
    fun testDeadlineNotificationFormatting() {
        val item = RecruitmentEntity(
            id = "ssc_chsl_2026",
            title = "SSC CHSL Tier 1",
            organization = "SSC",
            postName = "Lower Division Clerk",
            applicationLastDate = "2026-04-10"
        )

        val (hindiTitle, hindiMsg) = SmartRelevanceEngine.formatDeadlineNotification(
            item = item,
            daysRemaining = 2,
            isHindi = true
        )

        assertTrue("Deadline notification title should have alert icon", hindiTitle.contains("⏰") || hindiTitle.contains("Reminder") || hindiTitle.contains("Last Day"))
        assertTrue("Deadline message in Hindi should mention days remaining", hindiMsg.contains("2 दिन शेष") || hindiMsg.contains("last date") || hindiMsg.contains("अंतिम तिथि"))
    }

    @Test
    fun testResultNotificationFormatting() {
        val item = RecruitmentEntity(
            id = "upsc_prelims_2026",
            title = "UPSC CSE Prelims 2026",
            organization = "UPSC",
            postName = "Civil Services Prelims",
            resultDate = "2026-06-20",
            rawStatus = VacancyStatus.OPEN.name
        )

        val (hindiTitle, hindiMsg) = SmartRelevanceEngine.formatResultNotification(
            item = item,
            isHindiPreferred = true
        )

        assertTrue("Result notification title in Hindi should mention Result", hindiTitle.contains("Result") || hindiTitle.contains("रिजल्ट"))
        assertTrue("Result message should invite checking details", hindiMsg.contains("result") || hindiMsg.contains("StudyMate"))
    }

    @Test
    fun testAdmitCardNotificationFormatting() {
        val item = RecruitmentEntity(
            id = "ctet_july_2026",
            title = "CTET July 2026",
            organization = "CBSE",
            postName = "CTET July Paper 1 & 2",
            admitCardDate = "2026-07-01",
            rawStatus = VacancyStatus.OPEN.name
        )

        val (hindiTitle, hindiMsg) = SmartRelevanceEngine.formatAdmitCardNotification(
            item = item,
            isHindiPreferred = true
        )

        assertTrue("Admit card title in Hindi should mention Admit Card", hindiTitle.contains("Admit Card") || hindiTitle.contains("एडमिट कार्ड"))
        assertTrue("Admit card message should mention admit card", hindiMsg.contains("admit card") || hindiMsg.contains("उपलब्ध"))
    }
}
