package com.example.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assistant.AssistantForegroundService
import com.example.assistant.AssistantLanguage
import com.example.assistant.AssistantStateHolder
import com.example.assistant.AssistantUiEvent
import com.example.assistant.DeviceToolsManager
import com.example.assistant.SpeechQueryRecognizer
import com.example.assistant.TextToSpeechManager
import com.example.assistant.WakeWordState
import com.example.data.GeminiRepository
import com.example.data.local.ConversationEntity
import com.example.data.local.MahiDatabase
import com.example.data.local.UserMemoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "AssistantViewModel"
    private val context = application.applicationContext

    private val database = MahiDatabase.getInstance(context)
    private val dao = database.mahiDao()

    val geminiRepository = GeminiRepository(context)
    val toolsManager = DeviceToolsManager(context)
    val ttsManager = TextToSpeechManager(context)
    val queryRecognizer = SpeechQueryRecognizer(context)

    val conversations: StateFlow<List<ConversationEntity>> = dao.getRecentConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userMemories: StateFlow<List<UserMemoryEntity>> = dao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct access to state holder flows for UI convenience
    val isServiceActive = AssistantStateHolder.isServiceActive
    val assistantState = AssistantStateHolder.assistantState
    val liveAmplitude = AssistantStateHolder.liveAmplitude
    val currentTranscript = AssistantStateHolder.currentTranscript
    val lastResponse = AssistantStateHolder.lastAssistantResponse
    val isMicMuted = AssistantStateHolder.isMicMuted
    val isTtsEnabled = AssistantStateHolder.isTtsEnabled
    val selectedLanguage = AssistantStateHolder.selectedLanguage
    val sensitivity = AssistantStateHolder.sensitivity
    val lastActionSummary = AssistantStateHolder.lastActionSummary

    init {
        // Observe UI events from background service or components
        viewModelScope.launch {
            AssistantStateHolder.uiEvents.collect { event ->
                when (event) {
                    is AssistantUiEvent.RequestStartQueryListening -> {
                        triggerHapticFeedback()
                        startListeningQuery()
                    }
                    is AssistantUiEvent.RequestStopListening -> {
                        stopAllListening()
                    }
                    is AssistantUiEvent.RequestWakeWordSimulation -> {
                        simulateWakeWord()
                    }
                    is AssistantUiEvent.SubmitTextQuery -> {
                        processUserQuery(event.text)
                    }
                }
            }
        }
    }

    fun startForegroundService() {
        AssistantForegroundService.start(context)
    }

    fun stopForegroundService() {
        AssistantForegroundService.stop(context)
        ttsManager.stop()
        queryRecognizer.stopListening()
    }

    fun toggleService() {
        if (isServiceActive.value) {
            stopForegroundService()
        } else {
            startForegroundService()
        }
    }

    fun onOrbClicked() {
        triggerHapticFeedback()

        if (ttsManager.isSpeaking.value) {
            // User interrupted speech
            ttsManager.stop()
            return
        }

        if (queryRecognizer.isListening.value) {
            // Stop active listening
            queryRecognizer.stopListening()
            AssistantStateHolder.setAssistantState(
                if (isServiceActive.value) WakeWordState.LISTENING_FOR_WAKE_WORD else WakeWordState.IDLE
            )
            return
        }

        // Start listening to voice query
        startListeningQuery()
    }

    fun startListeningQuery() {
        ttsManager.stop()
        AssistantStateHolder.setCurrentTranscript("")
        AssistantStateHolder.setAssistantState(WakeWordState.LISTENING_FOR_QUERY)

        queryRecognizer.startListening(
            language = selectedLanguage.value,
            onResult = { queryText ->
                processUserQuery(queryText)
            },
            onError = { errMsg ->
                Log.w(tag, "Query recognizer error: $errMsg")
                AssistantStateHolder.setAssistantState(
                    if (isServiceActive.value) WakeWordState.LISTENING_FOR_WAKE_WORD else WakeWordState.IDLE
                )
            }
        )
    }

    fun simulateWakeWord() {
        triggerHapticFeedback()
        AssistantStateHolder.setCurrentTranscript("Hey Mahi (Simulated)")
        AssistantStateHolder.setAssistantState(WakeWordState.WAKE_WORD_DETECTED)
        startListeningQuery()
    }

    fun processUserQuery(queryText: String) {
        if (queryText.isBlank()) return

        viewModelScope.launch {
            try {
                // 1. Record user turn
                val userEntity = ConversationEntity(
                    sender = "user",
                    text = queryText,
                    language = selectedLanguage.value.code
                )
                dao.insertConversation(userEntity)

                AssistantStateHolder.setAssistantState(WakeWordState.PROCESSING_RESPONSE)

                // 2. Query Gemini / Local Engine
                val response = geminiRepository.generateAssistantReply(
                    query = queryText,
                    preferredLanguage = selectedLanguage.value
                )

                // 3. Execute tool if applicable
                var actionSummary: String? = null
                if (response.toolType != null) {
                    val result = when (response.toolType) {
                        "open_camera" -> toolsManager.openCamera()
                        "launch_app" -> toolsManager.launchAppByName(response.toolPayload ?: "youtube")
                        "media_control" -> toolsManager.controlMedia(response.toolPayload ?: "toggle")
                        "set_timer" -> toolsManager.setAlarm(response.toolPayload?.toIntOrNull() ?: 5, "Mahi Timer")
                        "search_web" -> toolsManager.searchWeb(response.toolPayload ?: queryText)
                        "open_settings" -> toolsManager.openSettings(response.toolPayload ?: "wifi")
                        else -> null
                    }
                    if (result is DeviceToolsManager.ToolExecutionResult.Success) {
                        actionSummary = result.actionTitle
                        AssistantStateHolder.setLastActionSummary(actionSummary)
                    }
                }

                // 4. Save Mahi response to DB
                val mahiEntity = ConversationEntity(
                    sender = "mahi",
                    text = response.replyText,
                    actionExecuted = actionSummary,
                    language = response.detectedLanguage.code
                )
                dao.insertConversation(mahiEntity)

                AssistantStateHolder.setLastResponse(response.replyText)

                // 5. Speak out loud if TTS is enabled
                if (isTtsEnabled.value) {
                    ttsManager.speak(response.replyText, response.detectedLanguage)
                } else {
                    AssistantStateHolder.setAssistantState(
                        if (isServiceActive.value) WakeWordState.LISTENING_FOR_WAKE_WORD else WakeWordState.IDLE
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error processing query: ${e.message}", e)
                AssistantStateHolder.setAssistantState(WakeWordState.ERROR)
            }
        }
    }

    fun toggleMicMute() {
        AssistantStateHolder.toggleMicMute()
    }

    fun toggleTts() {
        val next = !isTtsEnabled.value
        AssistantStateHolder.setTtsEnabled(next)
        if (!next) {
            ttsManager.stop()
        }
    }

    fun setLanguage(language: AssistantLanguage) {
        AssistantStateHolder.setLanguage(language)
    }

    fun setSensitivity(value: Float) {
        AssistantStateHolder.setSensitivity(value)
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearConversations()
        }
    }

    fun saveMemory(key: String, value: String) {
        viewModelScope.launch {
            dao.insertMemory(UserMemoryEntity(key = key, value = value))
        }
    }

    fun deleteMemory(key: String) {
        viewModelScope.launch {
            dao.deleteMemoryByKey(key)
        }
    }

    fun stopAllListening() {
        queryRecognizer.stopListening()
        ttsManager.stop()
        AssistantForegroundService.stop(context)
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (ignored: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.release()
        queryRecognizer.stopListening()
    }
}
