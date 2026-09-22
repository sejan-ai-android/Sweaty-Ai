package com.example.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.service.AssistantVoiceState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.VioletNeon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: AssistantVoiceState,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Continuous breathing pulse
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Continuous rotation for halo
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Audio reactive boost during listening
    val dynamicRms = remember { Animatable(0f) }
    LaunchedEffect(audioRms, state) {
        if (state == AssistantVoiceState.LISTENING) {
            dynamicRms.animateTo(audioRms.coerceIn(0f, 1f), tween(80))
        } else {
            dynamicRms.animateTo(0f, tween(300))
        }
    }

    val primaryColor = when (state) {
        AssistantVoiceState.IDLE -> CyanNeon
        AssistantVoiceState.LISTENING -> CyanNeon
        AssistantVoiceState.THINKING -> VioletNeon
        AssistantVoiceState.SPEAKING -> EmeraldGlow
        AssistantVoiceState.ERROR -> CoralAlert
    }

    val secondaryColor = when (state) {
        AssistantVoiceState.IDLE -> PurpleNeon
        AssistantVoiceState.LISTENING -> Color(0xFF38BDF8)
        AssistantVoiceState.THINKING -> AmberWarning
        AssistantVoiceState.SPEAKING -> CyanNeon
        AssistantVoiceState.ERROR -> AmberWarning
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
            .testTag("voice_orb_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f

            val effectiveScale = when (state) {
                AssistantVoiceState.LISTENING -> breathingScale + (dynamicRms.value * 0.28f)
                AssistantVoiceState.THINKING -> breathingScale * 0.98f
                AssistantVoiceState.SPEAKING -> breathingScale + 0.08f
                AssistantVoiceState.IDLE -> breathingScale
                AssistantVoiceState.ERROR -> 1.0f
            }

            val currentRadius = baseRadius * effectiveScale

            // Outer Atmospheric Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.38f
                ),
                radius = currentRadius * 1.38f,
                center = center
            )

            // Orbital Ring 1
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f),
                        secondaryColor.copy(alpha = 0.2f),
                        primaryColor.copy(alpha = 0.9f)
                    ),
                    center = center
                ),
                radius = currentRadius * 1.15f,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Dynamic Particle Points along orbital perimeter
            val particleCount = 8
            for (i in 0 until particleCount) {
                val rad = Math.toRadians((rotationAngle + (i * 360f / particleCount)).toDouble())
                val orbitDist = currentRadius * 1.15f
                val px = center.x + (orbitDist * cos(rad)).toFloat()
                val py = center.y + (orbitDist * sin(rad)).toFloat()
                drawCircle(
                    color = primaryColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Core Solid Radiant Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor,
                        Color(0xFF030712)
                    ),
                    center = center,
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // Inner Core Holographic Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = currentRadius * 0.45f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
