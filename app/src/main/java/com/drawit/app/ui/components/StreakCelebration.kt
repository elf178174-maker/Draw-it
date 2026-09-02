package com.drawit.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawit.app.R
import com.drawit.app.data.StreakState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class Flake(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val ratio: Float,
    val spin: Float,
    val drift: Float,
    val color: Color,
    val delay: Float
)

/**
 * The moment after a drawing lands: rings push out, paper flakes scatter, and
 * the number rolls up. Milestones get more of everything.
 */
@Composable
fun StreakCelebration(
    streak: Int,
    milestone: Boolean,
    isRecord: Boolean,
    onPlayFeedback: () -> Unit,
    onDone: () -> Unit
) {
    val scrim = remember { Animatable(0f) }
    val ring = remember { Animatable(0.55f) }
    val burst = remember { Animatable(0f) }
    val count = remember { Animatable((streak - 1).coerceAtLeast(0).toFloat()) }
    val pop = remember { Animatable(1f) }
    val particles = remember { Animatable(0f) }
    val label = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }

    val accent = MaterialTheme.colorScheme.primary
    val flakeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.primaryContainer
    )

    val flakes = remember(milestone) {
        val rng = Random(streak * 31 + if (milestone) 7 else 0)
        val flakeCount = if (milestone) 46 else 28
        List(flakeCount) {
            Flake(
                angle = rng.nextFloat() * 360f,
                speed = 0.55f + rng.nextFloat() * 0.75f,
                size = 5f + rng.nextFloat() * 7f,
                ratio = 0.35f + rng.nextFloat() * 0.5f,
                spin = (rng.nextFloat() - 0.5f) * 900f,
                drift = (rng.nextFloat() - 0.5f) * 0.4f,
                color = flakeColors[rng.nextInt(flakeColors.size)],
                delay = rng.nextFloat() * 0.14f
            )
        }
    }

    LaunchedEffect(Unit) {
        launch { scrim.animateTo(1f, tween(200)) }
        launch {
            delay(140)
            burst.animateTo(1f, tween(if (milestone) 900 else 700, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(120)
            particles.animateTo(1f, tween(if (milestone) 1900 else 1500, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(200)
            onPlayFeedback()
        }
        launch {
            delay(430)
            label.animateTo(1f, tween(420))
        }
        ring.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        count.animateTo(streak.toFloat(), tween(340, easing = FastOutSlowInEasing))
        pop.animateTo(1.16f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessHigh))
        pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
        delay(if (milestone) 1500 else 1050)
        exit.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        onDone()
    }

    val shown = count.value.roundToInt()

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f * scrim.value))
            .pressable(scaleDown = 1f) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        // Rings and flakes share the centre of the screen.
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f - 40.dp.toPx())
            val maxR = size.minDimension * 0.46f

            // Two rings pushing outwards.
            listOf(0f, 0.18f).forEach { offset ->
                val p = ((burst.value - offset) / (1f - offset)).coerceIn(0f, 1f)
                if (p > 0f) {
                    drawCircle(
                        color = accent.copy(alpha = 0.30f * (1f - p)),
                        radius = 54.dp.toPx() + p * maxR * 0.72f,
                        center = centre,
                        style = Stroke(width = (2.5f - 1.6f * p).coerceAtLeast(0.6f) * density)
                    )
                }
            }

            flakes.forEach { flake ->
                val p = ((particles.value - flake.delay) / (1f - flake.delay)).coerceIn(0f, 1f)
                if (p <= 0f) return@forEach
                val eased = 1f - (1f - p) * (1f - p)
                val rad = Math.toRadians(flake.angle.toDouble())
                val distance = eased * maxR * flake.speed
                val gravity = p * p * size.height * 0.16f
                val x = centre.x + cos(rad).toFloat() * distance + flake.drift * distance
                val y = centre.y + sin(rad).toFloat() * distance + gravity
                val alpha = (1f - p * p).coerceIn(0f, 1f)
                val w = flake.size * density
                val h = w * flake.ratio

                rotate(degrees = flake.spin * p, pivot = Offset(x, y)) {
                    drawRoundRect(
                        color = flake.color.copy(alpha = alpha * 0.9f),
                        topLeft = Offset(x - w / 2f, y - h / 2f),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 80.dp)
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = ring.value
                        scaleY = ring.value
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flame),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "$shown",
                fontSize = 86.sp,
                lineHeight = 92.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
            )

            Text(
                text = if (shown == 1) "day in a row" else "days in a row",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = label.value }
            ) {
                if (milestone) {
                    Text(
                        text = StreakState.milestoneLabel(streak),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (isRecord && streak > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_spark),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "Your longest run yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    val next = StreakState.nextMilestone(streak)
                    Text(
                        text = next?.let {
                            val away = it - streak
                            if (away == 1) "One more day to ${StreakState.milestoneLabel(it).lowercase()}"
                            else "$away days to ${StreakState.milestoneLabel(it).lowercase()}"
                        } ?: "Keep going.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
