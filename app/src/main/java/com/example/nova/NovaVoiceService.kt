package com.example.nova

/**
 * Abstraction layer for NOVA's modular Text-to-Speech (TTS) architecture.
 * Decouples speech synthesis capabilities from specific TTS providers and AI logic.
 */
interface NovaVoiceService {
    /**
     * Synthesizes and speaks the provided text.
     */
    fun speak(text: String)

    /**
     * Sets the playback speech speed multiplier (e.g., 0.75f to 1.5f).
     */
    fun setSpeed(speed: Float)

    /**
     * Sets the playback volume level (e.g., 0.0f to 1.0f).
     */
    fun setVolume(volume: Float)

    /**
     * Stops any ongoing speech playback immediately.
     */
    fun stop()
}
