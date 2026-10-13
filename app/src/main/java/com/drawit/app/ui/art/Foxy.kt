package com.drawit.app.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/*
 * Foxy, the Draw it fox. He is drawn entirely in code so he can move every
 * frame without any assets: breathing, blinking, glancing, ear twitches and a
 * tail that never quite stops. Moods (cheering, sad, surprised...) are plain
 * numbers that the UI springs between, so every change of expression blends.
 */

object FoxyPalette {
    val fur = Color(0xFFFF6A1F)
    val furLight = Color(0xFFFF9549)
    val furShade = Color(0xFFE2490C)
    val furDeep = Color(0xFFC23B06)
    val cream = Color(0xFFFFF6EA)
    val creamShade = Color(0xFFF6DEC6)
    val dark = Color(0xFF3A1E12)
    val darkLight = Color(0xFF5A3322)
    val eye = Color(0xFF24140C)
    val blush = Color(0xFFFF7E8E)
    val mouth = Color(0xFF6B1F1A)
    val tongue = Color(0xFFFF6B7A)
}

/**
 * Everything about Foxy that the UI animates. All values blend smoothly:
 * 0 is neutral, 1 is the full expression.
 */
data class FoxyPose(
    /** Height off the ground in art units. */
    val hop: Float = 0f,
    /** Positive squashes wide (landing), negative stretches tall (take-off). */
    val squash: Float = 0f,
    /** Arms up, happy eyes, open grin, fast tail. */
    val cheer: Float = 0f,
    /** Right paw waving hello. */
    val wave: Float = 0f,
    /** Droopy ears, frown, eyes down. */
    val sad: Float = 0f,
    /** Wide eyes, ears up, little "o" mouth. */
    val surprise: Float = 0f,
    /** Heavy lids. */
    val sleepy: Float = 0f,
    /** Paws together in front, holding something up (the flame, a trophy). */
    val hold: Float = 0f,
    /** Explicit gaze, -1..1. When [lookWeight] is 0 Foxy glances around by himself. */
    val lookX: Float = 0f,
    val lookY: Float = 0f,
    val lookWeight: Float = 0f,
    /** Whole-body lean in degrees. */
    val lean: Float = 0f,
    /** Whole-body spin (a happy twirl), degrees. */
    val spin: Float = 0f
)

/**
 * Foxy at [t] seconds, fitted to the draw area. [fit] shrinks him towards the
 * ground so hops and raised paws have headroom inside the same box.
 */
fun DrawScope.drawFoxy(pose: FoxyPose, t: Float, shadow: Boolean = true, fit: Float = 1f) {
    inArtUnits {
        scale(fit, fit, pivot = Offset(100f, 196f)) { foxyUnits(pose, t, shadow) }
    }
}

private const val GROUND = 186f

private fun DrawScope.foxyUnits(p: FoxyPose, t: Float, shadow: Boolean) {
    // -- the idle life ----------------------------------------------------
    val breath = wave(t, 3.4f)
    val excitement = max(p.cheer, p.surprise * 0.6f)
    val blink = blinkAmount(t) * (1f - p.cheer * 0.9f)
    val gazeX = mix(wander(t, 3.1f, 7) * 0.8f, p.lookX, p.lookWeight)
    val gazeY = mix(wander(t, 3.7f, 11) * 0.35f - 0.05f, p.lookY, p.lookWeight) + p.sad * 0.45f
    val tilt = wave(t, 5.3f) * 2.2f + gazeX * 3f + p.lean * 0.4f - p.sad * 2f

    if (shadow) {
        val lift = (p.hop / 70f).coerceIn(0f, 1f)
        val rx = 40f * (1f - lift * 0.45f) * (1f + p.squash * 0.08f)
        ovalAt(FoxyPalette.dark, 100f, GROUND + 4f, rx, 6.5f * (1f - lift * 0.4f), alpha = 0.16f * (1f - lift * 0.6f))
    }

    val sx = 1f + p.squash * 0.09f
    val sy = 1f - p.squash * 0.09f + breath * 0.008f

    translate(0f, -p.hop) {
        rotate(p.lean + p.spin, pivot = Offset(100f, 150f)) {
            scale(sx, sy, pivot = Offset(100f, GROUND)) {
                drawTail(t, excitement, p.sad)
                drawLegs(p)
                drawBody(breath)
                translate(0f, breath * 0.9f - p.sad * 2f) {
                    rotate(tilt, pivot = Offset(100f, 132f)) {
                        drawHead(p, t, blink, gazeX, gazeY)
                    }
                }
                drawArm(left = true, p = p, t = t)
                drawArm(left = false, p = p, t = t)
            }
        }
    }
}

