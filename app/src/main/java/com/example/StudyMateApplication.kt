package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.StudyMateDatabase
import com.example.data.remote.AuthRepository
import com.example.data.remote.GeminiRepository
import com.example.data.remote.supabase.SupabaseAuthManager
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseSyncService
import com.example.data.repository.StudyRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyMateApplication : Application(), ImageLoaderFactory {

    lateinit var database: StudyMateDatabase
        private set

    lateinit var supabaseClient: SupabaseClient
        private set

    lateinit var supabaseAuthManager: SupabaseAuthManager
        private set

    lateinit var supabaseSyncService: SupabaseSyncService
        private set

    lateinit var supabaseContentHubService: com.example.data.remote.supabase.SupabaseContentHubService
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var geminiRepository: GeminiRepository
        private set

    lateinit var studyRepository: StudyRepository
        private set

    lateinit var examCatalogRepository: com.example.data.repository.ExamCatalogRepository
        private set

    lateinit var liveExamIntelligenceEngine: com.example.service.intelligence.LiveExamIntelligenceEngine
        private set

    lateinit var recruitmentIntelligenceEngine: com.example.service.intelligence.RecruitmentIntelligenceEngine
        private set

    lateinit var telegramBotService: com.example.data.remote.telegram.TelegramBotService
        private set

    lateinit var sourceManager: com.example.service.collector.SourceManager
        private set

    lateinit var automatedContentCollectorEngine: com.example.service.collector.AutomatedContentCollectorEngine
        private set

    lateinit var automatedContentScheduler: com.example.service.collector.AutomatedContentScheduler
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
        supabaseClient = SupabaseClient()
        supabaseAuthManager = SupabaseAuthManager(this, supabaseClient)
        supabaseSyncService = SupabaseSyncService(supabaseClient, supabaseAuthManager, database)
        supabaseContentHubService = com.example.data.remote.supabase.SupabaseContentHubService(
            supabaseClient = supabaseClient,
            database = database
        )

        authRepository = AuthRepository(this, database.userDao(), supabaseAuthManager, supabaseClient, supabaseSyncService)
        geminiRepository = GeminiRepository()
        studyRepository = StudyRepository(database, supabaseSyncService)
        examCatalogRepository = com.example.data.repository.ExamCatalogRepository(database.examCatalogDao())
        liveExamIntelligenceEngine = com.example.service.intelligence.LiveExamIntelligenceEngine(
            liveExamUpdateDao = database.liveExamUpdateDao(),
            trendingExamTopicDao = database.trendingExamTopicDao(),
            geminiRepository = geminiRepository,
            supabaseClient = supabaseClient
        )
        recruitmentIntelligenceEngine = com.example.service.intelligence.RecruitmentIntelligenceEngine(
            recruitmentDao = database.recruitmentDao(),
            geminiRepository = geminiRepository,
            supabaseClient = supabaseClient
        )
        telegramBotService = com.example.data.remote.telegram.TelegramBotService()
        sourceManager = com.example.service.collector.SourceManager()
        automatedContentCollectorEngine = com.example.service.collector.AutomatedContentCollectorEngine(
            context = this,
            database = database,
            telegramBotService = telegramBotService,
            sourceManager = sourceManager,
            geminiRepository = geminiRepository,
            supabaseContentHub = supabaseContentHubService
        )
        automatedContentScheduler = com.example.service.collector.AutomatedContentScheduler(
            context = this,
            collectorEngine = automatedContentCollectorEngine
        )
        automatedContentScheduler.startScheduler()

        // Initialize Focus Shield & Offline Notification System
        com.example.service.FocusShieldManager.init(this)
        com.example.notification.StudyNotificationManager.initNotificationChannels(this)
        com.example.notification.StudyNotificationManager.scheduleAllReminders(this)

        CoroutineScope(Dispatchers.IO).launch {
            examCatalogRepository.seedDefaultCatalogIfEmpty()
            recruitmentIntelligenceEngine.seedInitialCatalogIfEmpty()
            studyRepository.populateInitialDataIfEmpty()
        }
    }
}
