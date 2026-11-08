package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantLanguage
import com.example.assistant.WakeWordState
import com.example.ui.AssistantViewModel
import com.example.ui.components.ConversationBubble
import com.example.ui.components.LiveWaveform
import com.example.ui.components.MahiOrb
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IndigoVibrant
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAssistantScreen(
    viewModel: AssistantViewModel,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val liveAmplitude by viewModel.liveAmplitude.collectAsState()
    val currentTranscript by viewModel.currentTranscript.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val conversations by viewModel.conversations.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showLangMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to top of list when new message arrives
    LaunchedEffect(conversations.size) {
        if (conversations.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val statusDotColor by animateColorAsState(
        targetValue = when {
            isMicMuted -> Color(0xFFEF4444)
            assistantState == WakeWordState.SPEAKING -> Color(0xFFA855F7)
            assistantState == WakeWordState.LISTENING_FOR_QUERY -> CyanElectric
            assistantState == WakeWordState.WAKE_WORD_DETECTED -> EmeraldActive
            isServiceActive -> EmeraldActive
            else -> Color(0xFF64748B)
        },
        label = "StatusDotColor"
    )

    val statusLabel = when {
        isMicMuted -> "Microphone Muted"
        assistantState == WakeWordState.LISTENING_FOR_QUERY -> "Listening to you…"
        assistantState == WakeWordState.PROCESSING_RESPONSE -> "Thinking & Reasoning…"
        assistantState == WakeWordState.SPEAKING -> "Mahi is Speaking…"
        assistantState == WakeWordState.WAKE_WORD_DETECTED -> "Wake Word Detected!"
        isServiceActive -> "Listening for \"Hey Mahi\""
        else -> "Foreground Service Standby"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar / Status Row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                }

                // Controls: Language, TTS, Mic, Power
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Language Dropdown
                    Box {
                        IconButton(
                            onClick = { showLangMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark)
                                .border(1.dp, SurfaceCardBorder, CircleShape)
                                .testTag("btn_language_select")
                        ) {
                            Text(
                                text = selectedLanguage.flag,
                                fontSize = 16.sp
                            )
                        }

                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            AssistantLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${lang.flag} ${lang.displayName}",
                                            color = if (selectedLanguage == lang) CyanElectric else TextPrimaryDark
                                        )
                                    },
                                    onClick = {
                                        viewModel.setLanguage(lang)
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // TTS Toggle
                    IconButton(
                        onClick = { viewModel.toggleTts() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceCardBorder, CircleShape)
                            .testTag("btn_toggle_tts")
                    ) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle TTS",
                            tint = if (isTtsEnabled) CyanElectric else TextMutedDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Mic Mute Toggle
                    IconButton(
                        onClick = { viewModel.toggleMicMute() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceCardBorder, CircleShape)
                            .testTag("btn_toggle_mic")
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute Microphone",
                            tint = if (isMicMuted) Color(0xFFEF4444) else CyanElectric,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Foreground Service Switch
                    IconButton(
                        onClick = {
                            onRequestMicPermission()
                            viewModel.toggleService()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isServiceActive) EmeraldActive.copy(alpha = 0.2f) else SurfaceDark
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                if (isServiceActive) EmeraldActive else SurfaceCardBorder,
                                CircleShape
                            )
                            .testTag("btn_toggle_service")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Toggle Foreground Service",
                            tint = if (isServiceActive) EmeraldActive else TextMutedDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Center Interactive Orb
                    MahiOrb(
                        state = assistantState,
                        amplitude = liveAmplitude,
                        isMicMuted = isMicMuted,
                        isSpeaking = assistantState == WakeWordState.SPEAKING,
                        onClick = {
                            onRequestMicPermission()
                            viewModel.onOrbClicked()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audio Waveform Visualizer
                    LiveWaveform(
                        amplitude = liveAmplitude,
                        isActive = isServiceActive || assistantState == WakeWordState.LISTENING_FOR_QUERY || assistantState == WakeWordState.SPEAKING,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .height(36.dp)
                    )

                    // Wake-Word Hint & Quick Simulator Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (isServiceActive) "Say \"Hey Mahi\" or tap orb to speak" else "Tap orb or turn on service to listen",
                            fontSize = 13.sp,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .clickable {
                                    onRequestMicPermission()
                                    viewModel.simulateWakeWord()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("btn_simulate_wake")
                        ) {
                            Text(
                                text = "⚡ Trigger",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanElectric
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Speech Transcript Banner
                    AnimatedVisibility(
                        visible = currentTranscript.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, CyanElectric.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = CyanElectric,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "\"$currentTranscript\"",
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Action Recommendation Chips
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickChips = listOf(
                            "📷 Open Camera" to "open camera",
                            "▶️ Play Music" to "play music",
                            "📺 Open YouTube" to "open youtube",
                            "💬 WhatsApp" to "open whatsapp",
                            "⏰ 5 Min Timer" to "set a timer for 5 minutes",
                            "✨ Namaste Mahi" to "namaste mahi, kaise ho?"
                        )

                        quickChips.forEach { (label, command) ->
                            AssistChip(
                                onClick = {
                                    viewModel.processUserQuery(command)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        color = TextPrimaryDark
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SurfaceDark
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = SurfaceCardBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (conversations.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONVERSATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                color = TextSecondaryDark,
                                modifier = Modifier
                                    .clickable { viewModel.clearHistory() }
                                    .padding(4.dp)
                                    .testTag("btn_clear_history")
                            )
                        }
                    }
                }

                // Conversation History Items
                items(conversations, key = { it.id }) { item ->
                    ConversationBubble(
                        conversation = item,
                        onSpeakAgain = { text ->
                            viewModel.ttsManager.speak(text, selectedLanguage)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Bottom Text & Voice Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 4.dp),
                color = SurfaceDark,
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onRequestMicPermission()
                            viewModel.startListeningQuery()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (assistantState == WakeWordState.LISTENING_FOR_QUERY) CyanElectric else Color(0xFF1E293B)
                            )
                            .testTag("btn_bottom_mic")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speak to Mahi",
                            tint = if (assistantState == WakeWordState.LISTENING_FOR_QUERY) DeepSpace else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = "Ask Mahi or give a command…",
                                fontSize = 14.sp,
                                color = TextMutedDark
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            cursorColor = CyanElectric
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.processUserQuery(textInput)
                                    textInput = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_query_text")
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.processUserQuery(textInput)
                                textInput = ""
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_send_query")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Query",
                            tint = if (textInput.isNotBlank()) CyanElectric else TextMutedDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