// -- blink schedule -------------------------------------------------------

/** 0 open, 1 shut. Blinks every few seconds, sometimes twice. */
private fun blinkAmount(t: Float): Float {
    val slot = floor(t / 3.6f).toInt()
    val start = slot * 3.6f + hash01(slot * 17 + 3) * 2.2f
    val one = bump(t, start, 0.17f)
    val twice = if (hash01(slot * 29 + 5) > 0.7f) bump(t, start + 0.24f, 0.15f) else 0f
    return max(one, twice)
}

// -- tail -----------------------------------------------------------------

private const val TAIL = "M118 172 C140 182 170 176 182 152 C192 132 186 108 170 96 " +
    "C164 92 158 94 160 100 C166 116 162 134 146 144 C136 150 126 154 116 156 Z"

private fun DrawScope.drawTail(t: Float, excitement: Float, sad: Float) {
    val speed = mix(1.9f, 0.55f, excitement)
    val amp = mix(7f, 16f, excitement) * (1f - sad * 0.7f)
    val angle = wave(t, speed) * amp + 4f - sad * 18f
    // A second, slower harmonic keeps it from looking mechanical.
    val sway = wave(t, speed * 2.7f, 0.3f) * amp * 0.25f
    rotate(angle + sway, pivot = Offset(122f, 166f)) {
        val tail = svg(TAIL)
        drawPath(
            tail,
            Brush.linearGradient(
                listOf(FoxyPalette.furShade, FoxyPalette.fur, FoxyPalette.furLight),
                start = Offset(120f, 170f),
                end = Offset(176f, 100f)
            )
        )
        clipPath(tail) {
            // The cream tip, with a fluffy notch where it meets the orange.
            drawCircle(FoxyPalette.cream, radius = 22f, center = Offset(174f, 100f))
            drawPath(svg("M150 112 C158 118 168 120 182 116 L190 124 L150 124 Z"), FoxyPalette.fur)
            drawPath(svg("M152 114 L160 108 L166 116 L174 108 L180 117 L190 112 L190 126 L152 126 Z"), FoxyPalette.fur)
            // Shade on the underside.
            drawPath(svg("M118 172 C140 182 170 176 182 152 L190 170 L118 190 Z"), FoxyPalette.furDeep, alpha = 0.35f)
        }
    }
}

// -- body, legs, arms ---------------------------------------------------------

