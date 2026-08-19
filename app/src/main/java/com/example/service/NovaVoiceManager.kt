package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.model.NovaVoiceState
import com.example.service.voice.AndroidAcousticTtsProvider
import com.example.service.voice.NovaVoiceEmotion
import com.example.service.voice.NovaVoiceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * High-level Coordinator managing Speech-to-Text, Voice synthesis via modular NovaVoiceService,
 * Audio Level (RMS) metering for visual animations, and playback controls.
 */
class NovaVoiceManager(
    private val context: Context,
    private var voiceService: NovaVoiceService = AndroidAcousticTtsProvider(context)
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow(NovaVoiceState.IDLE)
    val voiceState: StateFlow<NovaVoiceState> = _voiceState.asStateFlow()

    private val _audioLevelRms = MutableStateFlow(0f)
    val audioLevelRms: StateFlow<Float> = _audioLevelRms.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var voiceStateJob: kotlinx.coroutines.Job? = null

    init {
        observeVoiceState()
    }

    private fun observeVoiceState() {
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
                onDone?.invoke()
            }
        )
    }

    fun stopSpeaking() {
        voiceService.stopSpeaking()
        if (_voiceState.value == NovaVoiceState.SPEAKING) {
            _voiceState.value = NovaVoiceState.IDLE
        }
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

    fun switchProvider(providerType: com.example.service.voice.NovaVoiceProviderType) {
        voiceService.release()
        voiceService = com.example.service.voice.NovaVoiceServiceFactory.create(context, providerType)
        observeVoiceState()
    }

    fun previewVoice(
        text: String = "Boss, main tumhari AI assistant NOVA hoon. Aaj ka study session start karein?",
        emotion: NovaVoiceEmotion = NovaVoiceEmotion.CALM
    ) {
        speak(text, emotion)
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
        _audioLevelRms.value = (rmsdB.coerceIn(0f, 10f) / 10f)
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
