package com.example.service.focus

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.example.service.FocusSessionExecutionState
import com.example.service.FocusSessionPersistence
import com.example.service.PersistedFocusSession
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusProtectionEngineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FocusProtectionEngine.init(context)
        BlockingController.clearSessionSnapshot()
        FocusSessionPersistence.getInstance(context).clearActiveSession()
    }

    @Test
    fun `essential apps and system settings are never blocked by BlockingController`() {
        BlockingController.createSessionSnapshot(
            restrictedPackages = setOf("com.instagram.android", "com.android.settings", "com.android.phone"),
            isStrictMode = true
        )

        // Essential apps should be immune
        val settingsDecision = BlockingController.evaluateBlockDecision(
            packageName = "com.android.settings",
            currentPackage = context.packageName,
            sessionState = FocusSessionState.ACTIVE
        )
        assertEquals(BlockDecision.ALLOW, settingsDecision)

        val phoneDecision = BlockingController.evaluateBlockDecision(
            packageName = "com.android.phone",
            currentPackage = context.packageName,
            sessionState = FocusSessionState.ACTIVE
        )
        assertEquals(BlockDecision.ALLOW, phoneDecision)

        // StudyMate itself is never blocked
        val ownAppDecision = BlockingController.evaluateBlockDecision(
            packageName = context.packageName,
            currentPackage = context.packageName,
            sessionState = FocusSessionState.ACTIVE
        )
        assertEquals(BlockDecision.ALLOW, ownAppDecision)

        // Distracting app is blocked
        val instaDecision = BlockingController.evaluateBlockDecision(
            packageName = "com.instagram.android",
            currentPackage = context.packageName,
            sessionState = FocusSessionState.ACTIVE
        )
        assertEquals(BlockDecision.BLOCK, instaDecision)
    }

    @Test
    fun `payment and UPI apps are never blocked`() {
        BlockingController.createSessionSnapshot(
            restrictedPackages = setOf("net.one97.paytm", "com.phonepe.app", "com.google.android.apps.nbu.paisa.user"),
            isStrictMode = true
        )

        val paytmDecision = BlockingController.evaluateBlockDecision(
            packageName = "net.one97.paytm",
            currentPackage = context.packageName,
            sessionState = FocusSessionState.ACTIVE
        )
        assertEquals(BlockDecision.ALLOW, paytmDecision)

        val gpayDecision = BlockingController.evaluateBlockDecision(
            packageName = "com.google.android.apps.nbu.paisa.user",
            currentPackage = context.packageName,
            sessionState = FocusSessionState.ACTIVE
        )
        assertEquals(BlockDecision.ALLOW, gpayDecision)
    }

    @Test
    fun `snapshot immutability preserves block list during session`() {
        val initialRestricted = setOf("com.snapchat.android", "com.zhiliaoapp.musically")
        BlockingController.createSessionSnapshot(initialRestricted, isStrictMode = true)

        val snapshot = BlockingController.getActiveSnapshot()
        assertNotNull(snapshot)
        assertTrue(snapshot!!.restrictedPackages.contains("com.snapchat.android"))
        assertTrue(snapshot.isStrictMode)
    }

    @Test
    fun `session recovery restores active timestamp session`() {
        val now = System.currentTimeMillis()
        val session = PersistedFocusSession(
            sessionId = UUID.randomUUID().toString(),
            subject = "Mathematics",
            topic = "Calculus",
            examName = "JEE Advanced",
            sessionGoal = "Complete limits",
            planItemId = null,
            plannedDurationMinutes = 30,
            startedAtTimestamp = now,
            elapsedRealtimeStart = SystemClock.elapsedRealtime(),
            pausedAccumulatedSeconds = 0L,
            pauseStartTimestamp = 0L,
            isPaused = false,
            isStrictMode = true,
            isAutoStarted = false,
            state = FocusSessionExecutionState.FOCUS_ACTIVE,
            restrictedPackagesSnapshot = setOf("com.instagram.android")
        )

        val persistence = FocusSessionPersistence.getInstance(context)
        persistence.saveActiveSession(session)

        val recovery = SessionRecovery(context)
        val result = recovery.evaluateRecovery()

        assertTrue(result is SessionRecovery.RecoveryResult.Restored)
        val restored = result as SessionRecovery.RecoveryResult.Restored
        assertEquals("Mathematics", restored.session.subject)
        assertTrue(restored.remainingSeconds > 0)
    }

    @Test
    fun `expired session is cleaned up during recovery`() {
        // Session started 2 hours ago with 25 minutes planned duration
        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        val session = PersistedFocusSession(
            sessionId = UUID.randomUUID().toString(),
            subject = "Physics",
            topic = "Thermodynamics",
            examName = "NEET",
            sessionGoal = "Practice questions",
            planItemId = null,
            plannedDurationMinutes = 25,
            startedAtTimestamp = twoHoursAgo,
            elapsedRealtimeStart = SystemClock.elapsedRealtime() - (2 * 60 * 60 * 1000L),
            pausedAccumulatedSeconds = 0L,
            pauseStartTimestamp = 0L,
            isPaused = false,
            isStrictMode = false,
            isAutoStarted = false,
            state = FocusSessionExecutionState.FOCUS_ACTIVE,
            restrictedPackagesSnapshot = setOf("com.instagram.android")
        )

        val persistence = FocusSessionPersistence.getInstance(context)
        persistence.saveActiveSession(session)

        val recovery = SessionRecovery(context)
        val result = recovery.evaluateRecovery()

        assertTrue(result is SessionRecovery.RecoveryResult.Expired)
    }

    @Test
    fun `device compatibility layer detects OEM profile`() {
        val oem = DeviceCompatibilityLayer.detectOEM()
        assertNotNull(oem)
        val guidance = DeviceCompatibilityLayer.getOEMGuidance(context)
        assertNotNull(guidance)
    }
}
