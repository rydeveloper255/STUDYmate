package com.example.service.voice

enum class NovaVoiceEmotion(val label: String, val pitchMultiplier: Float, val speedMultiplier: Float) {
    CALM("Calm & Natural", 1.12f, 1.02f),
    GENTLE_MOTIVATION("Gentle & Motivating", 1.08f, 0.98f),
    HAPPY_ACHIEVEMENT("Happy & Energetic", 1.20f, 1.06f),
    WARNING("Calm & Clear", 1.04f, 0.95f)
}
