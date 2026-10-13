package com.drawit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Bit(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val w: Float,
    val h: Float,
    val color: Color,
    val round: Boolean,
    val wobble: Float,
    val delay: Float
)

/**
 * Flat paper confetti bursting from [origin] (fractions of the canvas).
 * [progress] runs 0..1 over the life of the burst; reading it inside the draw
 * lambda keeps it off the composition.
 */
@Composable
fun Confetti(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    seed: Int = 1,
    count: Int = 70,
    origin: Offset = Offset(0.5f, 0.42f),
    colors: List<Color> = listOf(
        Color(0xFFFF5001), Color(0xFFFFC800), Color(0xFF4F7BFF), Color(0xFF3CC85A),
        Color(0xFFFF4B8B), Color(0xFFA560FF), Color(0xFF1CB0F6)
    )
) {
    val bits = remember(seed, count) {
        val rng = Random(seed)
        List(count) {
            Bit(
                angle = -90f + (rng.nextFloat() - 0.5f) * 220f,
                speed = 0.5f + rng.nextFloat() * 0.9f,
                spin = (rng.nextFloat() - 0.5f) * 1400f,
                w = 7f + rng.nextFloat() * 7f,
                h = 4f + rng.nextFloat() * 5f,
                color = colors[rng.nextInt(colors.size)],
                round = rng.nextFloat() < 0.3f,
                wobble = rng.nextFloat() * 6.28f,
                delay = rng.nextFloat() * 0.08f
            )
        }
    }
    Canvas(modifier) {
        val p0 = progress()
        if (p0 <= 0f || p0 >= 1f) return@Canvas
        val o = Offset(size.width * origin.x, size.height * origin.y)
        val reach = size.minDimension * 0.75f
        bits.forEach { b ->
            val p = ((p0 - b.delay) / (1f - b.delay)).coerceIn(0f, 1f)
            if (p <= 0f) return@forEach
            val rad = Math.toRadians(b.angle.toDouble())
            // Fast out, then gravity takes over and the paper flutters down.
            val out = (1f - (1f - p) * (1f - p)) * reach * b.speed
            val fall = p * p * size.height * 0.55f
            val x = o.x + cos(rad).toFloat() * out + sin(p * 9f + b.wobble) * 10f * density * p
            val y = o.y + sin(rad).toFloat() * out + fall
            val alpha = (1f - p * p * p).coerceIn(0f, 1f)
            val w = b.w * density
            val h = b.h * density * (0.4f + 0.6f * kotlin.math.abs(cos(p * 12f + b.wobble)))
            rotate(b.spin * p, Offset(x, y)) {
                if (b.round) drawCircle(b.color.copy(alpha = alpha), w * 0.4f, Offset(x, y))
                else drawRoundRect(b.color.copy(alpha = alpha), Offset(x - w / 2, y - h / 2), Size(w, h), CornerRadius(2f, 2f))
            }
        }
    }
}