private fun DrawScope.drawLegs(p: FoxyPose) {
    val spread = p.hop.coerceIn(0f, 30f) / 30f
    listOf(-1f, 1f).forEach { side ->
        val x = 100f + side * (17f + spread * 4f)
        val kick = side * spread * 6f
        rotate(kick, pivot = Offset(x, GROUND - 14f)) {
            drawRoundRect(
                color = FoxyPalette.dark,
                topLeft = Offset(x - 10f, GROUND - 22f),
                size = Size(20f, 22f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            ovalAt(FoxyPalette.dark, x + side * 1.5f, GROUND - 3f, 13f, 7f)
            ovalAt(FoxyPalette.darkLight, x + side * 1.5f - 3f, GROUND - 6f, 5f, 2.2f, alpha = 0.7f)
        }
    }
}

private const val BODY = "M100 112 C128 112 144 134 144 156 C144 176 126 186 100 186 " +
    "C74 186 56 176 56 156 C56 134 72 112 100 112 Z"

private fun DrawScope.drawBody(breath: Float) {
    val body = svg(BODY)
    drawPath(
        body,
        Brush.radialGradient(
            listOf(FoxyPalette.furLight, FoxyPalette.fur, FoxyPalette.furShade),
            center = Offset(86f, 128f),
            radius = 78f
        )
    )
    clipPath(body) {
        // Cream belly that breathes a touch.
        val bellyR = 27f + breath * 0.6f
        ovalAt(FoxyPalette.cream, 100f, 164f, bellyR, 25f)
        ovalAt(FoxyPalette.creamShade, 100f, 176f, bellyR, 12f, alpha = 0.55f)
        // Fluffy chest tuft.
        drawPath(svg("M84 140 L92 150 L100 140 L108 150 L116 140 L112 156 L88 156 Z"), FoxyPalette.cream)
        ovalAt(FoxyPalette.furDeep, 100f, 190f, 50f, 10f, alpha = 0.25f)
    }
}

/**
 * Arms pivot at the shoulder. Angles are "outwards" from hanging straight
 * down: 0 hangs, 90 points sideways, 150 is up beside the head, negative
 * swings the paw in front of the chest.
 */
private fun DrawScope.drawArm(left: Boolean, p: FoxyPose, t: Float) {
    val side = if (left) -1f else 1f
    val shoulder = Offset(100f + side * 33f, 146f)
    var out = 16f
    out = mix(out, 152f, p.cheer)
    // Cheering arms pump, slightly out of step with each other.
    out += p.cheer * wave(t, 0.46f, if (left) 0f else 0.5f) * 9f
    if (!left && p.wave > 0f) {
        out = mix(out, 146f + wave(t, 0.62f) * 18f, p.wave)
    }
    out = mix(out, -58f, p.hold)
    out = mix(out, 6f, p.sad)
    // Clockwise rotation swings a hanging left arm outwards; mirror for the right.
    val degrees = -side * out
    val length = mix(30f, 38f, max(p.cheer, p.wave))

    rotate(degrees, pivot = shoulder) {
        drawRoundRect(
            color = FoxyPalette.fur,
            topLeft = Offset(shoulder.x - 9f, shoulder.y - 6f),
            size = Size(18f, length + 6f),
            cornerRadius = CornerRadius(9f, 9f)
        )
        drawRoundRect(
            color = FoxyPalette.furShade,
            topLeft = Offset(shoulder.x + side * 3f - 3f, shoulder.y),
            size = Size(6f, length - 6f),
            cornerRadius = CornerRadius(3f, 3f),
            alpha = 0.3f
        )
        // Dark sock paw.
        drawCircle(FoxyPalette.dark, radius = 10.5f, center = Offset(shoulder.x, shoulder.y + length))
        drawCircle(FoxyPalette.darkLight, radius = 3.4f, center = Offset(shoulder.x - 3f, shoulder.y + length - 3.5f), alpha = 0.7f)
    }
}

// -- head -----------------------------------------------------------------

private const val HEAD = "M100 38 C134 38 156 58 157 88 C158 102 162 114 172 124 " +
    "C160 128 150 132 140 136 C128 141 114 143 100 143 C86 143 72 141 60 136 " +
    "C50 132 40 128 28 124 C38 114 42 102 43 88 C44 58 66 38 100 38 Z"

private const val MASK = "M28 124 C40 114 48 102 62 98 C76 94 90 96 100 106 " +
    "C110 96 124 94 138 98 C152 102 160 114 172 124 C160 128 150 132 140 136 " +
    "C128 141 114 143 100 143 C86 143 72 141 60 136 C50 132 40 128 28 124 Z"

private fun DrawScope.drawHead(p: FoxyPose, t: Float, blink: Float, gazeX: Float, gazeY: Float) {
    drawEars(p, t)

    val head = svg(HEAD)
    drawPath(
        head,
        Brush.radialGradient(
            listOf(FoxyPalette.furLight, FoxyPalette.fur, FoxyPalette.furShade),
            center = Offset(82f, 62f),
            radius = 112f
        )
    )
    clipPath(head) {
        // Forehead shine.
        ovalAt(Color.White, 78f, 56f, 20f, 10f, alpha = 0.16f)
        // Cream face mask with a soft shadow line above it.
        val mask = svg(MASK)
        translate(0f, -2f) { drawPath(mask, FoxyPalette.furDeep, alpha = 0.18f) }
        drawPath(mask, FoxyPalette.cream)
        clipPath(mask) {
            ovalAt(FoxyPalette.creamShade, 100f, 146f, 60f, 12f, alpha = 0.7f)
        }
        // Darker rim along the jaw.
        ovalAt(FoxyPalette.furDeep, 100f, 150f, 80f, 10f, alpha = 0.15f)
    }

    // Cheek blush grows when happy.
    val blushAlpha = (0.42f + p.cheer * 0.35f - p.sad * 0.25f).coerceIn(0f, 1f)
    ovalAt(FoxyPalette.blush, 66f, 112f, 9f, 5.5f, alpha = blushAlpha)
    ovalAt(FoxyPalette.blush, 134f, 112f, 9f, 5.5f, alpha = blushAlpha)

    drawEyes(p, blink, gazeX, gazeY)
    drawBrows(p)
    drawNose(gazeX)
    drawMouth(p, t)
}

private const val EAR = "M58 70 C50 50 46 30 50 12 C52 8 56 8 59 10 C72 22 84 36 90 50 Z"
private const val EAR_INNER = "M62 60 C57 46 55 34 57 22 C66 32 74 42 80 52 Z"

private fun DrawScope.drawEars(p: FoxyPose, t: Float) {
    // Each ear twitches now and then, on its own clock.
    fun twitch(seed: Int): Float {
        val slot = floor(t / 5.5f).toInt()
        val at = slot * 5.5f + hash01(slot * 41 + seed) * 4f
        return if (hash01(slot * 7 + seed) > 0.4f) bump(t, at, 0.22f) else 0f
    }
    listOf(false, true).forEach { mirrored ->
        // Positive tips the ear inwards and upright; negative flops it outwards.
        val angle = p.surprise * 6f + p.cheer * 4f - p.sad * 28f - twitch(if (mirrored) 2 else 1) * 10f
        val ear: DrawScope.() -> Unit = {
            rotate(angle, pivot = Offset(70f, 58f)) {
                val shape = svg(EAR)
                drawPath(
                    shape,
                    Brush.linearGradient(
                        listOf(FoxyPalette.furLight, FoxyPalette.fur, FoxyPalette.furShade),
                        start = Offset(52f, 20f),
                        end = Offset(86f, 64f)
                    )
                )
                clipPath(shape) {
                    drawPath(svg("M40 0 L70 0 L70 30 C62 26 52 26 40 32 Z"), FoxyPalette.dark)
                }
                val inner = svg(EAR_INNER)
                drawPath(inner, FoxyPalette.cream, alpha = 0.95f)
                clipPath(inner) {
                    drawPath(svg("M50 10 L70 10 L72 40 C66 36 58 36 50 40 Z"), FoxyPalette.blush, alpha = 0.35f)
                }
            }
        }
        if (mirrored) scale(-1f, 1f, pivot = Offset(100f, 0f)) { ear() } else ear()
    }
}

private fun DrawScope.drawEyes(p: FoxyPose, blink: Float, gazeX: Float, gazeY: Float) {
    val happy = smoothstep(0.25f, 0.85f, p.cheer)
    val open = (1f - blink) * (1f - p.sleepy * 0.55f) * (1f - happy)
    val size = 1f + p.surprise * 0.18f - p.sad * 0.04f
    val dx = gazeX * 4.2f
    val dy = gazeY * 3.4f
    listOf(76f, 124f).forEach { cx ->
        val cy = 92f
        if (open > 0.02f) {
            val rx = 10.5f * size
            val ry = 13.5f * size * open
            ovalAt(FoxyPalette.eye, cx + dx, cy + dy, rx, ry)
            if (open > 0.35f) {
                // Big catchlight, small catchlight: they fade with the blink.
                val a = smoothstep(0.35f, 0.7f, open)
                drawCircle(Color.White, radius = 4.6f * size, center = Offset(cx + dx + 3.6f, cy + dy - 5f * open), alpha = a)
                drawCircle(Color.White, radius = 2f * size, center = Offset(cx + dx - 3.8f, cy + dy + 5f * open), alpha = a * 0.85f)
            }
            // Heavy lids when sleepy.
            if (p.sleepy > 0.05f) {
                drawArc(
                    color = FoxyPalette.fur,
                    startAngle = 180f, sweepAngle = 180f, useCenter = true,
                    topLeft = Offset(cx + dx - rx - 1f, cy + dy - ry - 1f),
                    size = Size(rx * 2 + 2f, ry * 2 * p.sleepy * 0.9f + 2f)
                )
            }
        }
        if (open <= 0.25f && happy < 0.5f) {
            // Closed in a blink: a soft curved line.
            val path = Path().apply {
                moveTo(cx + dx - 9.5f, cy + dy + 1f)
                quadraticBezierTo(cx + dx, cy + dy + 6f, cx + dx + 9.5f, cy + dy + 1f)
            }
            drawPath(path, FoxyPalette.eye, style = Stroke(width = 4.2f, cap = StrokeCap.Round))
        }
        if (happy > 0.01f) {
            // Happy "^ ^" arcs that rise in as the eyes squeeze shut.
            val path = Path().apply {
                moveTo(cx - 10f, cy + 4f)
                quadraticBezierTo(cx, cy - 10f * happy, cx + 10f, cy + 4f)
            }
            drawPath(path, FoxyPalette.eye, alpha = happy, style = Stroke(width = 5f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawBrows(p: FoxyPose) {
    val sad = p.sad
    val surprise = p.surprise
    if (sad < 0.05f && surprise < 0.05f) return
    listOf(-1f, 1f).forEach { side ->
        val cx = 100f + side * 24f
        val lift = surprise * -6f
        // Sad brows slope up towards the middle.
        val inner = Offset(cx - side * 8f, 72f + lift - sad * 4f)
        val outer = Offset(cx + side * 8f, 74f + lift + sad * 3f)
        drawLine(
            FoxyPalette.dark,
            start = inner,
            end = outer,
            strokeWidth = 3.4f,
            cap = StrokeCap.Round,
            alpha = max(sad, surprise).coerceIn(0f, 1f)
        )
    }
}

private fun DrawScope.drawNose(gazeX: Float) {
    val x = 100f + gazeX * 1.6f
    val nose = Path().apply {
        moveTo(x - 8f, 104f)
        cubicTo(x - 8f, 100f, x + 8f, 100f, x + 8f, 104f)
        cubicTo(x + 8f, 108f, x + 3f, 112f, x, 112f)
        cubicTo(x - 3f, 112f, x - 8f, 108f, x - 8f, 104f)
        close()
    }
    drawPath(nose, FoxyPalette.eye)
    ovalAt(Color.White, x - 2.6f, 103.4f, 2.8f, 1.6f, alpha = 0.55f)
}

private fun DrawScope.drawMouth(p: FoxyPose, t: Float) {
    val open = max(smoothstep(0.1f, 0.9f, p.cheer), p.surprise * 0.8f)
    val smile = p.cheer - p.sad
    val cy = 117f
    if (open < 0.08f) {
        // The little fox "w".
        val curl = 3.4f * (1f + smile)
        val path = Path().apply {
            moveTo(89f, cy - 1f + p.sad * 3f)
            quadraticBezierTo(94.5f, cy + curl, 100f, cy - 0.5f)
            quadraticBezierTo(105.5f, cy + curl, 111f, cy - 1f + p.sad * 3f)
        }
        drawPath(path, FoxyPalette.eye, style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        return
    }
    // Open mouth: a grin when cheering, an "o" when surprised.
    val round = (p.surprise * 0.8f).coerceIn(0f, 1f) * (1f - p.cheer * 0.6f)
    val w = mix(13f, 6.5f, round) * open.coerceAtLeast(0.4f)
    val h = mix(10f, 8f, round) * open + 2f
    val chatter = p.cheer * abs(wave(t, 0.36f)) * 1.8f
    val mouth = Path().apply {
        moveTo(100f - w, cy - 1f)
        cubicTo(100f - w * 0.6f, cy - 2f, 100f + w * 0.6f, cy - 2f, 100f + w, cy - 1f)
        cubicTo(100f + w, cy + h + chatter, 100f - w, cy + h + chatter, 100f - w, cy - 1f)
        close()
    }
    drawPath(mouth, FoxyPalette.mouth)
    clipPath(mouth) {
        ovalAt(FoxyPalette.tongue, 100f, cy + h + chatter, w * 0.75f, h * 0.55f)
    }
}

/** Where Foxy's paws meet when holding something up, in art units. */
val FoxyHoldPoint = Offset(100f, 150f)
