package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.StudyMateDatabase
import com.example.data.remote.AuthRepository
import com.example.data.remote.GeminiRepository
import com.example.data.repository.StudyRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyMateApplication : Application(), ImageLoaderFactory {

    lateinit var database: StudyMateDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var geminiRepository: GeminiRepository
        private set

    lateinit var studyRepository: StudyRepository
        private set

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .allowHardware(true)
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    val fallbackOptions = FirebaseOptions.Builder()
                        .setApplicationId(packageName)
                        .setApiKey("AIzaSyDummyKeyForOfflineDevEnvironment000")
                        .setProjectId("studymate-ai-dev")
                        .build()
                    FirebaseApp.initializeApp(this, fallbackOptions)
                }
            }
        } catch (e: Exception) {
            // Handled gracefully in offline or container test runs
        }

        database = StudyMateDatabase.getDatabase(this)
        authRepository = AuthRepository(this, database.userDao())
        geminiRepository = GeminiRepository()
        studyRepository = StudyRepository(database)

        // Initialize Focus Shield & Offline Notification System
        com.example.service.FocusShieldManager.init(this)
        com.example.notification.StudyNotificationManager.initNotificationChannels(this)
        com.example.notification.StudyNotificationManager.scheduleDailyStudyReminder(
            this,
            com.example.notification.StudyNotificationManager.getSettings(this).reminderHour,
            com.example.notification.StudyNotificationManager.getSettings(this).reminderMinute
        )

        CoroutineScope(Dispatchers.IO).launch {
            studyRepository.populateInitialDataIfEmpty()
        }
    }
}
