package com.example.service.voice

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Modular Cloud TTS Provider integrating ElevenLabs Neural Voice synthesis with on-device acoustic fallback.
 * Designed to connect securely to ElevenLabs without exposing API keys to client logs or UI.
 */
class CloudTtsProvider(
    private val context: Context,
    private val elevenLabsEngine: ElevenLabsTtsProvider = ElevenLabsTtsProvider(context)
) : NovaVoiceService, com.example.nova.NovaVoiceService {

    override val isSpeakingFlow: StateFlow<Boolean> = elevenLabsEngine.isSpeakingFlow
    override val isReadyFlow: StateFlow<Boolean> = elevenLabsEngine.isReadyFlow
    override val sessionStateFlow: StateFlow<NovaVoiceSessionState> = elevenLabsEngine.sessionState
    override val audioLevelRmsFlow: StateFlow<Float> = elevenLabsEngine.audioLevelRms

    override fun speak(text: String) {
        elevenLabsEngine.speak(text)
    }

    override fun speak(
        text: String,
        emotion: NovaVoiceEmotion,
        onStart: (() -> Unit)?,
        onDone: (() -> Unit)?
    ) {
        elevenLabsEngine.speak(text, emotion, onStart, onDone)
    }

    override fun stopSpeaking() {
        elevenLabsEngine.stopSpeaking()
    }

    override fun pause() {
        elevenLabsEngine.pause()
    }

    override fun resume() {
        elevenLabsEngine.resume()
    }

    override fun replay() {
        elevenLabsEngine.replay()
    }

    override fun stop() {
        elevenLabsEngine.stop()
    }

    override fun isSpeaking(): Boolean {
        return elevenLabsEngine.isSpeaking()
    }

    override fun setSpeechSpeed(speed: Float) {
        elevenLabsEngine.setSpeechSpeed(speed)
    }

    override fun setSpeed(speed: Float) {
        elevenLabsEngine.setSpeed(speed)
    }

    override fun setPitch(pitch: Float) {
        elevenLabsEngine.setPitch(pitch)
    }

    override fun setVolume(volume: Float) {
        elevenLabsEngine.setVolume(volume)
    }

    override fun release() {
        elevenLabsEngine.release()
    }
}
