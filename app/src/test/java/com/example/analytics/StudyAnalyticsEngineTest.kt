package com.example.analytics

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StudyMateDatabase
import com.example.data.model.InsightConfidence
import com.example.data.model.StudyEventType
import com.example.service.analytics.StudyAnalyticsEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyAnalyticsEngineTest {

    private lateinit var database: StudyMateDatabase
    private lateinit var analyticsEngine: StudyAnalyticsEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudyMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        analyticsEngine = StudyAnalyticsEngine(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testEventLoggingAndDailySummary() = runBlocking {
        analyticsEngine.logEvent(
            eventType = StudyEventType.FOCUS_COMPLETED,
            subject = "Mathematics",
            plannedDurationMinutes = 60,
            actualDurationMinutes = 60
        )
        analyticsEngine.logEvent(
            eventType = StudyEventType.FOCUS_COMPLETED,
            subject = "Physics",
            plannedDurationMinutes = 40,
            actualDurationMinutes = 40
        )

        val daily = analyticsEngine.getDailyAnalytics()
        assertEquals(100, daily.totalStudyMinutes)
        assertEquals(2, daily.completedSessions)
        assertEquals(60, daily.subjectBreakdown["Mathematics"])
        assertEquals(40, daily.subjectBreakdown["Physics"])
        assertEquals(100, daily.focusCompletionRate)
    }

    @Test
    fun testPlannedVsActualAccuracy() = runBlocking {
        analyticsEngine.logEvent(
            eventType = StudyEventType.FOCUS_COMPLETED,
            subject = "Chemistry",
            plannedDurationMinutes = 60,
            actualDurationMinutes = 45
        )

        val weekly = analyticsEngine.getWeeklyAnalytics()
        assertEquals(60, weekly.plannedMinutes)
        assertEquals(45, weekly.actualMinutes)
        assertEquals(15, weekly.uncompletedScheduledMinutes)
    }

    @Test
    fun testStreakCalculation() = runBlocking {
        // Log 30 mins study
        analyticsEngine.logEvent(
            eventType = StudyEventType.FOCUS_COMPLETED,
            subject = "Mathematics",
            actualDurationMinutes = 30
        )

        val streak = analyticsEngine.getStreakInfo()
        assertEquals(1, streak.currentStreak)
        assertTrue(streak.isQualifiedToday)
    }

    @Test
    fun testMinimumDataThresholdForInsights() = runBlocking {
        // Only 1 session
        analyticsEngine.logEvent(
            eventType = StudyEventType.FOCUS_COMPLETED,
            subject = "Physics",
            actualDurationMinutes = 25
        )

        val insights = analyticsEngine.getSmartInsights()
        val infoInsight = insights.find { it.confidence == InsightConfidence.INSUFFICIENT_DATA }
        assertNotNull("Should emit insufficient data notice when < 3 sessions exist", infoInsight)
    }

    @Test
    fun testNovaAnalyticsQuery() = runBlocking {
        analyticsEngine.logEvent(
            eventType = StudyEventType.FOCUS_COMPLETED,
            subject = "Mathematics",
            actualDurationMinutes = 50
        )

        val answer = analyticsEngine.getNovaAnalyticsAnswer("aaj kitna padha")
        assertTrue(answer.contains("50m") || answer.contains("Mathematics"))
    }
}
