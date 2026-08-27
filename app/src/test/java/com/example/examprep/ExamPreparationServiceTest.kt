package com.example.examprep

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StudyMateDatabase
import com.example.data.model.StudyScheduleItem
import com.example.service.intelligence.ExamPreparationService
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
class ExamPreparationServiceTest {

    private lateinit var database: StudyMateDatabase
    private lateinit var examPrepService: ExamPreparationService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudyMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        examPrepService = ExamPreparationService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCreateExamGoalAndDefaultSyllabusSeeding() = runBlocking {
        val goal = examPrepService.createOrUpdateExamGoal(
            examName = "SSC CGL 2026",
            organization = "Staff Selection Commission",
            examDateMillis = System.currentTimeMillis() + (120L * 24 * 60 * 60 * 1000),
            isExamDateKnown = true,
            target = "85+ Marks",
            priority = "PRIMARY"
        )

        assertNotNull(goal)
        assertEquals("SSC CGL 2026", goal.examName)
        assertEquals("PRIMARY", goal.priority)

        val summary = examPrepService.getExamPreparationSummary(goal.examId)
        assertNotNull(summary)
        assertEquals("SSC CGL 2026", summary?.examGoal?.examName)
        assertTrue("Syllabus topics should be seeded", (summary?.totalTopicsCount ?: 0) > 0)
    }

    @Test
    fun testExamPreparationSummaryCalculation() = runBlocking {
        val goal = examPrepService.createOrUpdateExamGoal(
            examName = "RRB NTPC 2026",
            organization = "Railway Recruitment Board",
            examDateMillis = System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000),
            isExamDateKnown = true,
            target = "Qualifier",
            priority = "PRIMARY"
        )

        val initialSummary = examPrepService.getExamPreparationSummary(goal.examId)
        assertNotNull(initialSummary)
        val firstTopic = database.examPrepDao().getSyllabusTopicsForExamOnce(goal.examId).first()

        // Update topic to COMPLETED
        examPrepService.updateTopicStatus(firstTopic.topicId, "COMPLETED")

        val updatedSummary = examPrepService.getExamPreparationSummary(goal.examId)
        assertNotNull(updatedSummary)
        assertEquals(1, updatedSummary?.completedTopicsCount)
        assertTrue(updatedSummary?.syllabusCoveragePercentage!! > 0)
    }

    @Test
    fun testDailyPlanGenerationAndConflictDetection() = runBlocking {
        val goal = examPrepService.createOrUpdateExamGoal(
            examName = "UPSC Prelims 2026",
            organization = "UPSC",
            examDateMillis = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000),
            isExamDateKnown = true,
            target = "Prelims Pass",
            priority = "PRIMARY"
        )

        // Insert conflicting schedule item
        val conflictingSchedule = StudyScheduleItem(
            subject = "History",
            topic = "Ancient India",
            durationMinutes = 60,
            dayOfWeek = "MON",
            startTime = "18:00",
            endTime = "19:00",
            repeatType = "ONCE"
        )
        database.studyScheduleDao().insertOrUpdateScheduleItem(conflictingSchedule)

        val preview = examPrepService.generateDailyStudyPlanPreview(goal.examId, dailyAvailableMinutes = 180)
        assertNotNull(preview)
        assertTrue(preview.plannedItems.isNotEmpty())

        val confirmSuccess = examPrepService.confirmAndScheduleDailyPlan(preview)
        assertTrue(confirmSuccess)

        val allScheduleItems = database.studyScheduleDao().getAllScheduleItems()
        assertTrue(allScheduleItems.size > 1)
    }

    @Test
    fun testFocusSessionProgressSyncing() = runBlocking {
        val goal = examPrepService.createOrUpdateExamGoal(
            examName = "Bank PO 2026",
            organization = "IBPS",
            examDateMillis = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
            isExamDateKnown = true,
            target = "75+ Score",
            priority = "PRIMARY"
        )

        val topics = database.examPrepDao().getSyllabusTopicsForExamOnce(goal.examId)
        val quantTopic = topics.first { it.subjectName.contains("Quantitative", ignoreCase = true) || it.subjectName.contains("Math", ignoreCase = true) }

        examPrepService.recordFocusSessionStudyProgress(quantTopic.subjectName, quantTopic.topicName, 45, goal.examId)

        val updatedTopic = database.examPrepDao().getSyllabusTopicsForExamOnce(goal.examId).first { it.topicId == quantTopic.topicId }
        assertEquals(45, updatedTopic.studyTimeMinutes)
        assertEquals(1, updatedTopic.sessionsCount)
        assertNotNull(updatedTopic.lastStudiedAt)
    }

    @Test
    fun testNovaExamPrepAnswer() = runBlocking {
        examPrepService.createOrUpdateExamGoal(
            examName = "SSC CGL",
            organization = "SSC",
            examDateMillis = System.currentTimeMillis() + (100L * 24 * 60 * 60 * 1000),
            isExamDateKnown = true,
            target = "85+ Marks",
            priority = "PRIMARY"
        )

        val dateAnswer = examPrepService.getNovaExamPrepAnswer("Mera exam kab hai?")
        assertTrue(dateAnswer.answerText.contains("SSC CGL"))
        assertTrue(dateAnswer.actions.isNotEmpty())

        val syllabusAnswer = examPrepService.getNovaExamPrepAnswer("Kitna syllabus hua?")
        assertTrue(syllabusAnswer.answerText.contains("Syllabus Progress"))

        val planAnswer = examPrepService.getNovaExamPrepAnswer("Aaj ka plan bata")
        assertTrue(planAnswer.answerText.contains("Study Plan"))
    }
}
