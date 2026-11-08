package com.example.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

/**
 * Adapter interface for wake-word engines (e.g. Porcupine, custom TFLite, or acoustic keyword spotter).
 */
interface WakeWordEngine {
    val state: StateFlow<WakeWordState>
    val audioAmplitude: StateFlow<Float>
    val sensitivity: StateFlow<Float>
    val lastDetectedPhrase: StateFlow<String>

    fun setSensitivity(value: Float)
    fun startListening(onWakeWordDetected: () -> Unit)
    fun stopListening()
    fun triggerManualWake()
    fun release()
}

/**
 * Concrete implementation of WakeWordEngine providing real-time audio amplitude analysis
 * via AudioRecord and keyword matching using the Android SpeechRecognizer adapter.
 */
class AcousticWakeWordEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : WakeWordEngine {

    private val tag = "AcousticWakeWordEngine"

    private val _state = MutableStateFlow(WakeWordState.IDLE)
    override val state: StateFlow<WakeWordState> = _state.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    override val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _sensitivity = MutableStateFlow(0.6f)
    override val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _lastDetectedPhrase = MutableStateFlow("")
    override val lastDetectedPhrase: StateFlow<String> = _lastDetectedPhrase.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var amplitudeJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var onWakeWordCallback: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private var lastTriggerTime = 0L
    private val triggerCooldownMs = 2000L

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val WAKE_WORDS = listOf("hey mahi", "mahi", "suno mahi", "namaste mahi", "hello mahi", "hi mahi")
    }

    override fun setSensitivity(value: Float) {
        _sensitivity.value = value.coerceIn(0.1f, 1.0f)
    }

    override fun startListening(onWakeWordDetected: () -> Unit) {
        if (isRunning) return
        isRunning = true
        onWakeWordCallback = onWakeWordDetected
        _state.value = WakeWordState.LISTENING_FOR_WAKE_WORD

        startAudioCapture()
        startSpeechKeywordDetector()
    }

    override fun stopListening() {
        isRunning = false
        _state.value = WakeWordState.IDLE
        _audioAmplitude.value = 0f
        stopAudioCapture()
        stopSpeechKeywordDetector()
    }

    override fun triggerManualWake() {
        Log.d(tag, "Manual wake triggered by user")
        _lastDetectedPhrase.value = "Hey Mahi"
        _state.value = WakeWordState.WAKE_WORD_DETECTED
        onWakeWordCallback?.invoke()
    }

    private fun startAudioCapture() {
        stopAudioCapture()
        amplitudeJob = scope.launch(Dispatchers.IO) {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = maxOf(minBufferSize, 2048)
                val buffer = ShortArray(bufferSize)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(tag, "AudioRecord failed to initialize, relying on speech recognizer")
                    return@launch
                }

                audioRecord?.startRecording()

                while (isActive && isRunning) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)
                        // Scale RMS to a normalized 0f..1f with non-linear boost
                        val normalized = (rms / 32767.0 * 8.0).toFloat().coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized
                    }
                }
            } catch (e: SecurityException) {
                Log.e(tag, "Microphone permission denied for AudioRecord", e)
            } catch (e: Exception) {
                Log.e(tag, "Error in audio capture loop", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (ignored: Exception) {}
                audioRecord = null
            }
        }
    }

    private fun stopAudioCapture() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
    }

    private fun startSpeechKeywordDetector() {
        mainHandler.post {
            if (!isRunning) return@post
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {
                                // Fallback amplitude if AudioRecord is blocked
                                if (_audioAmplitude.value == 0f && rmsdB > 0) {
                                    val amp = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                                    _audioAmplitude.value = amp
                                }
                            }
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            override fun onError(error: Int) {
                                // If listening ends or errors, restart loop if still running
                                restartKeywordLoop()
                            }
                            override fun onResults(results: Bundle?) {
                                handleRecognitionBundle(results)
                                restartKeywordLoop()
                            }
                            override fun onPartialResults(partialResults: Bundle?) {
                                handleRecognitionBundle(partialResults)
                            }
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                    restartRecognitionIntent()
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not start speech keyword detector: ${e.message}")
            }
        }
    }

    private fun restartRecognitionIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error starting recognition intent", e)
        }
    }

    private fun restartKeywordLoop() {
        if (!isRunning) return
        mainHandler.postDelayed({
            if (isRunning) {
                restartRecognitionIntent()
            }
        }, 300)
    }

    private fun handleRecognitionBundle(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (text in matches) {
            val lower = text.lowercase(Locale.ROOT).trim()
            for (keyword in WAKE_WORDS) {
                if (lower.contains(keyword)) {
                    val now = System.currentTimeMillis()
                    if (now - lastTriggerTime > triggerCooldownMs) {
                        lastTriggerTime = now
                        _lastDetectedPhrase.value = lower
                        _state.value = WakeWordState.WAKE_WORD_DETECTED
                        Log.i(tag, "Wake word matched: '$keyword' in '$lower'")
                        onWakeWordCallback?.invoke()
                        return
                    }
                }
            }
        }
    }

    private fun stopSpeechKeywordDetector() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (ignored: Exception) {}
            speechRecognizer = null
        }
    }

    override fun release() {
        stopListening()
    }
}
