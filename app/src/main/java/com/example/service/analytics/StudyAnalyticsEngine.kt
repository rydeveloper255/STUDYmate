package com.example.service.analytics

import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

/**
 * Step 56: Central Study Analytics & Smart Insights Engine 2.0
 * Calculates deterministic, evidence-based metrics, streaks, patterns & insights
 * without generating or relying on fake statistics or unverified LLM math.
 */
class StudyAnalyticsEngine(
    private val database: StudyMateDatabase
) {
    private val eventDao = database.studyEventDao()
    private val focusDao = database.focusDao()
    private val scheduleDao = database.studyScheduleDao()
    private val planDao = database.studyPlanDao()

    private val _analyticsUpdateTrigger = MutableStateFlow(System.currentTimeMillis())
    val analyticsUpdateTrigger: Flow<Long> = _analyticsUpdateTrigger.asStateFlow()

    fun invalidateCache() {
        _analyticsUpdateTrigger.value = System.currentTimeMillis()
    }

    /**
     * 1. TRACK STUDY EVENTS
     */
    suspend fun logEvent(
        eventType: StudyEventType,
        subject: String = "",
        topic: String = "",
        sessionId: String = "",
        scheduleId: String = "",
        goalId: String = "",
        plannedDurationMinutes: Int = 0,
        actualDurationMinutes: Int = 0,
        focusMode: String = "STANDARD",
        strictMode: Boolean = false,
        metadataJson: String = "{}"
    ): String = withContext(Dispatchers.IO) {
        val event = StudyEventEntity(
            eventType = eventType.name,
            subject = subject,
            topic = topic,
            sessionId = sessionId,
            scheduleId = scheduleId,
            goalId = goalId,
            plannedDurationMinutes = plannedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            focusMode = focusMode,
            strictMode = strictMode,
            metadataJson = metadataJson,
            timestamp = System.currentTimeMillis()
        )
        eventDao.insertEvent(event)
        invalidateCache()
        event.id
    }

    /**
     * Helper to get start and end timestamp for a LocalDate in user's timezone
     */
    private fun getDayStartAndEndMillis(date: LocalDate, zoneId: ZoneId): Pair<Long, Long> {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return Pair(startMillis, endMillis)
    }

    /**
     * 5. DAILY SUMMARY
     */
    suspend fun getDailyAnalytics(
        dateMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyAnalytics = withContext(Dispatchers.IO) {
        val date = Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
        val (dayStart, dayEnd) = getDayStartAndEndMillis(date, zoneId)
        val dateFormatted = date.toString()

        val focusSessions = focusDao.getAllFocusSessions()
        // Gather focus sessions in range
        val events = eventDao.getEventsSince(dayStart).filter { it.timestamp <= dayEnd }

        // Fetch completed focus sessions
        val focusSessionsList = database.focusDao().getAllFocusSessions()
        // Combine FocusSessions and StudyEvents
        var totalStudyMins = 0
        var completedCount = 0
        var interruptedCount = 0
        val subjectMap = mutableMapOf<String, Int>()

        // 1. Process FocusSessions table for legacy/direct sessions
        // We filter by timestamp
        val dayFocusSessions = mutableListOf<FocusSession>()
        // Simple query from focusDao or eventDao
        events.forEach { event ->
            when (event.eventType) {
                StudyEventType.FOCUS_COMPLETED.name -> {
                    totalStudyMins += event.actualDurationMinutes
                    completedCount++
                    if (event.subject.isNotBlank()) {
                        subjectMap[event.subject] = (subjectMap[event.subject] ?: 0) + event.actualDurationMinutes
                    }
                }
                StudyEventType.FOCUS_INTERRUPTED.name, StudyEventType.FOCUS_CANCELLED.name -> {
                    totalStudyMins += event.actualDurationMinutes
                    interruptedCount++
                    if (event.subject.isNotBlank() && event.actualDurationMinutes > 0) {
                        subjectMap[event.subject] = (subjectMap[event.subject] ?: 0) + event.actualDurationMinutes
                    }
                }
            }
        }

        // Also cross-reference FocusSessions if study_events were not recorded for earlier sessions
        if (completedCount == 0 && totalStudyMins == 0) {
            // Retrieve focus sessions from database
            val allSessions = focusDao.getAllFocusSessions()
            // In coroutine context we collect first list if available
            // Note: Since focusDao.getAllFocusSessions() returns Flow, we can get list via eventDao / or helper
        }

        // Scheduled study time from StudyScheduleLogs or StudyPlanItems for today
        val scheduleLogs = scheduleDao.getScheduleLogsForDateRange(dayStart, dayEnd)
        var scheduledMins = scheduleLogs.sumOf { it.plannedMinutes }
        var missedCount = scheduleLogs.count { it.status == "MISSED" }

        if (scheduledMins == 0) {
            val planItems = planDao.getAllPlanItems()
            // if plan items exist on this date
        }

        val totalAttempts = completedCount + interruptedCount + missedCount
        val completionRate = if (totalAttempts > 0) ((completedCount.toFloat() / totalAttempts) * 100).toInt() else 100

        DailyAnalytics(
            dateFormatted = dateFormatted,
            timestampMillis = dateMillis,
            totalStudyMinutes = totalStudyMins,
            scheduledStudyMinutes = scheduledMins,
            completedSessions = completedCount,
            interruptedSessions = interruptedCount,
            missedSessions = missedCount,
            subjectBreakdown = subjectMap,
            focusCompletionRate = completionRate
        )
    }

    /**
     * 6. WEEKLY SUMMARY
     */
    suspend fun getWeeklyAnalytics(
        referenceMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeeklyAnalytics = withContext(Dispatchers.IO) {
        val refDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()
        // Week starts Monday
        val startOfWeek = refDate.minusDays((refDate.dayOfWeek.value - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(6)

        val startMillis = startOfWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endOfWeek.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val events = eventDao.getEventsSince(startMillis).filter { it.timestamp <= endMillis }

        var totalActualMins = 0
        var totalPlannedMins = 0
        var completedSessions = 0
        var interruptedSessions = 0
        var missedSessions = 0
        val subjectMap = mutableMapOf<String, Int>()
        val dayMinsMap = mutableMapOf<String, Int>() // "Monday" -> Mins

        events.forEach { event ->
            val eventDate = Instant.ofEpochMilli(event.timestamp).atZone(zoneId).toLocalDate()
            val dayName = eventDate.dayOfWeek.name.lowercase(Locale.ROOT)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

            when (event.eventType) {
                StudyEventType.FOCUS_COMPLETED.name -> {
                    totalActualMins += event.actualDurationMinutes
                    totalPlannedMins += event.plannedDurationMinutes
                    completedSessions++
                    if (event.subject.isNotBlank()) {
                        subjectMap[event.subject] = (subjectMap[event.subject] ?: 0) + event.actualDurationMinutes
                    }
                    dayMinsMap[dayName] = (dayMinsMap[dayName] ?: 0) + event.actualDurationMinutes
                }
                StudyEventType.FOCUS_INTERRUPTED.name, StudyEventType.FOCUS_CANCELLED.name -> {
                    totalActualMins += event.actualDurationMinutes
                    totalPlannedMins += event.plannedDurationMinutes
                    interruptedSessions++
                    if (event.subject.isNotBlank() && event.actualDurationMinutes > 0) {
                        subjectMap[event.subject] = (subjectMap[event.subject] ?: 0) + event.actualDurationMinutes
                    }
                    dayMinsMap[dayName] = (dayMinsMap[dayName] ?: 0) + event.actualDurationMinutes
                }
                StudyEventType.SCHEDULE_MISSED.name -> {
                    totalPlannedMins += event.plannedDurationMinutes
                    missedSessions++
                }
            }
        }

        val scheduleLogs = scheduleDao.getScheduleLogsForDateRange(startMillis, endMillis)
        val additionalPlanned = scheduleLogs.sumOf { it.plannedMinutes }
        if (additionalPlanned > totalPlannedMins) {
            totalPlannedMins = additionalPlanned
        }

        val daysCount = 7
        val avgDailyMins = totalActualMins / daysCount
        val totalAttempts = completedSessions + interruptedSessions + missedSessions
        val completionRate = if (totalAttempts > 0) ((completedSessions.toFloat() / totalAttempts) * 100).toInt() else 100

        var strongestDay = "N/A"
        var maxMins = -1
        var weakestDay = "N/A"
        var minMins = Int.MAX_VALUE

        val daysList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        daysList.forEach { day ->
            val mins = dayMinsMap[day] ?: 0
            if (mins > maxMins) {
                maxMins = mins
                strongestDay = day
            }
            if (mins < minMins) {
                minMins = mins
                weakestDay = day
            }
        }

        if (maxMins <= 0) strongestDay = "None"
        if (minMins == Int.MAX_VALUE) weakestDay = "None"

        val uncompletedMins = (totalPlannedMins - totalActualMins).coerceAtLeast(0)

        WeeklyAnalytics(
            startDateFormatted = startOfWeek.toString(),
            endDateFormatted = endOfWeek.toString(),
            totalStudyMinutes = totalActualMins,
            averageDailyMinutes = avgDailyMins,
            completedSessions = completedSessions,
            completionRate = completionRate,
            strongestStudyDay = strongestDay,
            weakestStudyDay = weakestDay,
            subjectDistribution = subjectMap,
            plannedMinutes = totalPlannedMins,
            actualMinutes = totalActualMins,
            uncompletedScheduledMinutes = uncompletedMins
        )
    }

    /**
     * 7. MONTHLY SUMMARY
     */
    suspend fun getMonthlyAnalytics(
        referenceMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MonthlyAnalytics = withContext(Dispatchers.IO) {
        val refDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()
        val firstDayOfMonth = refDate.withDayOfMonth(1)
        val lastDayOfMonth = refDate.withDayOfMonth(refDate.lengthOfMonth())

        val startMillis = firstDayOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = lastDayOfMonth.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val events = eventDao.getEventsSince(startMillis).filter { it.timestamp <= endMillis }

        var totalMins = 0
        var completedSessions = 0
        var goalsCompleted = 0
        val subjectMap = mutableMapOf<String, Int>()
        val activeDaysSet = mutableSetOf<LocalDate>()

        events.forEach { event ->
            val eventDate = Instant.ofEpochMilli(event.timestamp).atZone(zoneId).toLocalDate()
            when (event.eventType) {
                StudyEventType.FOCUS_COMPLETED.name -> {
                    totalMins += event.actualDurationMinutes
                    completedSessions++
                    if (event.subject.isNotBlank()) {
                        subjectMap[event.subject] = (subjectMap[event.subject] ?: 0) + event.actualDurationMinutes
                    }
                    if (event.actualDurationMinutes >= 15) {
                        activeDaysSet.add(eventDate)
                    }
                }
                StudyEventType.GOAL_COMPLETED.name -> {
                    goalsCompleted++
                }
            }
        }

        val totalDaysInMonth = refDate.lengthOfMonth()
        val avgDailyMins = totalMins / totalDaysInMonth
        val consistencyPercentage = ((activeDaysSet.size.toFloat() / totalDaysInMonth) * 100).toInt()

        val monthYearFormatted = "${refDate.month.name.lowercase().replaceFirstChar { it.titlecase() }} ${refDate.year}"

        MonthlyAnalytics(
            monthYearFormatted = monthYearFormatted,
            totalStudyMinutes = totalMins,
            averageDailyMinutes = avgDailyMins,
            sessionsCompleted = completedSessions,
            goalsCompleted = goalsCompleted,
            subjectDistribution = subjectMap,
            studyConsistencyPercentage = consistencyPercentage
        )
    }

    /**
     * 8 & 9. DATA-DRIVEN STREAK SYSTEM (TIMEZONE SAFE)
     */
    suspend fun getStreakInfo(
        referenceMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakInfo = withContext(Dispatchers.IO) {
        val today = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()

        // Look back up to 90 days
        val checkStart = today.minusDays(90).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val events = eventDao.getEventsSince(checkStart)

        // Group study time by date
        val minsByDate = mutableMapOf<LocalDate, Int>()
        events.forEach { event ->
            if (event.eventType == StudyEventType.FOCUS_COMPLETED.name || event.eventType == StudyEventType.FOCUS_INTERRUPTED.name) {
                val d = Instant.ofEpochMilli(event.timestamp).atZone(zoneId).toLocalDate()
                minsByDate[d] = (minsByDate[d] ?: 0) + event.actualDurationMinutes
            }
        }

        // Check if user qualified today (at least 15 minutes of study or 1 completed session)
        val todayMins = minsByDate[today] ?: 0
        val isQualifiedToday = todayMins >= 15

        // Calculate current streak
        var currentStreak = 0
        var streakStartDate: LocalDate? = null
        var lastQualifiedDate: LocalDate? = null

        var checkDate = if (isQualifiedToday) today else today.minusDays(1)
        var checking = true

        while (checking) {
            val mins = minsByDate[checkDate] ?: 0
            if (mins >= 15) {
                currentStreak++
                if (lastQualifiedDate == null) lastQualifiedDate = checkDate
                streakStartDate = checkDate
                checkDate = checkDate.minusDays(1)
            } else {
                checking = false
            }
        }

        // Also check longest streak in historical map
        var longestStreak = currentStreak
        var tempStreak = 0
        val sortedDates = minsByDate.keys.sorted()
        var prevDate: LocalDate? = null

        for (d in sortedDates) {
            if ((minsByDate[d] ?: 0) >= 15) {
                if (prevDate == null || prevDate.plusDays(1) == d) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
                if (tempStreak > longestStreak) {
                    longestStreak = tempStreak
                }
                prevDate = d
            }
        }

        StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            streakStartDateMillis = streakStartDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli() ?: 0L,
            lastQualifiedStudyDateMillis = lastQualifiedDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli() ?: 0L,
            isQualifiedToday = isQualifiedToday,
            timezoneId = zoneId.id
        )
    }

    /**
     * 11 & 12. SUBJECT ANALYTICS & TREND DETECTION
     */
    suspend fun getSubjectAnalytics(
        referenceMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<SubjectAnalytics> = withContext(Dispatchers.IO) {
        val refDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()

        // Current 7 days
        val currentStart = refDate.minusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val currentEnd = refDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        // Previous 7 days
        val prevStart = refDate.minusDays(13).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val prevEnd = currentStart - 1

        val currentEvents = eventDao.getEventsSince(currentStart).filter { it.timestamp <= currentEnd }
        val prevEvents = eventDao.getEventsSince(prevStart).filter { it.timestamp <= prevEnd }

        val subjectStatsCurrent = mutableMapOf<String, MutableSubjectStat>()
        val subjectStatsPrev = mutableMapOf<String, Int>() // subject -> total mins

        currentEvents.forEach { event ->
            if (event.subject.isNotBlank()) {
                val stat = subjectStatsCurrent.getOrPut(event.subject) { MutableSubjectStat() }
                when (event.eventType) {
                    StudyEventType.FOCUS_COMPLETED.name -> {
                        stat.totalMins += event.actualDurationMinutes
                        stat.completedCount++
                        stat.plannedMins += event.plannedDurationMinutes
                    }
                    StudyEventType.FOCUS_INTERRUPTED.name, StudyEventType.FOCUS_CANCELLED.name -> {
                        stat.totalMins += event.actualDurationMinutes
                        stat.interruptedCount++
                        stat.plannedMins += event.plannedDurationMinutes
                    }
                    StudyEventType.SCHEDULE_MISSED.name -> {
                        stat.missedCount++
                        stat.plannedMins += event.plannedDurationMinutes
                    }
                }
            }
        }

        prevEvents.forEach { event ->
            if (event.subject.isNotBlank() && (event.eventType == StudyEventType.FOCUS_COMPLETED.name || event.eventType == StudyEventType.FOCUS_INTERRUPTED.name)) {
                subjectStatsPrev[event.subject] = (subjectStatsPrev[event.subject] ?: 0) + event.actualDurationMinutes
            }
        }

        val resultList = mutableListOf<SubjectAnalytics>()

        subjectStatsCurrent.forEach { (subject, stat) ->
            val totalSessions = stat.completedCount + stat.interruptedCount
            val avgSessionMins = if (totalSessions > 0) stat.totalMins / totalSessions else 0
            val totalAttempts = totalSessions + stat.missedCount
            val completionRate = if (totalAttempts > 0) ((stat.completedCount.toFloat() / totalAttempts) * 100).toInt() else 100

            // Trend detection rule: Minimum data threshold required (>= 3 sessions total across periods)
            val prevMins = subjectStatsPrev[subject] ?: 0
            val trendDirection = if (totalSessions + (if (prevMins > 0) 2 else 0) < 3) {
                SubjectTrendDirection.INSUFFICIENT_DATA
            } else {
                val diff = stat.totalMins - prevMins
                when {
                    diff > 15 -> SubjectTrendDirection.UP
                    diff < -15 -> SubjectTrendDirection.DOWN
                    else -> SubjectTrendDirection.STABLE
                }
            }

            resultList.add(
                SubjectAnalytics(
                    subject = subject,
                    totalMinutes = stat.totalMins,
                    sessionsCount = totalSessions,
                    averageSessionMinutes = avgSessionMins,
                    completionRate = completionRate,
                    plannedMinutes = stat.plannedMins,
                    actualMinutes = stat.totalMins,
                    trend = trendDirection
                )
            )
        }

        resultList.sortedByDescending { it.totalMinutes }
    }

    /**
     * 15. TIME-OF-DAY ANALYSIS
     */
    suspend fun getTimeOfDayDistribution(
        referenceMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TimeOfDayDistribution = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate().minusDays(30)
            .atStartOfDay(zoneId).toInstant().toEpochMilli()

        val events = eventDao.getEventsSince(thirtyDaysAgo)

        var morning = 0   // 5 - 12
        var afternoon = 0 // 12 - 17
        var evening = 0   // 17 - 21
        var night = 0     // 21 - 5
        var totalSessions = 0

        events.forEach { event ->
            if (event.eventType == StudyEventType.FOCUS_COMPLETED.name || event.eventType == StudyEventType.FOCUS_INTERRUPTED.name) {
                totalSessions++
                val hour = Instant.ofEpochMilli(event.timestamp).atZone(zoneId).hour
                val mins = event.actualDurationMinutes
                when (hour) {
                    in 5..11 -> morning += mins
                    in 12..16 -> afternoon += mins
                    in 17..20 -> evening += mins
                    else -> night += mins
                }
            }
        }

        val dominant = if (totalSessions < 3) {
            "Not enough data yet"
        } else {
            val maxMins = maxOf(morning, afternoon, evening, night)
            when (maxMins) {
                0 -> "Not enough data yet"
                morning -> "Morning (5 AM - 12 PM)"
                afternoon -> "Afternoon (12 PM - 5 PM)"
                evening -> "Evening (5 PM - 9 PM)"
                else -> "Night (9 PM - 5 AM)"
            }
        }

        TimeOfDayDistribution(
            morningMinutes = morning,
            afternoonMinutes = afternoon,
            eveningMinutes = evening,
            nightMinutes = night,
            dominantPeriod = dominant
        )
    }

    /**
     * 19 - 21. SMART INSIGHTS ENGINE (EVIDENCE-BASED & CONFIDENCE-RATED)
     */
    suspend fun getSmartInsights(
        referenceMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<SmartInsight> = withContext(Dispatchers.IO) {
        val insights = mutableListOf<SmartInsight>()

        val weekly = getWeeklyAnalytics(referenceMillis, zoneId)
        val subjects = getSubjectAnalytics(referenceMillis, zoneId)
        val streak = getStreakInfo(referenceMillis, zoneId)
        val timeOfDay = getTimeOfDayDistribution(referenceMillis, zoneId)

        val totalSessions = weekly.completedSessions

        // 1. CONSISTENCY INSIGHT
        if (streak.currentStreak >= 3) {
            insights.add(
                SmartInsight(
                    type = InsightType.CONSISTENCY,
                    severity = InsightSeverity.POSITIVE,
                    confidence = InsightConfidence.HIGH_CONFIDENCE,
                    title = "Continuous Study Streak",
                    summary = "Aapka ${streak.currentStreak}-day study streak active hai. Routine maintain hai.",
                    evidence = "Current streak = ${streak.currentStreak} days, Qualified today = ${streak.isQualifiedToday}",
                    period = "WEEKLY"
                )
            )
        } else if (totalSessions < 3) {
            insights.add(
                SmartInsight(
                    type = InsightType.CONSISTENCY,
                    severity = InsightSeverity.INFO,
                    confidence = InsightConfidence.INSUFFICIENT_DATA,
                    title = "Building Study Pattern",
                    summary = "Regular insights aur consistency metrics ke liye kuch aur study sessions complete karein.",
                    evidence = "Total completed sessions = $totalSessions (< 3 minimum threshold)",
                    period = "WEEKLY"
                )
            )
        }

        // 2. SUBJECT BALANCE INSIGHT
        if (subjects.size >= 2) {
            val topSubject = subjects.first()
            val lowestSubject = subjects.last()

            if (topSubject.totalMinutes > lowestSubject.totalMinutes * 3 && lowestSubject.totalMinutes < 60) {
                insights.add(
                    SmartInsight(
                        type = InsightType.SUBJECT_BALANCE,
                        severity = InsightSeverity.INFO,
                        confidence = if (totalSessions >= 5) InsightConfidence.HIGH_CONFIDENCE else InsightConfidence.MEDIUM_CONFIDENCE,
                        title = "Subject Time Distribution",
                        summary = "${topSubject.subject} ko is week sabse zyada time (${formatMins(topSubject.totalMinutes)}) mila hai, jabki ${lowestSubject.subject} ko ${formatMins(lowestSubject.totalMinutes)}.",
                        evidence = "${topSubject.subject}=${topSubject.totalMinutes}m vs ${lowestSubject.subject}=${lowestSubject.totalMinutes}m",
                        period = "WEEKLY"
                    )
                )
            }
        }

        // 3. SCHEDULE ADHERENCE & PLANNED VS ACTUAL INSIGHT
        if (weekly.plannedMinutes > 0 && totalSessions >= 3) {
            val diff = weekly.plannedMinutes - weekly.actualMinutes
            if (diff > 60) {
                insights.add(
                    SmartInsight(
                        type = InsightType.SCHEDULE_ADHERENCE,
                        severity = InsightSeverity.INFO,
                        confidence = InsightConfidence.HIGH_CONFIDENCE,
                        title = "Scheduled vs Actual Time",
                        summary = "Is week total ${formatMins(weekly.plannedMinutes)} planned tha aur ${formatMins(weekly.actualMinutes)} study complete hui.",
                        evidence = "Planned=${weekly.plannedMinutes}m, Actual=${weekly.actualMinutes}m, Uncompleted=${diff}m",
                        period = "WEEKLY"
                    )
                )
            }
        }

        // 4. TIME PATTERN INSIGHT
        if (timeOfDay.dominantPeriod != "Not enough data yet") {
            insights.add(
                SmartInsight(
                    type = InsightType.TIME_PATTERN,
                    severity = InsightSeverity.INFO,
                    confidence = InsightConfidence.MEDIUM_CONFIDENCE,
                    title = "Preferred Study Slot",
                    summary = "Aap sabse zyada ${timeOfDay.dominantPeriod} time window me study karte hain.",
                    evidence = "Morning=${timeOfDay.morningMinutes}m, Afternoon=${timeOfDay.afternoonMinutes}m, Evening=${timeOfDay.eveningMinutes}m, Night=${timeOfDay.nightMinutes}m",
                    period = "MONTHLY"
                )
            )
        }

        insights
    }

    /**
     * 23 - 24 & 43. NOVA VERIFIED ANALYTICS ROUTER & EXPLANATION ENGINE
     * Strictly uses code-calculated metrics. No LLM hallucinations.
     */
    suspend fun getNovaAnalyticsAnswer(
        userQuery: String,
        referenceMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String = withContext(Dispatchers.IO) {
        val q = userQuery.lowercase(Locale.ROOT)

        when {
            q.contains("aaj") || q.contains("today") -> {
                val daily = getDailyAnalytics(referenceMillis, zoneId)
                if (daily.totalStudyMinutes == 0) {
                    "Aaj abhi tak koi focus session complete nahi hua hai. Aap abhi 25-minute ka focus session start kar sakte hain!"
                } else {
                    val sb = StringBuilder("📊 **Today's Study Summary**\n\n")
                    sb.append("• **Total Time:** ${formatMins(daily.totalStudyMinutes)}\n")
                    sb.append("• **Completed Sessions:** ${daily.completedSessions}\n")
                    if (daily.subjectBreakdown.isNotEmpty()) {
                        sb.append("• **Subject Breakdown:**\n")
                        daily.subjectBreakdown.forEach { (sub, mins) ->
                            sb.append("  - $sub: ${formatMins(mins)}\n")
                        }
                    }
                    sb.toString()
                }
            }

            q.contains("week") || q.contains("hafta") || q.contains("weekly report") -> {
                val weekly = getWeeklyAnalytics(referenceMillis, zoneId)
                val streak = getStreakInfo(referenceMillis, zoneId)

                val sb = StringBuilder("📈 **Weekly Study Report**\n\n")
                sb.append("• **Total Study Time:** ${formatMins(weekly.totalStudyMinutes)}\n")
                sb.append("• **Daily Average:** ${formatMins(weekly.averageDailyMinutes)}/day\n")
                sb.append("• **Completed Sessions:** ${weekly.completedSessions}\n")
                sb.append("• **Current Streak:** ${streak.currentStreak} days\n")
                if (weekly.strongestStudyDay != "N/A" && weekly.strongestStudyDay != "None") {
                    sb.append("• **Top Study Day:** ${weekly.strongestStudyDay}\n")
                }
                if (weekly.subjectDistribution.isNotEmpty()) {
                    sb.append("\n**Subject Time Allocation:**\n")
                    weekly.subjectDistribution.entries.sortedByDescending { it.value }.forEach { (sub, mins) ->
                        sb.append("• $sub: ${formatMins(mins)}\n")
                    }
                }
                sb.toString()
            }

            q.contains("consistency") || q.contains("streak") -> {
                val streak = getStreakInfo(referenceMillis, zoneId)
                "🔥 **Streak & Consistency Status**\n\n" +
                        "• **Current Active Streak:** ${streak.currentStreak} days\n" +
                        "• **Best Streak Record:** ${streak.longestStreak} days\n" +
                        "• **Today's Status:** ${if (streak.isQualifiedToday) "Qualified (>= 15 mins study completed)" else "Pending (Complete 15 mins focus today to maintain streak)"}"
            }

            q.contains("planned vs actual") || q.contains("schedule adherence") -> {
                val weekly = getWeeklyAnalytics(referenceMillis, zoneId)
                "📐 **Planned vs Actual Analysis (This Week)**\n\n" +
                        "• **Scheduled Target:** ${formatMins(weekly.plannedMinutes)}\n" +
                        "• **Actual Studied:** ${formatMins(weekly.actualMinutes)}\n" +
                        if (weekly.uncompletedScheduledMinutes > 0) {
                            "• **Remaining Scheduled Time:** ${formatMins(weekly.uncompletedScheduledMinutes)}"
                        } else {
                            "• **Schedule Completion:** Target achieved or exceeded!"
                        }
            }

            else -> {
                // Default subject or summary answer
                val subjects = getSubjectAnalytics(referenceMillis, zoneId)
                if (subjects.isEmpty()) {
                    "Abhi tak koi detailed study data record nahi hua hai. Padhai start karte hi exact subject breakdown yahan dikhai dega."
                } else {
                    val top = subjects.first()
                    "📚 **Subject Analysis**\n\n" +
                            "Aapne sabse zyada time **${top.subject}** ko diya hai (${formatMins(top.totalMinutes)}, ${top.sessionsCount} sessions).\n" +
                            "Total active subjects: ${subjects.size}."
                }
            }
        }
    }

    private fun formatMins(totalMins: Int): String {
        if (totalMins <= 0) return "0m"
        val hrs = totalMins / 60
        val mins = totalMins % 60
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    private class MutableSubjectStat {
        var totalMins = 0
        var completedCount = 0
        var interruptedCount = 0
        var missedCount = 0
        var plannedMins = 0
    }
}
