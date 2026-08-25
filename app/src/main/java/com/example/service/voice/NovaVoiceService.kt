package com.example.service.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported TTS vendor/provider engines for NOVA voice synthesis.
 */
enum class NovaVoiceProviderType(val displayName: String, val description: String) {
    ELEVENLABS("ElevenLabs Neural Voice", "Ultra-natural human conversational speech (Hindi, Hinglish & English) with low latency"),
    ON_DEVICE_ACOUSTIC("On-Device Acoustic Voice", "Low-latency offline TTS with tuned young-adult female AI acoustic parameters"),
    CLOUD_NEURAL("ElevenLabs Neural Voice", "Ultra-natural human conversational speech with on-device fallback")
}

/**
 * Clean modular abstraction for NOVA's voice synthesis.
 * Decouples NOVA's core brain and UI from any specific TTS vendor, engine, or cloud provider.
 */
interface NovaVoiceService {
    val isSpeakingFlow: StateFlow<Boolean>
    val isReadyFlow: StateFlow<Boolean>
    val sessionStateFlow: StateFlow<NovaVoiceSessionState> get() = MutableStateFlow(NovaVoiceSessionState.IDLE).asStateFlow()
    val audioLevelRmsFlow: StateFlow<Float> get() = MutableStateFlow(0f).asStateFlow()

    /**
     * Synthesize and speak text using the configured voice profile and emotion inflection.
     */
    fun speak(
        text: String,
        emotion: NovaVoiceEmotion = NovaVoiceEmotion.CALM,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    )

    /**
     * Immediately stop any in-progress voice playback and release audio focus.
     */
    fun stopSpeaking()

    /**
     * Pause active speech playback.
     */
    fun pause() {}

    /**
     * Resume paused speech playback.
     */
    fun resume() {}

    /**
     * Replay last spoken phrase.
     */
    fun replay() {}

    /**
     * Check whether speech synthesis is actively playing.
     */
    fun isSpeaking(): Boolean

    /**
     * Adjust speech speed multiplier (e.g., 0.75x to 1.5x).
     */
    fun setSpeechSpeed(speed: Float)

    /**
     * Adjust pitch multiplier (e.g., 0.8x to 1.4x).
     */
    fun setPitch(pitch: Float)

    /**
     * Adjust playback volume (0.0 to 1.0).
     */
    fun setVolume(volume: Float)

    /**
     * Release system TTS resources when lifecycle ends.
     */
    fun release()
}

/**
 * Factory for creating and swapping NovaVoiceService implementations.
 */
object NovaVoiceServiceFactory {
    fun create(
        context: Context,
        providerType: NovaVoiceProviderType = NovaVoiceProviderType.ELEVENLABS
    ): NovaVoiceService {
        return when (providerType) {
            NovaVoiceProviderType.ELEVENLABS,
            NovaVoiceProviderType.CLOUD_NEURAL -> ElevenLabsTtsProvider(context)
            NovaVoiceProviderType.ON_DEVICE_ACOUSTIC -> AndroidAcousticTtsProvider(context)
        }
    }
}

