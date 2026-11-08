package com.example.assistant

enum class WakeWordState {
    IDLE,
    INITIALIZING,
    LISTENING_FOR_WAKE_WORD,
    WAKE_WORD_DETECTED,
    LISTENING_FOR_QUERY,
    PROCESSING_RESPONSE,
    SPEAKING,
    ERROR
}

enum class AssistantLanguage(val code: String, val displayName: String, val flag: String) {
    ENGLISH("en", "English", "🇺🇸"),
    HINDI("hi", "हिन्दी (Hindi)", "🇮🇳"),
    HINGLISH("en-IN", "Hinglish", "🇮🇳")
}
