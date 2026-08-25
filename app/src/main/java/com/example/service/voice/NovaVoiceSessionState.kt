package com.example.service.voice

/**
 * Standard lifecycle states for NOVA voice synthesis and playback session.
 */
enum class NovaVoiceSessionState {
    /** Voice engine is idle, waiting for commands. */
    IDLE,

    /** Audio is being synthesized or fetched from ElevenLabs / cache. */
    GENERATING,

    /** Voice is actively playing audio. */
    PLAYING,

    /** Voice playback has been paused by user. */
    PAUSED,

    /** Voice playback has been manually stopped. */
    STOPPED,

    /** Voice generation or playback encountered an error. */
    ERROR
}
