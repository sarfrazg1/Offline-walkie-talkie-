package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PttGreen
import com.example.ui.theme.RadioReceiveCyan
import kotlin.random.Random

/**
 * Animated multi-bar audio spectrum visualizer.
 * Displays fluctuating sound levels when transmitting or receiving live voice.
 */
@Composable
fun AudioVisualizer(
    level: Float,
    isActive: Boolean,
    isReceiving: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = if (isReceiving) RadioReceiveCyan else PttGreen
    val barCount = 18

    val infiniteTransition = rememberInfiniteTransition(label = "viz_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animatable = remember { Animatable(0.12f) }

            LaunchedEffect(level, isActive, phase) {
                if (isActive) {
                    val distanceFromCenter = kotlin.math.abs(i - (barCount / 2f)) / (barCount / 2f)
                    val bellCurve = 1.0f - (distanceFromCenter * 0.6f)
                    val variance = (Random.nextFloat() * 0.35f + 0.65f)
                    val target = ((level * 1.5f + 0.15f) * bellCurve * variance).coerceIn(0.12f, 1.0f)
                    animatable.animateTo(
                        targetValue = target,
                        animationSpec = tween(75, easing = FastOutSlowInEasing)
                    )
                } else {
                    animatable.animateTo(
                        targetValue = 0.12f,
                        animationSpec = tween(250)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(animatable.value)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isActive) barColor else barColor.copy(alpha = 0.2f)
                    )
            )
        }
    }
}
