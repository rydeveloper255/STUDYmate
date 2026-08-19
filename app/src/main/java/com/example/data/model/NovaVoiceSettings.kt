package com.example.data.model

/**
 * Data class representing the user's voice configuration preferences for NOVA.
 *
 * @property isEnabled Whether voice synthesis playback is enabled.
 * @property volume Volume level for speech playback (0.0f to 1.0f).
 * @property speed Speech rate multiplier (e.g. 0.75f to 1.5f).
 */
data class NovaVoiceSettings(
    val isEnabled: Boolean = true,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f
)
