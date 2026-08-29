package com.example.localization

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.GeminiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class LanguageManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val geminiRepository: GeminiRepository by lazy { GeminiRepository() }

    private val _currentLanguage = MutableStateFlow(loadSavedLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // In-memory translation cache to guarantee zero-lag dynamic content rendering
    private val dynamicTranslationCache = ConcurrentHashMap<String, String>()

    // Memory for persistent translation pairs
    private val persistentTranslationPrefs: SharedPreferences =
        context.getSharedPreferences(TRANSLATION_CACHE_PREFS, Context.MODE_PRIVATE)

    init {
        // Pre-load common dynamic translations from persistent cache into memory
        CoroutineScope(Dispatchers.IO).launch {
            try {
                persistentTranslationPrefs.all.forEach { (k, v) ->
                    if (v is String) {
                        dynamicTranslationCache[k] = v
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun loadSavedLanguage(): AppLanguage {
        val savedCode = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.ENGLISH.code)
        return AppLanguage.fromCode(savedCode)
    }

    fun setLanguage(language: AppLanguage) {
        if (_currentLanguage.value == language) return
        _currentLanguage.value = language
        prefs.edit().putString(KEY_APP_LANGUAGE, language.code).apply()
    }

    fun toggleLanguage() {
        val next = if (_currentLanguage.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        setLanguage(next)
    }

    fun getString(key: String, vararg args: Any): String {
        return AppStrings.get(key, _currentLanguage.value, *args)
    }

    /**
     * Translates single words or small phrases using local dictionary instantly.
     */
    fun translateWord(text: String): String {
        return AppStrings.translateWordOrPhrase(text, _currentLanguage.value)
    }

    /**
     * Translates dynamic content (e.g., job notification summary, topic description)
     * with local caching to avoid unnecessary API requests.
     */
    suspend fun translateDynamic(
        text: String,
        contentId: String? = null,
        targetLanguage: AppLanguage = _currentLanguage.value
    ): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        if (targetLanguage == AppLanguage.ENGLISH) return@withContext text

        val cacheKey = "${targetLanguage.code}_${contentId ?: text.hashCode()}"
        
        // 1. Check in-memory cache
        dynamicTranslationCache[cacheKey]?.let { return@withContext it }

        // 2. Check dictionary heuristic
        val dictWord = AppStrings.translateWordOrPhrase(text, targetLanguage)
        if (dictWord != text) {
            dynamicTranslationCache[cacheKey] = dictWord
            return@withContext dictWord
        }

        // 3. Translate via Gemini with strict safety rules
        try {
            val prompt = """
                Translate the following academic/job notification text from English to natural, accurate Hindi.
                
                Rules:
                - Do NOT translate URLs, website links (http/https), PDF URLs, or email addresses.
                - Do NOT translate unique IDs, post codes, or technical identifiers.
                - Keep numbers, dates (e.g. 25-10-2025), and abbreviations (e.g. UPSC, SSC, RRB, NDA, CDS, IIT) intact.
                - Return ONLY the direct Hindi translation with no extra commentary or quotes.
                
                Text to translate:
                $text
            """.trimIndent()

            val result = geminiRepository.askNovaSimple(prompt)
            if (result.isSuccess) {
                val translated = result.getOrNull()?.trim()
                if (!translated.isNullOrBlank() && !translated.startsWith("Error")) {
                    dynamicTranslationCache[cacheKey] = translated
                    persistentTranslationPrefs.edit().putString(cacheKey, translated).apply()
                    return@withContext translated
                }
            }
        } catch (e: Exception) {
            // Graceful fallback to original text
        }

        return@withContext text
    }

    fun clearCache() {
        dynamicTranslationCache.clear()
        persistentTranslationPrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "studymate_prefs"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val TRANSLATION_CACHE_PREFS = "studymate_translations_cache"

        @Volatile
        private var INSTANCE: LanguageManager? = null

        fun init(context: Context): LanguageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanguageManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getInstance(): LanguageManager {
            return INSTANCE ?: throw IllegalStateException("LanguageManager is not initialized. Call init(context) first.")
        }
    }
}
