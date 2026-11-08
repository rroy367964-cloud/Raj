package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.IndigoVibrant
import com.example.ui.theme.VioletNeon
import kotlin.math.sin

@Composable
fun LiveWaveform(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    height: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.65f
        val spacing = (totalWidth / barCount) * 0.35f
        val centerY = canvasHeight / 2f

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount
            val wave = sin((normalizedIndex * 4.0 * Math.PI) + phase).toFloat()

            val baseH = if (isActive) {
                val ampFactor = (amplitude * canvasHeight * 0.85f).coerceAtLeast(6.dp.toPx())
                (ampFactor * (0.4f + 0.6f * ((wave + 1f) / 2f))).coerceIn(4.dp.toPx(), canvasHeight * 0.95f)
            } else {
                3.dp.toPx()
            }

            val x = i * (barWidth + spacing) + spacing / 2f
            val y = centerY - (baseH / 2f)

            val barColor = if (isActive) {
                Brush.verticalGradient(
                    colors = listOf(
                        CyanElectric,
                        IndigoVibrant,
                        VioletNeon
                    ),
                    startY = y,
                    endY = y + baseH
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF334155),
                        Color(0xFF1E293B)
                    )
                )
            }

            drawRoundRect(
                brush = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, baseH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
