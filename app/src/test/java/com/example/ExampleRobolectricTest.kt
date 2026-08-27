package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.FocusShieldManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    FocusShieldManager.init(context)
  }

  @Test
  fun `read app name string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("StudyMate AI", appName)
  }

  @Test
  fun `essential system apps are never restricted`() {
    assertFalse(FocusShieldManager.isAppRestricted("com.example"))
    assertFalse(FocusShieldManager.isAppRestricted("com.android.phone"))
    assertFalse(FocusShieldManager.isAppRestricted("com.android.settings"))
    assertFalse(FocusShieldManager.isAppRestricted("com.google.android.calculator"))
  }

  @Test
  fun `toggle and persist blocked apps`() {
    FocusShieldManager.setAppRestricted(context, "com.test.distractingapp", true)
    assertTrue(FocusShieldManager.getRestrictedPackages().contains("com.test.distractingapp"))

    FocusShieldManager.setAppRestricted(context, "com.test.distractingapp", false)
    assertFalse(FocusShieldManager.getRestrictedPackages().contains("com.test.distractingapp"))
  }

  @Test
  fun `select and deselect multiple blocked apps`() {
    val sampleApps = listOf("com.app.one", "com.app.two", "com.app.three")
    FocusShieldManager.selectAllApps(context, sampleApps)
    sampleApps.forEach {
      assertTrue(FocusShieldManager.getRestrictedPackages().contains(it))
    }

    FocusShieldManager.deselectAllApps(context, sampleApps)
    sampleApps.forEach {
      assertFalse(FocusShieldManager.getRestrictedPackages().contains(it))
    }
  }

  @Test
  fun `focus session lifecycle state`() {
    FocusShieldManager.startFocusSession(context, "Physics", "Thermodynamics", 30)
    assertTrue(FocusShieldManager.isSessionActive.value)
    assertEquals("Physics", FocusShieldManager.currentSubject.value)
    assertEquals("Thermodynamics", FocusShieldManager.currentTopic.value)
    assertEquals(30 * 60, FocusShieldManager.remainingSeconds.value)

    FocusShieldManager.updateRemainingTime(25 * 60)
    assertEquals(25 * 60, FocusShieldManager.remainingSeconds.value)

    FocusShieldManager.endFocusSession()
    assertFalse(FocusShieldManager.isSessionActive.value)
  }

  @Test
  fun `focus session persistence survives save and reload`() {
    val persistence = com.example.service.FocusSessionPersistence.getInstance(context)
    val session = com.example.service.PersistedFocusSession(
        sessionId = "test-session-123",
        subject = "Mathematics",
        topic = "Calculus",
        examName = "RRB NTPC",
        sessionGoal = "Calculus Mastery",
        planItemId = null,
        plannedDurationMinutes = 25,
        startedAtTimestamp = System.currentTimeMillis() - 60_000L, // started 1 min ago
        elapsedRealtimeStart = android.os.SystemClock.elapsedRealtime() - 60_000L,
        pausedAccumulatedSeconds = 0L,
        pauseStartTimestamp = 0L,
        isPaused = false,
        isStrictMode = false,
        isAutoStarted = false,
        state = com.example.service.FocusSessionExecutionState.FOCUS_ACTIVE,
        restrictedPackagesSnapshot = emptySet()
    )
    persistence.saveActiveSession(session)

    val reloaded = persistence.loadActiveSession()
    assertNotNull(reloaded)
    assertEquals("Mathematics", reloaded?.subject)
    assertEquals("Calculus", reloaded?.topic)
    assertEquals(25, reloaded?.plannedDurationMinutes)
    assertTrue(reloaded?.calculateRemainingSeconds() ?: 0 in (23 * 60)..(25 * 60))

    persistence.clearActiveSession()
    assertNull(persistence.loadActiveSession())
  }

  @Test
  fun `performance coach computes real analytics metrics`() {
    val profile = com.example.data.model.UserProfile(
        examName = "RRB Group D",
        streakDays = 5,
        dailyTargetMinutes = 120
    )
    val attempts = listOf(
        com.example.data.model.MockTestAttempt(
            id = 1L,
            title = "RRB NTPC Math Practice",
            examName = "RRB Group D",
            subject = "Mathematics",
            score = 15,
            totalQuestions = 20,
            accuracyPercent = 75f,
            timeSpentSeconds = 1200,
            timestamp = System.currentTimeMillis()
        ),
        com.example.data.model.MockTestAttempt(
            id = 2L,
            title = "RRB NTPC Science Practice",
            examName = "RRB Group D",
            subject = "General Science",
            score = 8,
            totalQuestions = 10,
            accuracyPercent = 80f,
            timeSpentSeconds = 600,
            timestamp = System.currentTimeMillis()
        )
    )
    val report = com.example.service.intelligence.PerformanceCoachEngine.computePerformanceReport(
        profile = profile,
        mockAttempts = attempts,
        mistakes = emptyList(),
        topicMasteries = emptyList(),
        focusSessions = emptyList(),
        plans = emptyList()
    )

    assertEquals(2, report.totalMocksTaken)
    assertEquals(30, report.totalQuestionsAttempted)
    assertTrue(report.overallAccuracyPercent in 75f..80f)
  }

  @Test
  fun `smart recruitment date normalization across formats`() {
    val scraper = com.example.service.intelligence.SmartRecruitmentScraperService()
    
    assertEquals("2026-09-10", scraper.normalizeDate("10/09/2026"))
    assertEquals("2026-09-10", scraper.normalizeDate("10-09-2026"))
    assertEquals("2026-09-10", scraper.normalizeDate("10 September 2026"))
    assertEquals("2026-09-10", scraper.normalizeDate("10 Sept 2026"))
    assertEquals("2026-09-10", scraper.normalizeDate("September 10, 2026"))
    assertEquals("2026-10-15", scraper.normalizeDate("15-10-2026"))
    assertNull(scraper.normalizeDate("invalid-date-string"))
  }

  @Test
  fun `active vacancy rule correctly separates open from closed vacancies`() {
    val pastDate = "2020-01-01"
    val futureDate = "2030-12-31"

    val daysPast = com.example.data.model.RecruitmentDateLogic.calculateDaysRemaining(pastDate)
    assertNotNull(daysPast)
    assertTrue(daysPast!! < 0)

    val daysFuture = com.example.data.model.RecruitmentDateLogic.calculateDaysRemaining(futureDate)
    assertNotNull(daysFuture)
    assertTrue(daysFuture!! > 0)

    val pastEntity = com.example.data.model.RecruitmentEntity(
        id = "expired_1",
        title = "Old Job 2020",
        organization = "Test Org",
        postName = "Clerk",
        examCategory = "SSC",
        state = "All India",
        contentType = com.example.data.model.RecruitmentContentType.VACANCY.name,
        rawStatus = "OPEN",
        applicationLastDate = pastDate
    )
    // getComputedStatus should evaluate past applicationLastDate to CLOSED
    assertEquals(com.example.data.model.VacancyStatus.CLOSED, pastEntity.getComputedStatus())
    assertFalse(pastEntity.getComputedStatus().isApplyActive)

    val futureEntity = com.example.data.model.RecruitmentEntity(
        id = "active_1",
        title = "Future Job 2030",
        organization = "Test Org",
        postName = "Clerk",
        examCategory = "SSC",
        state = "All India",
        contentType = com.example.data.model.RecruitmentContentType.VACANCY.name,
        rawStatus = "OPEN",
        applicationLastDate = futureDate
    )
    assertEquals(com.example.data.model.VacancyStatus.OPEN, futureEntity.getComputedStatus())
    assertTrue(futureEntity.getComputedStatus().isApplyActive)
  }

  @Test
  fun `recruitment category detection maps accurately`() {
    val scraper = com.example.service.intelligence.SmartRecruitmentScraperService()

    assertEquals(
        com.example.data.model.RecruitmentCategory.RAILWAY,
        scraper.detectCategory("RRB Assistant Loco Pilot ALP 2026 Recruitment")
    )
    assertEquals(
        com.example.data.model.RecruitmentCategory.SSC,
        scraper.detectCategory("SSC Combined Graduate Level CGL 2026 Online Form")
    )
    assertEquals(
        com.example.data.model.RecruitmentCategory.BANKING,
        scraper.detectCategory("IBPS PO Probationary Officer XIV Online Form")
    )
    assertEquals(
        com.example.data.model.RecruitmentCategory.DEFENCE,
        scraper.detectCategory("UPSC NDA & NA II Indian Army Defence Recruitment")
    )
    assertEquals(
        com.example.data.model.RecruitmentCategory.TEACHING,
        scraper.detectCategory("CTET Central Teacher Eligibility Test 2026")
    )
  }

  @Test
  fun `recruitment change detection identifies deadline extensions and results`() {
    val scraper = com.example.service.intelligence.SmartRecruitmentScraperService()

    val oldVacancy = com.example.data.model.RecruitmentEntity(
        id = "rrb_alp",
        title = "RRB ALP 2026",
        organization = "RRB",
        postName = "ALP",
        examCategory = "RAILWAY",
        state = "All India",
        contentType = com.example.data.model.RecruitmentContentType.VACANCY.name,
        applicationLastDate = "2026-09-01"
    )

    val extendedVacancy = oldVacancy.copy(
        applicationLastDate = "2026-10-15"
    )

    val diff = scraper.detectChanges(oldVacancy, extendedVacancy)
    assertEquals(
        com.example.service.intelligence.SmartRecruitmentScraperService.DetectedChangeType.DEADLINE_EXTENDED,
        diff.changeType
    )
    assertEquals("2026-09-01", diff.previousValue)
    assertEquals("2026-10-15", diff.newValue)
  }
}
