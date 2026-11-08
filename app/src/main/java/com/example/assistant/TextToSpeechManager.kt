package com.example.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "TextToSpeechManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.05f) // Warm, friendly tone for Mahi
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    AssistantStateHolder.setAssistantState(WakeWordState.SPEAKING)
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    if (AssistantStateHolder.isServiceActive.value) {
                        AssistantStateHolder.setAssistantState(WakeWordState.LISTENING_FOR_WAKE_WORD)
                    } else {
                        AssistantStateHolder.setAssistantState(WakeWordState.IDLE)
                    }
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            Log.i(tag, "TextToSpeech initialized successfully")
        } else {
            Log.e(tag, "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String, language: AssistantLanguage = AssistantLanguage.ENGLISH, onComplete: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready yet")
            return
        }

        // Interrupt any ongoing utterance immediately
        stop()

        try {
            val targetLocale = when (language) {
                AssistantLanguage.HINDI -> Locale("hi", "IN")
                AssistantLanguage.HINGLISH -> Locale("en", "IN")
                AssistantLanguage.ENGLISH -> Locale.US
            }

            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default
                tts?.setLanguage(Locale.getDefault())
            }

            val utteranceId = "mahi_speech_${System.currentTimeMillis()}"
            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e(tag, "Error during TTS speak", e)
            _isSpeaking.value = false
        }
    }

    fun stop() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun release() {
        try {
            stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }
}
