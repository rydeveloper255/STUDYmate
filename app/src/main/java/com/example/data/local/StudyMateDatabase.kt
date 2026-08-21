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
        com.example.data.persistence.PendingSyncEntity::class
    ],
    version = 11,
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
    abstract fun novaMemoryDao(): NovaMemoryDao
    abstract fun novaReminderDao(): NovaReminderDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun smartNoteDao(): SmartNoteDao
    abstract fun currentAffairsDao(): CurrentAffairsDao
    abstract fun examUpdateDao(): ExamUpdateDao
    abstract fun examObjectiveDao(): ExamObjectiveDao
    abstract fun topicMasteryDao(): TopicMasteryDao
    abstract fun studentSessionHistoryDao(): StudentSessionHistoryDao
    abstract fun intelligenceSnapshotDao(): IntelligenceSnapshotDao
    abstract fun examCatalogDao(): ExamCatalogDao
    abstract fun learningTopicContentDao(): LearningTopicContentDao
    abstract fun userLearningBookmarkDao(): UserLearningBookmarkDao
    abstract fun questionHistoryDao(): QuestionHistoryDao
    abstract fun questionQualityReportDao(): QuestionQualityReportDao
    abstract fun pendingSyncDao(): com.example.data.persistence.PendingSyncDao

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
