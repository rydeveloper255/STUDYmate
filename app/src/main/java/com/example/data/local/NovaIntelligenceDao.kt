package com.example.data.local

import androidx.room.*
import com.example.data.model.DailyMissionTask
import com.example.data.model.UserWeeklyGoalEntity
import com.example.data.model.MotivationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovaIntelligenceDao {

    // Daily Mission Tasks
    @Query("SELECT * FROM daily_mission_tasks WHERE dateFormatted = :dateFormatted AND isDismissed = 0 ORDER BY isFromSchedule DESC, isCompleted ASC")
    fun getDailyMissionsForDate(dateFormatted: String): Flow<List<DailyMissionTask>>

    @Query("SELECT * FROM daily_mission_tasks WHERE dateFormatted = :dateFormatted AND isDismissed = 0")
    suspend fun getDailyMissionsForDateOnce(dateFormatted: String): List<DailyMissionTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyMissionTasks(tasks: List<DailyMissionTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyMissionTask(task: DailyMissionTask): Long

    @Query("UPDATE daily_mission_tasks SET isCompleted = :isCompleted, completedTimestamp = :timestamp WHERE id = :taskId")
    suspend fun updateMissionCompletion(taskId: String, isCompleted: Boolean, timestamp: Long)

    @Query("UPDATE daily_mission_tasks SET isDismissed = 1 WHERE id = :taskId")
    suspend fun dismissMissionTask(taskId: String)

    @Query("DELETE FROM daily_mission_tasks WHERE dateFormatted < :cutoffDate")
    suspend fun cleanupOldMissions(cutoffDate: String)

    // User Weekly Goals
    @Query("SELECT * FROM user_weekly_goals WHERE userId = :userId LIMIT 1")
    fun getWeeklyGoal(userId: String = "current_user"): Flow<UserWeeklyGoalEntity?>

    @Query("SELECT * FROM user_weekly_goals WHERE userId = :userId LIMIT 1")
    suspend fun getWeeklyGoalOnce(userId: String = "current_user"): UserWeeklyGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setWeeklyGoal(goal: UserWeeklyGoalEntity)

    // Motivation History
    @Query("SELECT * FROM motivation_history WHERE userId = :userId ORDER BY sentTimestamp DESC LIMIT 20")
    suspend fun getRecentMotivationHistory(userId: String = "current_user"): List<MotivationHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordMotivationSent(history: MotivationHistoryEntity)
}
