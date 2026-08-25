package com.example.service.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

/**
 * Production ElevenLabs Neural Text-to-Speech Engine for NOVA.
 *
 * Implements conversational multi-language TTS (Hindi, Hinglish, English) with deterministic
 * caching, low-latency audio delivery, visual waveform RMS amplitude metering,
 * and robust fallback to AndroidAcousticTtsProvider when offline or unconfigured.
 */
class ElevenLabsTtsProvider(
    private val context: Context,
    private val acousticFallback: AndroidAcousticTtsProvider = AndroidAcousticTtsProvider(context),
    private val voiceCache: NovaVoiceCache = NovaVoiceCache(context)
) : NovaVoiceService, com.example.nova.NovaVoiceService {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var mediaPlayer: MediaPlayer? = null

    private val _sessionState = MutableStateFlow(NovaVoiceSessionState.IDLE)
    val sessionState: StateFlow<NovaVoiceSessionState> = _sessionState.asStateFlow()

    private val _isSpeakingFlow = MutableStateFlow(false)
    override val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    private val _isReadyFlow = MutableStateFlow(true)
    override val isReadyFlow: StateFlow<Boolean> = _isReadyFlow.asStateFlow()

    private val _audioLevelRms = MutableStateFlow(0f)
    val audioLevelRms: StateFlow<Float> = _audioLevelRms.asStateFlow()

    private var currentSpeed: Float = 1.0f
    private var currentVolume: Float = 1.0f
    private var lastSpokenText: String = ""
    private var lastEmotion: NovaVoiceEmotion = NovaVoiceEmotion.CALM

    private var onStartCallback: (() -> Unit)? = null
    private var onDoneCallback: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activePlaybackJob: Job? = null
    private var rmsSimulationJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Configurable voice and model parameters
    var customVoiceId: String? = null
    var customModelId: String? = null

    companion object {
        private const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM" // Rachel (Warm, Natural, Young Adult)
        private const val DEFAULT_MODEL_ID = "eleven_multilingual_v2"
        private const val API_BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun getBuildConfigField(fieldName: String): String {
        return try {
            val field = BuildConfig::class.java.getField(fieldName)
            (field.get(null) as? String)?.trim() ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    private fun getResolvedApiKey(): String {
        return try {
            val key = getBuildConfigField("ELEVENLABS_API_KEY")
            if (key.isBlank() || key.contains("dummy_elevenlabs_key", ignoreCase = true)) {
                ""
            } else {
                key.trim()
            }
        } catch (e: Throwable) {
            ""
        }
    }

    private fun getResolvedVoiceId(): String {
        customVoiceId?.takeIf { it.isNotBlank() }?.let { return it }
        return try {
            val voiceId = getBuildConfigField("ELEVENLABS_VOICE_ID")
            if (voiceId.isNotBlank() && !voiceId.contains("dummy", ignoreCase = true)) {
                voiceId.trim()
            } else {
                DEFAULT_VOICE_ID
            }
        } catch (e: Throwable) {
            DEFAULT_VOICE_ID
        }
    }

    private fun getResolvedModelId(): String {
        customModelId?.takeIf { it.isNotBlank() }?.let { return it }
        return try {
            val modelId = getBuildConfigField("ELEVENLABS_MODEL_ID")
            if (modelId.isNotBlank() && !modelId.contains("dummy", ignoreCase = true)) {
                modelId.trim()
            } else {
                DEFAULT_MODEL_ID
            }
        } catch (e: Throwable) {
            DEFAULT_MODEL_ID
        }
    }

    override fun speak(text: String) {
        speak(text, NovaVoiceEmotion.CALM, null, null)
    }

    override fun speak(
        text: String,
        emotion: NovaVoiceEmotion,
        onStart: (() -> Unit)?,
        onDone: (() -> Unit)?
    ) {
        // Stop any previous playback immediately
        stopSpeaking()

        if (isMutedOrDnd()) {
            onDone?.invoke()
            return
        }

        val normalized = NovaVoiceTextNormalizer.normalizeForSpeech(text)
        if (normalized.isBlank()) {
            onDone?.invoke()
            return
        }

        this.lastSpokenText = text
        this.lastEmotion = emotion
        this.onStartCallback = onStart
        this.onDoneCallback = onDone

        val apiKey = getResolvedApiKey()
        val voiceId = getResolvedVoiceId()
        val modelId = getResolvedModelId()
        val language = NovaVoiceTextNormalizer.detectLanguage(normalized)

        // If no ElevenLabs API key is configured or available, seamlessly route to acoustic fallback
        if (apiKey.isBlank()) {
            _sessionState.value = NovaVoiceSessionState.PLAYING
            _isSpeakingFlow.value = true
            acousticFallback.setSpeechSpeed(currentSpeed)
            acousticFallback.setVolume(currentVolume)
            acousticFallback.speak(
                text = normalized,
                emotion = emotion,
                onStart = {
                    _sessionState.value = NovaVoiceSessionState.PLAYING
                    _isSpeakingFlow.value = true
                    onStart?.invoke()
                    startRmsMetering()
                },
                onDone = {
                    stopRmsMetering()
                    _sessionState.value = NovaVoiceSessionState.IDLE
                    _isSpeakingFlow.value = false
                    onDone?.invoke()
                }
            )
            return
        }

        // Generate or retrieve audio from ElevenLabs with caching
        activePlaybackJob = scope.launch {
            _sessionState.value = NovaVoiceSessionState.GENERATING
            val cacheKey = voiceCache.createCacheKey(normalized, voiceId, modelId, currentSpeed, language)
            var audioFile = voiceCache.getAudioFile(cacheKey)

            if (audioFile == null) {
                // Fetch from ElevenLabs API with safe exponential retry
                val fetchedBytes = fetchElevenLabsAudioWithRetry(
                    apiKey = apiKey,
                    voiceId = voiceId,
                    modelId = modelId,
                    text = normalized,
                    emotion = emotion
                )

                if (fetchedBytes != null && fetchedBytes.isNotEmpty()) {
                    audioFile = voiceCache.saveAudio(cacheKey, fetchedBytes)
                }
            }

            if (audioFile != null && audioFile.exists()) {
                withContext(Dispatchers.Main) {
                    playAudioFile(audioFile)
                }
            } else {
                // ElevenLabs failed -> fallback to Android acoustic engine without crashing
                withContext(Dispatchers.Main) {
                    _sessionState.value = NovaVoiceSessionState.PLAYING
                    _isSpeakingFlow.value = true
                    acousticFallback.setSpeechSpeed(currentSpeed)
                    acousticFallback.setVolume(currentVolume)
                    acousticFallback.speak(
                        text = normalized,
                        emotion = emotion,
                        onStart = {
                            _sessionState.value = NovaVoiceSessionState.PLAYING
                            _isSpeakingFlow.value = true
                            onStart?.invoke()
                            startRmsMetering()
                        },
                        onDone = {
                            stopRmsMetering()
                            _sessionState.value = NovaVoiceSessionState.IDLE
                            _isSpeakingFlow.value = false
                            onDone?.invoke()
                        }
                    )
                }
            }
        }
    }

    /**
     * Executes the ElevenLabs TTS REST API request with up to 2 retry attempts for transient errors.
     */
    private suspend fun fetchElevenLabsAudioWithRetry(
        apiKey: String,
        voiceId: String,
        modelId: String,
        text: String,
        emotion: NovaVoiceEmotion
    ): ByteArray? {
        var attempts = 0
        var backoffMs = 500L

        while (attempts < 2) {
            attempts++
            try {
                val jsonBody = JSONObject().apply {
                    put("text", text)
                    put("model_id", modelId)
                    put("voice_settings", JSONObject().apply {
                        put("stability", 0.50f)
                        put("similarity_boost", 0.75f)
                        put("style", when (emotion) {
                            NovaVoiceEmotion.HAPPY_ACHIEVEMENT -> 0.15f
                            NovaVoiceEmotion.GENTLE_MOTIVATION -> 0.10f
                            NovaVoiceEmotion.WARNING -> 0.20f
                            else -> 0.0f
                        })
                        put("use_speaker_boost", true)
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$API_BASE_URL/$voiceId")
                    .addHeader("xi-api-key", apiKey)
                    .addHeader("Accept", "audio/mpeg")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    response.close()
                    if (bytes != null && bytes.isNotEmpty()) {
                        return bytes
                    }
                } else {
                    // Check for rate limit 429
                    if (response.code == 429) {
                        response.close()
                        delay(1200)
                    } else {
                        response.close()
                    }
                }
            } catch (e: Exception) {
                // Transient network exception
                delay(backoffMs)
                backoffMs *= 2
            }
        }
        return null
    }

    /**
     * Plays a cached MP3 audio file via Android MediaPlayer.
     */
    private fun playAudioFile(file: File) {
        try {
            requestAudioFocus()
            releaseMediaPlayer()

            val mp = MediaPlayer()
            mediaPlayer = mp

            val fis = FileInputStream(file)
            mp.setDataSource(fis.fd)
            fis.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                mp.setAudioAttributes(audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }

            mp.setVolume(currentVolume, currentVolume)

            mp.setOnPreparedListener { player ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val params = PlaybackParams()
                        params.speed = currentSpeed.coerceIn(0.7f, 1.6f)
                        player.playbackParams = params
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                player.start()
                _sessionState.value = NovaVoiceSessionState.PLAYING
                _isSpeakingFlow.value = true
                onStartCallback?.invoke()
                startRmsMetering()
            }

            mp.setOnCompletionListener {
                _sessionState.value = NovaVoiceSessionState.IDLE
                _isSpeakingFlow.value = false
                stopRmsMetering()
                abandonAudioFocus()
                onDoneCallback?.invoke()
            }

            mp.setOnErrorListener { _, _, _ ->
                _sessionState.value = NovaVoiceSessionState.ERROR
                _isSpeakingFlow.value = false
                stopRmsMetering()
                abandonAudioFocus()
                true
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            _sessionState.value = NovaVoiceSessionState.ERROR
            _isSpeakingFlow.value = false
            abandonAudioFocus()
            onDoneCallback?.invoke()
        }
    }

    /**
     * Simulates dynamic RMS amplitude levels while audio is playing to drive waveform UI.
     */
    private fun startRmsMetering() {
        rmsSimulationJob?.cancel()
        rmsSimulationJob = scope.launch {
            var phase = 0f
            while (isActive && _isSpeakingFlow.value) {
                phase += 0.25f
                val wave = (kotlin.math.sin(phase) + 1f) / 2f
                val noise = (Math.random().toFloat() * 0.3f)
                _audioLevelRms.value = ((wave * 0.7f + noise) * currentVolume).coerceIn(0.1f, 1.0f)
                delay(80)
            }
            _audioLevelRms.value = 0f
        }
    }

    private fun stopRmsMetering() {
        rmsSimulationJob?.cancel()
        rmsSimulationJob = null
        _audioLevelRms.value = 0f
    }

    override fun stopSpeaking() {
        activePlaybackJob?.cancel()
        activePlaybackJob = null
        stopRmsMetering()

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            releaseMediaPlayer()
        } catch (e: Exception) {
            // Ignore
        }

        acousticFallback.stopSpeaking()
        abandonAudioFocus()
        _isSpeakingFlow.value = false
        _sessionState.value = NovaVoiceSessionState.STOPPED
    }

    override fun stop() {
        stopSpeaking()
    }

    override fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _sessionState.value = NovaVoiceSessionState.PAUSED
                _isSpeakingFlow.value = false
                stopRmsMetering()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun resume() {
        try {
            if (_sessionState.value == NovaVoiceSessionState.PAUSED && mediaPlayer != null) {
                mediaPlayer?.start()
                _sessionState.value = NovaVoiceSessionState.PLAYING
                _isSpeakingFlow.value = true
                startRmsMetering()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun replay() {
        if (lastSpokenText.isNotBlank()) {
            speak(lastSpokenText, lastEmotion, onStartCallback, onDoneCallback)
        }
    }

    override fun isSpeaking(): Boolean {
        return _isSpeakingFlow.value || mediaPlayer?.isPlaying == true || acousticFallback.isSpeaking()
    }

    override fun setSpeechSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
        acousticFallback.setSpeechSpeed(currentSpeed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer?.isPlaying == true) {
            try {
                val params = PlaybackParams()
                params.speed = currentSpeed
                mediaPlayer?.playbackParams = params
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun setSpeed(speed: Float) {
        setSpeechSpeed(speed)
    }

    override fun setPitch(pitch: Float) {
        acousticFallback.setPitch(pitch)
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1.0f)
        acousticFallback.setVolume(currentVolume)
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun release() {
        stopSpeaking()
        releaseMediaPlayer()
        acousticFallback.release()
        _isReadyFlow.value = false
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun isMutedOrDnd(): Boolean {
        val am = audioManager ?: return false
        val ringerMode = am.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) return true
        val mediaVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return mediaVol == 0
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }
}
