package com.example.service.voice

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Supported TTS vendor/provider engines for NOVA voice synthesis.
 */
enum class NovaVoiceProviderType(val displayName: String, val description: String) {
    ANDROID_ACOUSTIC("On-Device Acoustic Voice", "Low-latency offline TTS with tuned young-adult female AI acoustic parameters"),
    CLOUD_NEURAL("Cloud Neural Voice", "High-fidelity cloud-rendered voice synthesis with fallback to on-device engine")
}

/**
 * Clean modular abstraction for NOVA's voice synthesis.
 * Decouples NOVA's core brain and UI from any specific TTS vendor, engine, or cloud provider.
 */
interface NovaVoiceService {
    val isSpeakingFlow: StateFlow<Boolean>
    val isReadyFlow: StateFlow<Boolean>

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
        providerType: NovaVoiceProviderType = NovaVoiceProviderType.ANDROID_ACOUSTIC
    ): NovaVoiceService {
        return when (providerType) {
            NovaVoiceProviderType.ANDROID_ACOUSTIC -> AndroidAcousticTtsProvider(context)
            NovaVoiceProviderType.CLOUD_NEURAL -> CloudTtsProvider(context)
        }
    }
}
