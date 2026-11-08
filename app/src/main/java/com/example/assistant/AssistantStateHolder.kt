package com.example.assistant

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global application state holder coordinating the Foreground Service,
 * Speech/Wake-Word Engine, Gemini Client, and Compose UI.
 */
object AssistantStateHolder {

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _assistantState = MutableStateFlow(WakeWordState.IDLE)
    val assistantState: StateFlow<WakeWordState> = _assistantState.asStateFlow()

    private val _liveAmplitude = MutableStateFlow(0f)
    val liveAmplitude: StateFlow<Float> = _liveAmplitude.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastAssistantResponse = MutableStateFlow("Namaste! I'm Mahi. Say \"Hey Mahi\" or tap the orb to talk.")
    val lastAssistantResponse: StateFlow<String> = _lastAssistantResponse.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AssistantLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AssistantLanguage> = _selectedLanguage.asStateFlow()

    private val _sensitivity = MutableStateFlow(0.6f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _lastActionSummary = MutableStateFlow<String?>(null)
    val lastActionSummary: StateFlow<String?> = _lastActionSummary.asStateFlow()

    // Event signals
    private val _uiEvents = MutableSharedFlow<AssistantUiEvent>(extraBufferCapacity = 10)
    val uiEvents: SharedFlow<AssistantUiEvent> = _uiEvents.asSharedFlow()

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
        if (!active && _assistantState.value != WakeWordState.PROCESSING_RESPONSE && _assistantState.value != WakeWordState.SPEAKING) {
            _assistantState.value = WakeWordState.IDLE
            _liveAmplitude.value = 0f
        }
    }

    fun setAssistantState(state: WakeWordState) {
        _assistantState.value = state
    }

    fun setLiveAmplitude(amplitude: Float) {
        if (!_isMicMuted.value) {
            _liveAmplitude.value = amplitude
        } else {
            _liveAmplitude.value = 0f
        }
    }

    fun setCurrentTranscript(transcript: String) {
        _currentTranscript.value = transcript
    }

    fun setLastResponse(response: String) {
        _lastAssistantResponse.value = response
    }

    fun toggleMicMute() {
        _isMicMuted.value = !_isMicMuted.value
        if (_isMicMuted.value) {
            _liveAmplitude.value = 0f
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
    }

    fun setLanguage(language: AssistantLanguage) {
        _selectedLanguage.value = language
    }

    fun setSensitivity(value: Float) {
        _sensitivity.value = value.coerceIn(0.1f, 1.0f)
    }

    fun setLastActionSummary(action: String?) {
        _lastActionSummary.value = action
    }

    fun emitEvent(event: AssistantUiEvent) {
        _uiEvents.tryEmit(event)
    }
}

sealed class AssistantUiEvent {
    data object RequestStartQueryListening : AssistantUiEvent()
    data object RequestStopListening : AssistantUiEvent()
    data object RequestWakeWordSimulation : AssistantUiEvent()
    data class SubmitTextQuery(val text: String) : AssistantUiEvent()
}
