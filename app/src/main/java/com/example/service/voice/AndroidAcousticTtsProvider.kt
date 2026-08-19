package com.example.service.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * High-fidelity Android Acoustic TTS Provider with customized original young-adult female AI persona.
 * Tunes pitch, speed, cadence, and voice profile to achieve consistent pronunciation for English, Hindi, and Hinglish.
 */
class AndroidAcousticTtsProvider(
    private val context: Context
) : NovaVoiceService, com.example.nova.NovaVoiceService {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var textToSpeech: TextToSpeech? = null

    private val _isSpeakingFlow = MutableStateFlow(false)
    override val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    private val _isReadyFlow = MutableStateFlow(false)
    override val isReadyFlow: StateFlow<Boolean> = _isReadyFlow.asStateFlow()

    private var currentSpeed: Float = 1.0f
    private var basePitch: Float = 1.12f // Young-adult warm female register
    private var currentVolume: Float = 1.0f

    private var onDoneCallback: (() -> Unit)? = null
    private var onStartCallback: (() -> Unit)? = null

    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureFemaleVoiceProfile()
                _isReadyFlow.value = true
            }
        }
    }

    private fun configureFemaleVoiceProfile() {
        val tts = textToSpeech ?: return

        // Set AudioAttributes for Media/Assistant playback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts.setAudioAttributes(audioAttributes)
        }

        // Try Indian English first for natural Hinglish & Indian English inflection
        val localeEnIn = Locale("en", "IN")
        val localeHiIn = Locale("hi", "IN")

        var selectedLocale = Locale.getDefault()
        if (tts.isLanguageAvailable(localeEnIn) >= TextToSpeech.LANG_AVAILABLE) {
            selectedLocale = localeEnIn
        } else if (tts.isLanguageAvailable(localeHiIn) >= TextToSpeech.LANG_AVAILABLE) {
            selectedLocale = localeHiIn
        } else if (tts.isLanguageAvailable(Locale.ENGLISH) >= TextToSpeech.LANG_AVAILABLE) {
            selectedLocale = Locale.ENGLISH
        }
        tts.language = selectedLocale

        // Inspect available voices for female identifiers (e.g. "female", "network", "#female_1", "en-in-x-end-network")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val availableVoices = tts.voices
                if (!availableVoices.isNullOrEmpty()) {
                    val preferredVoice = availableVoices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        (name.contains("female") || name.contains("en-in-x-end") || name.contains("en-in-x-enc") || name.contains("hi-in-x-hia") || name.contains("woman")) &&
                                !voice.isNetworkConnectionRequired
                    } ?: availableVoices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        name.contains("female") || name.contains("woman") || name.contains("#female")
                    } ?: availableVoices.firstOrNull { voice ->
                        voice.locale.language == "en" && voice.locale.country == "IN"
                    }

                    if (preferredVoice != null) {
                        tts.voice = preferredVoice
                    }
                }
            } catch (e: Exception) {
                // Fallback to default voice with pitch calibration
            }
        }

        tts.setPitch(basePitch)
        tts.setSpeechRate(currentSpeed * 1.02f)

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeakingFlow.value = true
                onStartCallback?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                _isSpeakingFlow.value = false
                abandonAudioFocus()
                onDoneCallback?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeakingFlow.value = false
                abandonAudioFocus()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeakingFlow.value = false
                abandonAudioFocus()
            }
        })
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
        val tts = textToSpeech ?: return
        if (!_isReadyFlow.value) return

        // Respect Android silent mode and DND
        if (isMutedOrDnd()) {
            onDone?.invoke()
            return
        }

        val cleaned = sanitizeSpokenText(text)
        if (cleaned.isBlank()) {
            onDone?.invoke()
            return
        }

        requestAudioFocus()

        this.onStartCallback = onStart
        this.onDoneCallback = onDone

        // Calibrate pitch and speech rate based on emotion
        val targetPitch = (basePitch * emotion.pitchMultiplier).coerceIn(0.7f, 1.8f)
        val targetRate = (currentSpeed * emotion.speedMultiplier).coerceIn(0.6f, 1.6f)

        tts.setPitch(targetPitch)
        tts.setSpeechRate(targetRate)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume.coerceIn(0f, 1.0f))
        }

        val utteranceId = "NOVA_VOICE_${System.currentTimeMillis()}"
        tts.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    override fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeakingFlow.value = false
        abandonAudioFocus()
    }

    override fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true || _isSpeakingFlow.value
    }

    override fun setSpeechSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(currentSpeed * 1.02f)
    }

    override fun setPitch(pitch: Float) {
        basePitch = pitch.coerceIn(0.7f, 1.6f)
        textToSpeech?.setPitch(basePitch)
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1.0f)
    }

    override fun setSpeed(speed: Float) {
        setSpeechSpeed(speed)
    }

    override fun stop() {
        stopSpeaking()
    }

    override fun release() {
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
        _isReadyFlow.value = false
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

    private fun sanitizeSpokenText(text: String): String {
        return text
            .replace(Regex("""\[ACTION:.*?\]"""), "")
            .replace(Regex("""\[MEMORY:.*?\]"""), "")
            .replace(Regex("""[*#_`~>\[\]\(\)\{\}]"""), " ")
            .replace("->", " gives ")
            .replace("=>", " implies ")
            .replace("!=", " not equal to ")
            .replace("<=", " less than or equal to ")
            .replace(">=", " greater than or equal to ")
            .replace("approx", "approximately")
            .replace("mins", "minutes")
            .replace("hr", "hour")
            .replace("hrs", "hours")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
