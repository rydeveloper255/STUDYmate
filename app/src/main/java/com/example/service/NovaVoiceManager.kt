package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.NovaVoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class NovaVoiceManager(private val context: Context) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow(NovaVoiceState.IDLE)
    val voiceState: StateFlow<NovaVoiceState> = _voiceState.asStateFlow()

    private val _audioLevelRms = MutableStateFlow(0f)
    val audioLevelRms: StateFlow<Float> = _audioLevelRms.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val result = textToSpeech?.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.language = Locale.getDefault()
                }
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setPitch(1.05f)

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = NovaVoiceState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _voiceState.value = NovaVoiceState.IDLE
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _voiceState.value = NovaVoiceState.IDLE
                    }
                })
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

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isTtsReady || textToSpeech == null) return

        val cleanSpokenText = text
            .replace(Regex("""[*#_`~>\[\]\(\)]"""), "")
            .replace(Regex("""\[ACTION:.*?\]"""), "")
            .replace(Regex("""\[MEMORY:.*?\]"""), "")
            .trim()

        if (cleanSpokenText.isBlank()) return

        stopListening()
        _voiceState.value = NovaVoiceState.SPEAKING
        textToSpeech?.speak(cleanSpokenText, TextToSpeech.QUEUE_FLUSH, null, "NOVA_UTTERANCE_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        if (_voiceState.value == NovaVoiceState.SPEAKING) {
            _voiceState.value = NovaVoiceState.IDLE
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
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
