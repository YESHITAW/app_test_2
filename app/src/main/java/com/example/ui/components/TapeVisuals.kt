package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas dual-reel cassette tape indicator that spins smoothly when active/recording/playing.
 */
@Composable
fun CassetteReelDeckVisual(
    isSpinning: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    val transition = rememberInfiniteTransition(label = "cassette_spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reel_angle"
    )
    val activeAngle = if (isSpinning) angle else 0f
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val radius = (h * 0.38f).coerceAtLeast(14f)
        val leftCenter = Offset(w * 0.27f, h * 0.5f)
        val rightCenter = Offset(w * 0.73f, h * 0.5f)

        // Tape bridge housing
        drawRoundRect(
            color = surfaceColor,
            topLeft = Offset(0f, h * 0.08f),
            size = Size(w, h * 0.84f),
            cornerRadius = CornerRadius(h * 0.25f, h * 0.25f)
        )
        drawRoundRect(
            color = outlineColor.copy(alpha = 0.55f),
            topLeft = Offset(0f, h * 0.08f),
            size = Size(w, h * 0.84f),
            cornerRadius = CornerRadius(h * 0.25f, h * 0.25f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Magnetic tape ribbon connecting reels
        drawLine(
            color = accentColor.copy(alpha = if (isSpinning) 0.7f else 0.3f),
            start = Offset(leftCenter.x, leftCenter.y + radius * 0.7f),
            end = Offset(rightCenter.x, rightCenter.y + radius * 0.7f),
            strokeWidth = 2.dp.toPx()
        )

        // Center window ticks
        val windowLeft = w * 0.39f
        val windowRight = w * 0.61f
        drawRoundRect(
            color = outlineColor.copy(alpha = 0.35f),
            topLeft = Offset(windowLeft, h * 0.32f),
            size = Size(windowRight - windowLeft, h * 0.36f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        listOf(leftCenter, rightCenter).forEach { center ->
            drawCircle(
                color = accentColor.copy(alpha = if (isSpinning) 0.22f else 0.1f),
                radius = radius * 1.15f,
                center = center
            )
            drawCircle(
                color = accentColor,
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            for (i in 0 until 3) {
                val theta = ((activeAngle + i * 120f) * PI / 180.0)
                val spokeEnd = Offset(
                    x = center.x + (radius * 0.82f * cos(theta)).toFloat(),
                    y = center.y + (radius * 0.82f * sin(theta)).toFloat()
                )
                drawLine(
                    color = accentColor,
                    start = center,
                    end = spokeEnd,
                    strokeWidth = 2.dp.toPx()
                )
            }
            drawCircle(
                color = surfaceColor,
                radius = radius * 0.28f,
                center = center
            )
        }
    }
}

/**
 * Real-time segmented LED VU meter driven by MediaRecorder maxAmplitude (0..100).
 */
@Composable
fun LiveVuMeter(
    amplitude: Int,
    modifier: Modifier = Modifier,
    segments: Int = 20
) {
    val activeColor = MaterialTheme.colorScheme.secondary
    val warnColor = MaterialTheme.colorScheme.tertiary
    val peakColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
    ) {
        val gap = 4.dp.toPx()
        val segWidth = (size.width - gap * (segments - 1)) / segments
        val litCount = ((amplitude.coerceIn(0, 100) / 100f) * segments).toInt()

        for (i in 0 until segments) {
            val x = i * (segWidth + gap)
            val ratio = i.toFloat() / segments
            val color = when {
                i >= litCount -> inactiveColor
                ratio > 0.78f -> peakColor
                ratio > 0.55f -> warnColor
                else -> activeColor
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(segWidth.coerceAtLeast(2f), size.height),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }
}

/**
 * Interactive waveform visualizer rendered from real sampled recording peaks, supporting tap-to-seek.
 */
@Composable
fun InteractiveWaveformBar(
    peaksCsv: String,
    progressFraction: Float,
    isActive: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val peaks = rememberParsedPeaks(peaksCsv)
    val playedColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val unplayedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(peaksCsv, isActive) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekFraction(fraction)
                }
            }
    ) {
        val count = peaks.size
        if (count == 0) return@Canvas
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
        val centerY = size.height / 2f

        peaks.forEachIndexed { index, amp ->
            val fraction = index.toFloat() / count.toFloat()
            val barColor = if (fraction <= progressFraction) playedColor else unplayedColor
            val barHeight = (size.height * (amp / 100f)).coerceAtLeast(size.height * 0.16f)
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun rememberParsedPeaks(peaksCsv: String): List<Int> {
    if (peaksCsv.isBlank()) {
        return List(32) { index ->
            val wave = ((sin(index * 0.55) + 1.0) * 30.0 + 22.0).toInt()
            wave.coerceIn(16, 86)
        }
    }
    val parsed = peaksCsv.split(",").mapNotNull { it.trim().toIntOrNull()?.coerceIn(8, 100) }
    return if (parsed.isEmpty()) List(32) { 28 } else parsed
}

fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}

fun formatMillis(millis: Int): String = formatDuration(millis / 1000)
