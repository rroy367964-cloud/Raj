package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.assistant.WakeWordState
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IndigoVibrant
import com.example.ui.theme.VioletNeon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MahiOrb(
    state: WakeWordState,
    amplitude: Float,
    isMicMuted: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathScale"
    )

    // Cosmic rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Wave ring ripple
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingPulse"
    )

    val primaryColor = when {
        isMicMuted -> Color(0xFFEF4444)
        state == WakeWordState.SPEAKING -> VioletNeon
        state == WakeWordState.PROCESSING_RESPONSE -> IndigoVibrant
        state == WakeWordState.LISTENING_FOR_QUERY -> CyanElectric
        state == WakeWordState.WAKE_WORD_DETECTED -> EmeraldActive
        state == WakeWordState.LISTENING_FOR_WAKE_WORD -> CyanElectric
        else -> CyanElectric.copy(alpha = 0.8f)
    }

    val secondaryColor = when {
        isMicMuted -> Color(0xFF991B1B)
        state == WakeWordState.SPEAKING -> IndigoVibrant
        state == WakeWordState.LISTENING_FOR_QUERY -> EmeraldActive
        else -> VioletNeon
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .testTag("mahi_orb")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 3.4f
            val audioBoost = (amplitude * 35.dp.toPx()).coerceAtMost(50.dp.toPx())
            val effectiveRadius = (baseRadius * breathScale) + audioBoost

            // Outer Aura Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.55f
                ),
                radius = effectiveRadius * 1.55f,
                center = center
            )

            // Dynamic Acoustic Ripple Rings when active or speaking
            if (state == WakeWordState.LISTENING_FOR_QUERY || state == WakeWordState.SPEAKING || amplitude > 0.05f) {
                val ringRadius = baseRadius * ringPulse + (amplitude * 40.dp.toPx())
                val alpha = (1.4f - ringPulse).coerceIn(0f, 0.8f)
                drawCircle(
                    color = primaryColor.copy(alpha = alpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Secondary echo ring
                val echoRadius = baseRadius * ((ringPulse * 0.85f) + 0.3f)
                val echoAlpha = (1.3f - ringPulse * 0.85f).coerceIn(0f, 0.5f)
                drawCircle(
                    color = secondaryColor.copy(alpha = echoAlpha),
                    radius = echoRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Rotating Cosmic Aura Rays
            rotate(rotation, pivot = center) {
                for (i in 0 until 12) {
                    val angle = (i * 30.0) * (Math.PI / 180.0)
                    val rayLength = effectiveRadius * 1.15f + (amplitude * 20.dp.toPx())
                    val x = center.x + (cos(angle) * rayLength).toFloat()
                    val y = center.y + (sin(angle) * rayLength).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) primaryColor.copy(alpha = 0.4f) else secondaryColor.copy(alpha = 0.3f),
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // Core Holographic Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        primaryColor,
                        secondaryColor,
                        Color(0xFF070B19)
                    ),
                    center = Offset(center.x - effectiveRadius * 0.25f, center.y - effectiveRadius * 0.25f),
                    radius = effectiveRadius
                ),
                radius = effectiveRadius,
                center = center
            )

            // Inner Gloss Specular Arc
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    start = Offset(center.x - effectiveRadius * 0.6f, center.y - effectiveRadius * 0.8f),
                    end = Offset(center.x, center.y)
                ),
                radius = effectiveRadius * 0.8f,
                center = Offset(center.x - effectiveRadius * 0.15f, center.y - effectiveRadius * 0.2f)
            )

            // Ambient Boundary Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f),
                        secondaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                radius = effectiveRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Icon based on current State
        val centerIcon = when {
            isMicMuted -> Icons.Default.MicOff
            isSpeaking -> Icons.Default.RecordVoiceOver
            state == WakeWordState.LISTENING_FOR_QUERY -> Icons.Default.GraphicEq
            state == WakeWordState.PROCESSING_RESPONSE -> Icons.Default.Sensors
            state == WakeWordState.LISTENING_FOR_WAKE_WORD -> Icons.Default.Mic
            else -> Icons.Default.Mic
        }

        Icon(
            imageVector = centerIcon,
            contentDescription = "Mahi Assistant Status",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}
