package com.example.service.intelligence

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.*
import com.example.viewmodel.ActiveTestState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust Local Persistence & Crash Recovery Store for Mock Test Sessions.
 * 
 * Ensures an active test can survive app backgrounding, configuration changes,
 * process death, crashes, and device reboots without answer corruption or lost time.
 */
class TestSessionPersistence(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "active_mock_test_session_store"
        private const val KEY_ACTIVE_SESSION = "active_session_json"
        private const val TAG = "TestSessionPersistence"
    }

    /**
     * Saves the current active test state to local persistent storage.
     */
    fun saveActiveSession(state: ActiveTestState) {
        if (!state.isTestInProgress || state.isCompleted || state.questions.isEmpty()) {
            clearActiveSession()
            return
        }

        try {
            val json = JSONObject()
            json.put("requestId", state.requestId)
            json.put("subject", state.subject)
            json.put("title", state.title)
            json.put("currentQuestionIndex", state.currentQuestionIndex)
            json.put("startedAtTimestamp", state.startedAtTimestamp)
            json.put("expiresAtTimestamp", state.expiresAtTimestamp)
            json.put("totalDurationSeconds", state.totalDurationSeconds)
            json.put("remainingSeconds", state.remainingSeconds)
            json.put("currentQuestionEnteredTimestamp", state.currentQuestionEnteredTimestamp)

            // Selected Answers Map (questionIndex -> optionIndex)
            val answersObj = JSONObject()
            state.selectedAnswers.forEach { (qIdx, optIdx) ->
                answersObj.put(qIdx.toString(), optIdx)
            }
            json.put("selectedAnswers", answersObj)

            // Marked For Review Set
            val markedArr = JSONArray()
            state.markedForReview.forEach { markedArr.put(it) }
            json.put("markedForReview", markedArr)

            // Visited Questions Set
            val visitedArr = JSONArray()
            state.visitedQuestions.forEach { visitedArr.put(it) }
            json.put("visitedQuestions", visitedArr)

            // Time Spent Map
            val timeSpentObj = JSONObject()
            state.timeSpentSeconds.forEach { (qIdx, secs) ->
                timeSpentObj.put(qIdx.toString(), secs)
            }
            json.put("timeSpentSeconds", timeSpentObj)

            // Config Object
            val configObj = JSONObject()
            configObj.put("examId", state.config.examId)
            configObj.put("exam", state.config.exam)
            configObj.put("testType", state.config.testType.name)
            configObj.put("questionSource", state.config.questionSource.name)
            configObj.put("subject", state.config.subject)
            configObj.put("chapter", state.config.chapter)
            configObj.put("topic", state.config.topic)
            configObj.put("difficulty", state.config.difficulty)
            configObj.put("language", state.config.language)
            configObj.put("questionCount", state.config.questionCount)
            configObj.put("timeLimitMinutes", state.config.timeLimitMinutes)
            json.put("config", configObj)

            // Questions Array
            val questionsArr = JSONArray()
            state.questions.forEach { q ->
                val qObj = JSONObject()
                qObj.put("id", q.id)
                qObj.put("questionText", q.questionText)
                
                val optsArr = JSONArray()
                q.options.forEach { optsArr.put(it) }
                qObj.put("options", optsArr)

                qObj.put("correctOptionIndex", q.correctOptionIndex)
                qObj.put("explanation", q.explanation)
                qObj.put("subject", q.subject)
                qObj.put("topic", q.topic)
                qObj.put("chapter", q.chapter)
                qObj.put("examName", q.examName)
                qObj.put("year", q.year)
                qObj.put("shift", q.shift)
                qObj.put("sourceReference", q.sourceReference)
                qObj.put("difficulty", q.difficulty)
                qObj.put("source", q.source.name)
                qObj.put("sourceLabel", q.sourceLabel)
                qObj.put("yearOrTag", q.yearOrTag)
                qObj.put("examId", q.examId)
                qObj.put("language", q.language)
                questionsArr.put(qObj)
            }
            json.put("questions", questionsArr)

            prefs.edit().putString(KEY_ACTIVE_SESSION, json.toString()).apply()
            Log.d(TAG, "Active test session saved successfully. Questions: ${state.questions.size}, Answers: ${state.selectedAnswers.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize active test session: ${e.message}", e)
        }
    }

    /**
     * Loads the stored active session if it exists and is still valid.
     */
    fun loadActiveSession(): ActiveTestState? {
        val rawJson = prefs.getString(KEY_ACTIVE_SESSION, null) ?: return null
        try {
            val json = JSONObject(rawJson)

            val requestId = json.optString("requestId", "")
            val subject = json.optString("subject", "All Subjects")
            val title = json.optString("title", "Mock Test")
            val currentQuestionIndex = json.optInt("currentQuestionIndex", 0)
            val startedAtTimestamp = json.optLong("startedAtTimestamp", System.currentTimeMillis())
            val expiresAtTimestamp = json.optLong("expiresAtTimestamp", System.currentTimeMillis() + 600000L)
            val totalDurationSeconds = json.optInt("totalDurationSeconds", 600)
            val remainingSeconds = json.optInt("remainingSeconds", 600)

            // Calculate exact remaining time from authoritative expiresAtTimestamp
            val now = System.currentTimeMillis()
            val recalculatedRemainingSeconds = if (expiresAtTimestamp > 0L) {
                ((expiresAtTimestamp - now) / 1000L).coerceAtLeast(0L).toInt()
            } else {
                remainingSeconds
            }

            // Parse Selected Answers
            val answersMap = mutableMapOf<Int, Int>()
            val answersObj = json.optJSONObject("selectedAnswers")
            answersObj?.keys()?.forEach { k ->
                val qIdx = k.toIntOrNull()
                val optIdx = answersObj.optInt(k, -1)
                if (qIdx != null && optIdx >= 0) {
                    answersMap[qIdx] = optIdx
                }
            }

            // Parse Marked For Review
            val markedSet = mutableSetOf<Int>()
            val markedArr = json.optJSONArray("markedForReview")
            if (markedArr != null) {
                for (i in 0 until markedArr.length()) {
                    markedSet.add(markedArr.getInt(i))
                }
            }

            // Parse Visited Questions
            val visitedSet = mutableSetOf<Int>()
            val visitedArr = json.optJSONArray("visitedQuestions")
            if (visitedArr != null) {
                for (i in 0 until visitedArr.length()) {
                    visitedSet.add(visitedArr.getInt(i))
                }
            }

            // Parse Time Spent Map
            val timeSpentMap = mutableMapOf<Int, Int>()
            val timeSpentObj = json.optJSONObject("timeSpentSeconds")
            timeSpentObj?.keys()?.forEach { k ->
                val qIdx = k.toIntOrNull()
                val secs = timeSpentObj.optInt(k, 0)
                if (qIdx != null) {
                    timeSpentMap[qIdx] = secs
                }
            }

            // Parse Config
            val configObj = json.optJSONObject("config")
            val config = if (configObj != null) {
                val testTypeName = configObj.optString("testType", MockTestType.FULL_MOCK.name)
                val testTypeEnum = try { MockTestType.valueOf(testTypeName) } catch (e: Exception) { MockTestType.FULL_MOCK }
                val sourceType = configObj.optString("questionSource", QuestionSourceType.MIXED.name)
                val sourceEnum = try { QuestionSourceType.valueOf(sourceType) } catch (e: Exception) { QuestionSourceType.MIXED }

                MockTestConfig(
                    examId = configObj.optString("examId", "default_exam"),
                    exam = configObj.optString("exam", "Exam"),
                    testType = testTypeEnum,
                    questionSource = sourceEnum,
                    subject = configObj.optString("subject", "All Subjects"),
                    chapter = configObj.optString("chapter", "All Chapters"),
                    topic = configObj.optString("topic", "All Topics"),
                    difficulty = configObj.optString("difficulty", "Medium"),
                    language = configObj.optString("language", "English"),
                    questionCount = configObj.optInt("questionCount", 25),
                    timeLimitMinutes = configObj.optInt("timeLimitMinutes", 30)
                )
            } else {
                MockTestConfig()
            }

            // Parse Questions
            val questionsList = mutableListOf<Question>()
            val questionsArr = json.optJSONArray("questions")
            if (questionsArr != null) {
                for (i in 0 until questionsArr.length()) {
                    val qObj = questionsArr.getJSONObject(i)
                    val optsArr = qObj.getJSONArray("options")
                    val optsList = mutableListOf<String>()
                    for (j in 0 until optsArr.length()) {
                        optsList.add(optsArr.getString(j))
                    }

                    val sourceStr = qObj.optString("source", QuestionSource.AI_GENERATED.name)
                    val sourceEnum = try { QuestionSource.valueOf(sourceStr) } catch (e: Exception) { QuestionSource.AI_GENERATED }

                    val q = Question(
                        id = qObj.optString("id", "q_$i"),
                        questionText = qObj.optString("questionText", ""),
                        options = optsList,
                        correctOptionIndex = qObj.optInt("correctOptionIndex", 0),
                        explanation = qObj.optString("explanation", ""),
                        subject = qObj.optString("subject", "Physics"),
                        topic = qObj.optString("topic", "General"),
                        chapter = qObj.optString("chapter", ""),
                        examName = qObj.optString("examName", ""),
                        year = qObj.optString("year", ""),
                        shift = qObj.optString("shift", ""),
                        sourceReference = qObj.optString("sourceReference", ""),
                        difficulty = qObj.optString("difficulty", "Medium"),
                        source = sourceEnum,
                        sourceLabel = qObj.optString("sourceLabel", "Practice Question"),
                        yearOrTag = qObj.optString("yearOrTag", ""),
                        examId = qObj.optString("examId", ""),
                        language = qObj.optString("language", "English")
                    )
                    questionsList.add(q)
                }
            }

            if (questionsList.isEmpty()) {
                clearActiveSession()
                return null
            }

            val restoredState = ActiveTestState(
                isTestInProgress = true,
                requestId = requestId,
                subject = subject,
                title = title,
                questions = questionsList,
                currentQuestionIndex = currentQuestionIndex.coerceIn(0, questionsList.size - 1),
                selectedAnswers = answersMap,
                markedForReview = markedSet,
                visitedQuestions = visitedSet,
                timeSpentSeconds = timeSpentMap,
                startedAtTimestamp = startedAtTimestamp,
                expiresAtTimestamp = expiresAtTimestamp,
                totalDurationSeconds = totalDurationSeconds,
                remainingSeconds = recalculatedRemainingSeconds,
                currentQuestionEnteredTimestamp = now,
                isCompleted = false,
                completedAttempt = null,
                config = config
            )

            Log.d(TAG, "Restored active session with ${questionsList.size} questions, remainingSeconds=$recalculatedRemainingSeconds")
            return restoredState

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load active session from JSON: ${e.message}", e)
            clearActiveSession()
            return null
        }
    }

    /**
     * Clears the saved session from disk.
     */
    fun clearActiveSession() {
        prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
        Log.d(TAG, "Cleared stored active session")
    }
}
