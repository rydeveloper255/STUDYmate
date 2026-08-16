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
}
