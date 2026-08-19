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

    private var imageLoaderInstance: ImageLoader? = null

    override fun newImageLoader(): ImageLoader {
        val loader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
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
        imageLoaderInstance = loader
        return loader
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            if (level >= TRIM_MEMORY_BACKGROUND) {
                imageLoaderInstance?.memoryCache?.clear()
            }
        } catch (e: Exception) {
            // Handled safely
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            imageLoaderInstance?.memoryCache?.clear()
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            // Handled gracefully in offline or container test runs if google-services.json is absent
        }

        database = StudyMateDatabase.getDatabase(this)
        authRepository = AuthRepository(this, database.userDao())
        geminiRepository = GeminiRepository()
        studyRepository = StudyRepository(database)

        // Initialize Focus Shield & Offline Notification System
        com.example.service.FocusShieldManager.init(this)
        com.example.notification.StudyNotificationManager.initNotificationChannels(this)
        com.example.notification.StudyNotificationManager.scheduleAllReminders(this)

        CoroutineScope(Dispatchers.IO).launch {
            studyRepository.populateInitialDataIfEmpty()
        }
    }
}
