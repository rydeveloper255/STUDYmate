package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        UserProfile::class,
        StudyPlanItem::class,
        FocusSession::class,
        MockTestAttempt::class,
        MistakeItem::class,
        FlashcardItem::class,
        UserQuestionMaterial::class,
        NovaConversationEntity::class,
        NovaMemoryItem::class,
        NovaReminderItem::class,
        VoiceNoteItem::class,
        SmartNoteItem::class,
        CurrentAffairsItem::class,
        ExamUpdateItem::class,
        ExamObjective::class,
        TopicMastery::class,
        StudentSessionHistory::class,
        IntelligenceSnapshot::class,
        ExamEntity::class,
        ExamSubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        UserStudyPreferences::class,
        LearningTopicContent::class,
        UserLearningBookmark::class,
        QuestionHistoryEntity::class,
        QuestionQualityReportEntity::class,
        LiveExamUpdateEntity::class,
        TrendingExamTopicEntity::class,
        com.example.data.model.RecruitmentEntity::class,
        com.example.data.persistence.PendingSyncEntity::class,
        com.example.data.model.StudyScheduleItem::class,
        com.example.data.model.StudyScheduleLog::class,
        com.example.data.model.DailyMissionTask::class,
        com.example.data.model.UserWeeklyGoalEntity::class,
        com.example.data.model.MotivationHistoryEntity::class,
        com.example.data.model.StudyEventEntity::class,
        com.example.data.model.ExamGoalEntity::class,
        com.example.data.model.SyllabusTopicEntity::class,
        com.example.data.model.StudyResourceEntity::class,
        com.example.data.model.ResourceBookmarkEntity::class,
        PracticeSessionEntity::class,
        QuestionAttemptEntity::class,
        SavedQuestionEntity::class,
        RevisionItemEntity::class,
        RevisionSessionEntity::class,
        com.example.data.model.content.RawSourceRecordEntity::class,
        com.example.data.model.content.ContentVersionEntity::class,
        com.example.data.model.content.ReviewQueueItemEntity::class,
        com.example.data.model.content.ContentCollectionJobLogEntity::class,
        com.example.data.model.content.AiProcessingLogEntity::class,
        com.example.data.model.content.TelegramPublicationEntity::class,
        com.example.data.model.content.ContentSourceConfig::class,
        com.example.data.model.content.ContentSourceReferenceEntity::class
    ],
    version = 24,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyMateDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun userStudyPreferencesDao(): UserStudyPreferencesDao
    abstract fun focusDao(): FocusDao
    abstract fun mockTestDao(): MockTestDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun userQuestionMaterialDao(): UserQuestionMaterialDao
    abstract fun novaConversationDao(): NovaConversationDao
    abstract fun novaMemoryDao(): NovaMemoryDao
    abstract fun novaReminderDao(): NovaReminderDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun smartNoteDao(): SmartNoteDao
    abstract fun currentAffairsDao(): CurrentAffairsDao
    abstract fun examUpdateDao(): ExamUpdateDao
    abstract fun liveExamUpdateDao(): LiveExamUpdateDao
    abstract fun trendingExamTopicDao(): TrendingExamTopicDao
    abstract fun examObjectiveDao(): ExamObjectiveDao
    abstract fun topicMasteryDao(): TopicMasteryDao
    abstract fun studentSessionHistoryDao(): StudentSessionHistoryDao
    abstract fun intelligenceSnapshotDao(): IntelligenceSnapshotDao
    abstract fun examCatalogDao(): ExamCatalogDao
    abstract fun learningTopicContentDao(): LearningTopicContentDao
    abstract fun userLearningBookmarkDao(): UserLearningBookmarkDao
    abstract fun questionHistoryDao(): QuestionHistoryDao
    abstract fun questionQualityReportDao(): QuestionQualityReportDao
    abstract fun recruitmentDao(): RecruitmentDao
    abstract fun pendingSyncDao(): com.example.data.persistence.PendingSyncDao
    abstract fun studyScheduleDao(): StudyScheduleDao
    abstract fun novaIntelligenceDao(): NovaIntelligenceDao
    abstract fun studyEventDao(): StudyEventDao
    abstract fun examPrepDao(): ExamPrepDao
    abstract fun resourceDao(): ResourceDao
    abstract fun practiceDao(): PracticeDao
    abstract fun revisionDao(): RevisionDao
    abstract fun contentIntelligenceDao(): ContentIntelligenceDao

    companion object {
        @Volatile
        private var INSTANCE: StudyMateDatabase? = null

        fun getDatabase(context: Context): StudyMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyMateDatabase::class.java,
                    "studymate_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
