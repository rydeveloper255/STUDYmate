package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.model.NovaVoiceState
import com.example.service.voice.ElevenLabsTtsProvider
import com.example.service.voice.NovaVoiceEmotion
import com.example.service.voice.NovaVoiceProviderType
import com.example.service.voice.NovaVoiceService
import com.example.service.voice.NovaVoiceServiceFactory
import com.example.service.voice.NovaVoiceSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Unified Voice Coordinator managing Speech-to-Text, ElevenLabs Primary TTS Engine,
 * On-Device Acoustic fallback, audio RMS metering, and playback controls.
 */
class NovaVoiceManager(
    private val context: Context,
    private var voiceService: NovaVoiceService = ElevenLabsTtsProvider(context)
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow(NovaVoiceState.IDLE)
    val voiceState: StateFlow<NovaVoiceState> = _voiceState.asStateFlow()

    private val _sessionState = MutableStateFlow(NovaVoiceSessionState.IDLE)
    val sessionState: StateFlow<NovaVoiceSessionState> = _sessionState.asStateFlow()

    private val _audioLevelRms = MutableStateFlow(0f)
    val audioLevelRms: StateFlow<Float> = _audioLevelRms.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var voiceStateJob: Job? = null
    private var audioRmsJob: Job? = null
    private var sessionStateJob: Job? = null

    init {
        observeVoiceService()
    }

    private fun observeVoiceService() {
        voiceStateJob?.cancel()
        voiceStateJob = scope.launch {
            voiceService.isSpeakingFlow.collectLatest { isSpeaking ->
                if (isSpeaking) {
                    _voiceState.value = NovaVoiceState.SPEAKING
                } else if (_voiceState.value == NovaVoiceState.SPEAKING) {
                    _voiceState.value = NovaVoiceState.IDLE
                }
            }
        }

        sessionStateJob?.cancel()
        sessionStateJob = scope.launch {
            voiceService.sessionStateFlow.collectLatest { state ->
                _sessionState.value = state
            }
        }

        audioRmsJob?.cancel()
        audioRmsJob = scope.launch {
            voiceService.audioLevelRmsFlow.collectLatest { rms ->
                if (_voiceState.value == NovaVoiceState.SPEAKING) {
                    _audioLevelRms.value = rms
                }
            }
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult("Voice recognition is not available on this device.")
            return
        }

        stopSpeaking()
        onSpeechResultCallback = onResult

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@NovaVoiceManager)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            _recognizedText.value = ""
            _voiceState.value = NovaVoiceState.LISTENING
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = NovaVoiceState.IDLE
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
        _voiceState.value = NovaVoiceState.PROCESSING
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
        _voiceState.value = NovaVoiceState.IDLE
    }

    fun speak(
        text: String,
        emotion: NovaVoiceEmotion = NovaVoiceEmotion.CALM,
        onDone: (() -> Unit)? = null
    ) {
        stopListening()
        _voiceState.value = NovaVoiceState.SPEAKING
        voiceService.speak(
            text = text,
            emotion = emotion,
            onStart = { _voiceState.value = NovaVoiceState.SPEAKING },
            onDone = {
                _voiceState.value = NovaVoiceState.IDLE
                _audioLevelRms.value = 0f
                onDone?.invoke()
            }
        )
    }

    fun stopSpeaking() {
        voiceService.stopSpeaking()
        if (_voiceState.value == NovaVoiceState.SPEAKING) {
            _voiceState.value = NovaVoiceState.IDLE
        }
        _audioLevelRms.value = 0f
    }

    fun pause() {
        voiceService.pause()
    }

    fun resume() {
        voiceService.resume()
    }

    fun replay() {
        voiceService.replay()
    }

    fun isSpeaking(): Boolean {
        return voiceService.isSpeaking()
    }

    fun setSpeechSpeed(speed: Float) {
        voiceService.setSpeechSpeed(speed)
    }

    fun setPitch(pitch: Float) {
        voiceService.setPitch(pitch)
    }

    fun setVolume(volume: Float) {
        voiceService.setVolume(volume)
    }

    fun switchProvider(providerType: NovaVoiceProviderType) {
        voiceService.release()
        voiceService = NovaVoiceServiceFactory.create(context, providerType)
        observeVoiceService()
    }

    fun previewVoice(
        language: String = "HINGLISH",
        emotion: NovaVoiceEmotion = NovaVoiceEmotion.CALM
    ) {
        val sampleText = when (language.uppercase()) {
            "HI", "HINDI" -> "नमस्ते! मैं नोवा हूँ, आपकी स्मार्ट स्टडी और रिक्रूटमेंट साथी। आज क्या पढ़ना चाहते हैं?"
            "EN", "ENGLISH" -> "Hello! I'm Nova, your AI study mentor. Let's make today's preparation super productive."
            else -> "Boss, main tumhari AI assistant NOVA hoon. Aaj ka study session start karein?"
        }
        speak(sampleText, emotion)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            voiceService.release()
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- RecognitionListener Callbacks ---
    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = NovaVoiceState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = NovaVoiceState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (_voiceState.value == NovaVoiceState.LISTENING) {
            _audioLevelRms.value = (rmsdB.coerceIn(0f, 10f) / 10f)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _voiceState.value = NovaVoiceState.PROCESSING
    }

    override fun onError(error: Int) {
        _voiceState.value = NovaVoiceState.IDLE
        _audioLevelRms.value = 0f
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        _recognizedText.value = text
        _voiceState.value = NovaVoiceState.IDLE
        _audioLevelRms.value = 0f
        if (text.isNotBlank()) {
            onSpeechResultCallback?.invoke(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _recognizedText.value = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
