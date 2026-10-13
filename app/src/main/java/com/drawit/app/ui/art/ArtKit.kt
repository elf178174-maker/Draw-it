package com.drawit.app.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * Small toolkit shared by every piece of drawn art: Foxy, the avatar, the
 * flames and the currency. Everything is authored in a 200 x 200 unit box and
 * scaled to whatever size it is shown at, so the same code draws a 24dp icon
 * and a full-screen celebration.
 */

const val ART_UNITS = 200f

private val pathCache = HashMap<String, Path>()

/** Parses SVG path data once and reuses it. */
fun svg(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** Runs [block] in art units, centred and scaled to fit the current draw area. */
inline fun DrawScope.inArtUnits(block: DrawScope.() -> Unit) {
    val s = size.minDimension / ART_UNITS
    translate((size.width - ART_UNITS * s) / 2f, (size.height - ART_UNITS * s) / 2f) {
        scale(s, s, pivot = Offset.Zero) { block() }
    }
}

/*
 * House style: flat colour, no gradients, and one hard-edged shadow per
 * shape. [flat] draws the shape in its shadow colour, then the base colour
 * nudged up and to the left inside it, which leaves a crisp crescent of
 * shadow along the lower-right edge.
 */
fun DrawScope.flat(path: Path, base: Color, shade: Color, dx: Float = -3.5f, dy: Float = -4.5f) {
    drawPath(path, shade)
    clipPath(path) { translate(dx, dy) { drawPath(path, base) } }
}

fun DrawScope.flatOval(base: Color, shade: Color, cx: Float, cy: Float, rx: Float, ry: Float, dx: Float = -3f, dy: Float = -3.5f) {
    flat(Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }, base, shade, dx, dy)
}

fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = Offset(cx - rx, cy - ry) to Size(rx * 2, ry * 2)

fun DrawScope.ovalAt(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Float = 1f) {
    drawOval(color = color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2), alpha = alpha)
}

fun Color.lighten(f: Float): Color = lerp(this, Color.White, f)
fun Color.darken(f: Float): Color = lerp(this, Color.Black, f)

/** Smooth 0..1 ramp between [a] and [b]. */
fun smoothstep(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** A soft 0..1..0 bump centred in the window [start, start + length]. */
fun bump(t: Float, start: Float, length: Float): Float {
    if (t < start || t > start + length) return 0f
    return sin(((t - start) / length) * PI.toFloat())
}

fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Deterministic pseudo-random in [0, 1) so animation is stable frame to frame. */
fun hash01(seed: Int): Float {
    var x = seed * 374761393 + 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0x7fffffff) / 2147483648f
}

fun wave(t: Float, period: Float, phase: Float = 0f): Float =
    sin((t / period + phase) * 2f * PI.toFloat())

fun cosWave(t: Float, period: Float, phase: Float = 0f): Float =
    cos((t / period + phase) * 2f * PI.toFloat())

/**
 * A value that wanders between random targets every [interval] seconds,
 * easing smoothly from one to the next. Used for idle glances.
 */
fun wander(t: Float, interval: Float, seed: Int, hold: Float = 0.7f): Float {
    val i = floor(t / interval).toInt()
    val local = t / interval - i
    val from = hash01(seed * 131 + i) * 2f - 1f
    val to = hash01(seed * 131 + i + 1) * 2f - 1f
    // Hold still for most of the interval, then glide.
    return mix(from, to, smoothstep(hold, 1f, local))
}

/** Points on a circle, handy for sparkles and rays. */
fun polar(cx: Float, cy: Float, r: Float, degrees: Float): Offset {
    val rad = degrees * PI.toFloat() / 180f
    return Offset(cx + cos(rad) * r, cy + sin(rad) * r)
}

/** A four-point sparkle star. */
fun sparklePath(cx: Float, cy: Float, r: Float, waist: Float = 0.22f): Path = Path().apply {
    val w = r * waist
    moveTo(cx, cy - r)
    quadraticBezierTo(cx + w, cy - w, cx + r, cy)
    quadraticBezierTo(cx + w, cy + w, cx, cy + r)
    quadraticBezierTo(cx - w, cy + w, cx - r, cy)
    quadraticBezierTo(cx - w, cy - w, cx, cy - r)
    close()
}

/** A five-point star with rounded-ish inner corners. */
fun starPath(cx: Float, cy: Float, outer: Float, inner: Float = outer * 0.48f, rotation: Float = -90f): Path =
    Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val p = polar(cx, cy, r, rotation + i * 36f)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

/** A heart centred on (cx, cy). */
fun heartPath(cx: Float, cy: Float, w: Float): Path = Path().apply {
    val h = w * 0.9f
    moveTo(cx, cy + h * 0.45f)
    cubicTo(cx - w * 0.9f, cy - h * 0.05f, cx - w * 0.45f, cy - h * 0.75f, cx, cy - h * 0.28f)
    cubicTo(cx + w * 0.45f, cy - h * 0.75f, cx + w * 0.9f, cy - h * 0.05f, cx, cy + h * 0.45f)
    close()
}

/** Hue rotation for the rainbow items. [hue] in degrees. */
fun hsv(hue: Float, s: Float = 0.75f, v: Float = 1f, alpha: Float = 1f): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color.hsv(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f), alpha.coerceIn(0f, 1f))
}
