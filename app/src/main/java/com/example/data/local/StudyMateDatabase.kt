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
        UserQuestionMaterial::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyMateDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun focusDao(): FocusDao
    abstract fun mockTestDao(): MockTestDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun userQuestionMaterialDao(): UserQuestionMaterialDao

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
