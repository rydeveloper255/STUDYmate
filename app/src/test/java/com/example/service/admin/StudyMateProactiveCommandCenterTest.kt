package com.example.service.admin

import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.telegram.ErrorSeverity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyMateProactiveCommandCenterTest {

    @Test
    fun testMorningReportGeneration() = runBlocking {
        val report = StudyMateProactiveCommandCenter.generateMorningReport(
            db = null,
            sb = null,
            tgService = null,
            errorFingerprints = emptyMap()
        )
        assertNotNull(report)
        assertTrue(report.contains("STUDYMATE MORNING REPORT"))
        assertTrue(report.contains("USERS"))
        assertTrue(report.contains("CONTENT"))
        assertTrue(report.contains("ISSUES"))
        assertTrue(report.contains("SYSTEM"))
        assertTrue(report.contains("TOP INSIGHT"))
    }

    @Test
    fun testEveningSummaryGeneration() = runBlocking {
        val summary = StudyMateProactiveCommandCenter.generateEveningSummary(
            db = null,
            sb = null,
            tgService = null,
            errorFingerprints = emptyMap()
        )
        assertNotNull(summary)
        assertTrue(summary.contains("STUDYMATE DAILY SUMMARY"))
        assertTrue(summary.contains("Users:"))
        assertTrue(summary.contains("Errors:"))
        assertTrue(summary.contains("Feedback:"))
        assertTrue(summary.contains("System Health:"))
    }

    @Test
    fun testWeeklyReportGeneration() = runBlocking {
        val report = StudyMateProactiveCommandCenter.generateWeeklyReport(
            db = null,
            sb = null,
            tgService = null,
            errorFingerprints = emptyMap()
        )
        assertNotNull(report)
        assertTrue(report.contains("STUDYMATE WEEKLY REPORT"))
        assertTrue(report.contains("Users:"))
        assertTrue(report.contains("Activity & Features:"))
        assertTrue(report.contains("Error Summary:"))
    }

    @Test
    fun testAnalyticsReportGeneration() = runBlocking {
        val report = StudyMateProactiveCommandCenter.generateAnalyticsReport(
            db = null,
            sb = null,
            errorFingerprints = emptyMap()
        )
        assertNotNull(report)
        assertTrue(report.contains("STUDYMATE ANALYTICS & HEALTH"))
        assertTrue(report.contains("User Growth:"))
        assertTrue(report.contains("Feature Health Matrix:"))
    }

    @Test
    fun testFeatureHealthCalculation() {
        val errorStatsHealthy = StudyMateAiAdminAssistant.ErrorSummarySnapshot(
            totalOpenFingerprints = 1,
            criticalCount = 0,
            errorCount = 1,
            warningCount = 0,
            topAffectedFeatures = listOf("Practice" to 1),
            recentErrors = emptyList(),
            timestamp = "2026-08-30 08:00"
        )
        val feedbackStatsHealthy = StudyMateAiAdminAssistant.FeedbackSummarySnapshot(
            totalFeedback = 0,
            newCount = 0,
            reviewingCount = 0,
            highPriorityCount = 0,
            resolvedCount = 0,
            topReportedFeatures = emptyList(),
            recentFeedbackSummary = emptyList(),
            timestamp = "2026-08-30 08:00"
        )

        val healthList = StudyMateProactiveCommandCenter.calculateFeatureHealth(errorStatsHealthy, feedbackStatsHealthy)
        assertNotNull(healthList)
        assertTrue(healthList.isNotEmpty())
        val practiceHealth = healthList.find { it.featureName.contains("Practice") }
        assertNotNull(practiceHealth)
        assertEquals("🟢", practiceHealth!!.statusSymbol)
        assertEquals("HEALTHY", practiceHealth.healthLabel)

        // Moderate state test
        val feedbackStatsModerate = feedbackStatsHealthy.copy(
            topReportedFeatures = listOf("Practice" to 1)
        )
        val moderateHealthList = StudyMateProactiveCommandCenter.calculateFeatureHealth(errorStatsHealthy, feedbackStatsModerate)
        val practiceModerate = moderateHealthList.find { it.featureName.contains("Practice") }
        assertEquals("🟡", practiceModerate?.statusSymbol)
        assertEquals("MODERATE", practiceModerate?.healthLabel)
    }

    @Test
    fun testTwoStepSafeAdminActionExecutionAndAudit() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val adminChatId = "test_admin_12345"

        // Register authorized admin in test environment
        com.example.data.remote.telegram.TelegramBotConfig.setAdminChatId(context, adminChatId)

        val action = StudyMateProactiveCommandCenter.createPendingAction(
            actionType = StudyMateProactiveCommandCenter.AdminActionType.CLEAR_ERROR_THROTTLES,
            requesterChatId = adminChatId,
            description = "Clear Error Throttles Test",
            effect = "Flushes throttle window map"
        )

        assertNotNull(action.actionId)
        assertTrue(action.actionId.startsWith("ACT-"))

        // Execute action
        val result = StudyMateProactiveCommandCenter.executeConfirmedAction(
            actionId = action.actionId,
            adminChatId = adminChatId,
            context = context,
            db = null
        )

        assertTrue(result.contains("✅"))
        assertTrue(result.contains("cleared"))

        // Verify audit log
        val logs = StudyMateProactiveCommandCenter.getAuditLogs(5)
        assertTrue(logs.any { it.actionId == action.actionId })
    }

    @Test
    fun testServiceDownAndRecoveryTracking() {
        // Record service down
        StudyMateProactiveCommandCenter.recordServiceState(
            serviceName = "Mock Supabase",
            isUp = true
        )
        StudyMateProactiveCommandCenter.recordServiceState(
            serviceName = "Mock Supabase",
            isUp = false,
            errorDetails = "Connection timeout 504"
        )

        // Record service recovery
        StudyMateProactiveCommandCenter.recordServiceState(
            serviceName = "Mock Supabase",
            isUp = true
        )

        val botHealth = StudyMateProactiveCommandCenter.getBotHealthReport()
        assertNotNull(botHealth)
    }
}
