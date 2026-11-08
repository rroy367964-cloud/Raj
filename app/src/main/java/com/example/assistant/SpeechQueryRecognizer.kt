package com.example.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechQueryRecognizer(private val context: Context) {

    private val tag = "SpeechQueryRecognizer"
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    fun startListening(
        language: AssistantLanguage = AssistantLanguage.ENGLISH,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        mainHandler.post {
            stopListening()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Speech recognition not available on this device")
                return@post
            }

            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                            _partialTranscript.value = ""
                            AssistantStateHolder.setAssistantState(WakeWordState.LISTENING_FOR_QUERY)
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {
                            if (rmsdB > 0) {
                                val amp = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                                AssistantStateHolder.setLiveAmplitude(amp)
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            AssistantStateHolder.setAssistantState(WakeWordState.PROCESSING_RESPONSE)
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_NETWORK -> "Network required for speech"
                                else -> "Speech recognition error ($error)"
                            }
                            Log.w(tag, "Recognition error: $message")
                            onError(message)
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val finalSpeech = matches?.firstOrNull().orEmpty().trim()
                            if (finalSpeech.isNotEmpty()) {
                                _partialTranscript.value = finalSpeech
                                AssistantStateHolder.setCurrentTranscript(finalSpeech)
                                onResult(finalSpeech)
                            } else {
                                onError("No speech recognized")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull().orEmpty().trim()
                            if (partial.isNotEmpty()) {
                                _partialTranscript.value = partial
                                AssistantStateHolder.setCurrentTranscript(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val targetLocale = when (language) {
                    AssistantLanguage.HINDI -> Locale("hi", "IN")
                    AssistantLanguage.HINGLISH -> Locale("en", "IN")
                    AssistantLanguage.ENGLISH -> Locale.US
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale.toString())
                }

                recognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(tag, "Failed to start speech query listening: ${e.message}", e)
                _isListening.value = false
                onError(e.message ?: "Failed to start speech recognizer")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (ignored: Exception) {}
            recognizer = null
            _isListening.value = false
        }
    }
}
