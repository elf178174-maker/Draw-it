package com.drawit.app.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.drawit.app.model.Economy
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/*
 * The rewards: a treasure chest for each goal and a medal per goal on the
 * streak path. Same house style as the rest: flat colour, one hard shadow,
 * a white shine stroke, chunky gold trim.
 */

private object Chest {
    val wood = Color(0xFFC8742F)
    val woodShade = Color(0xFFA1561C)
    val woodLight = Color(0xFFDE9150)
    val inner = Color(0xFF8A4515)
    val innerShade = Color(0xFF6B3210)
    val gold = Color(0xFFFFC800)
    val goldShade = Color(0xFFE5A100)
    val dark = Color(0xFF3B1C0A)
    val light = Color(0xFFFFF4B8)
    val ray = Color(0xFFFFD84D)
}

/** Below this the lid's outside faces us; past it we see the inside of the lid. */
private const val LID_FLIP = 0.6f

private fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float) =
    Path().apply { addRoundRect(RoundRect(l, t, r, b, radius, radius)) }

/**
 * A treasure chest.
 *  - [open] 0..1 swings the lid back on its hinge; a spring may overshoot past 1.
 *  - [shake] 0..1 rattles it on the spot.
 *  - [glow] 0..1 lights the rays, beams and sparkles once it is open.
 *  - [leak] 0..1 lets light escape round the shut lid, for the build-up.
 */
fun DrawScope.drawChest(open: Float, shake: Float, t: Float, glow: Float = 0f, leak: Float = 0f) {
    inArtUnits {
        val g = glow.coerceIn(0f, 1f)
        if (g > 0f) chestRays(t, g)
        ovalAt(Color.Black, 100f, 181f, 62f, 7f, alpha = 0.13f)
        val rattle = wave(t, 0.09f) * 6f * shake
        val jump = abs(wave(t, 0.18f)) * 5f * shake
        val lk = if (open < 0.05f) leak.coerceIn(0f, 1f) else 0f
        translate(0f, -jump) {
            rotate(rattle, pivot = Offset(100f, 178f)) {
                val o = open.coerceAtLeast(0f)
                val flipped = o > LID_FLIP
                if (lk > 0f) leakRays(lk, t)
                if (flipped) lidInside(o, g)
                chestBody()
                if (o > 0.02f) {
                    chestMouth(o, g)
                    if (g > 0f) chestBeams(t, g)
                    // The body's top rim sits in front of the light.
                    flat(roundRect(40f, 104f, 160f, 116f, 5f), Chest.gold, Chest.goldShade, dx = -5f, dy = -3f)
                }
                if (!flipped) lidFront(o)
                if (lk > 0f) seamGlow(lk)
                lockPlate()
            }
        }
        if (g > 0f) chestSparkles(t, g)
    }
}

private fun DrawScope.chestBody() {
    val body = roundRect(42f, 106f, 158f, 178f, 14f)
    flat(body, Chest.wood, Chest.woodShade, dx = -8f, dy = -6f)
    clipPath(body) {
        // A lighter top plank and the seam between the planks.
        drawRect(Chest.woodLight, Offset(42f, 106f), Size(108f, 8f))
        drawLine(Chest.woodShade, Offset(42f, 142f), Offset(158f, 142f), 4f)
        // Gold straps, each with its own shadow side.
        listOf(58f, 130f).forEach { x ->
            drawRect(Chest.gold, Offset(x, 106f), Size(12f, 72f))
            drawRect(Chest.goldShade, Offset(x + 8f, 106f), Size(4f, 72f))
        }
        drawRect(Chest.goldShade, Offset(42f, 168f), Size(116f, 10f))
        drawRect(Chest.gold, Offset(42f, 166f), Size(116f, 6f))
    }
    // Metal corner caps.
    flat(roundRect(38f, 158f, 60f, 180f, 7f), Chest.gold, Chest.goldShade, dx = -4f, dy = -4f)
    flat(roundRect(140f, 158f, 162f, 180f, 7f), Chest.gold, Chest.goldShade, dx = -4f, dy = -4f)
}

private fun DrawScope.lockPlate() {
    flat(roundRect(86f, 106f, 114f, 134f, 8f), Chest.gold, Chest.goldShade, dx = -4f, dy = -4f)
    drawCircle(Chest.dark, 5f, Offset(100f, 117f))
    drawRect(Chest.dark, Offset(98f, 118f), Size(4f, 9f))
    drawCircle(Color.White, 2.4f, Offset(91.5f, 111f), alpha = 0.7f)
}

/** The lid seen from the front, foreshortening and lifting as it tips back. */
private fun DrawScope.lidFront(o: Float) {
    val a = (o / LID_FLIP).coerceIn(0f, 1f) * (PI.toFloat() / 2f)
    val lift = sin(a) * 24f
    translate(0f, -lift) {
        scale(1f, max(cos(a), 0.02f), pivot = Offset(100f, 112f)) {
            val lid = svg("M38 112 L38 86 C38 66 60 56 100 56 C140 56 162 66 162 86 L162 112 Z")
            flat(lid, Chest.wood, Chest.woodShade, dx = -8f, dy = -6f)
            clipPath(lid) {
                listOf(58f, 130f).forEach { x ->
                    drawRect(Chest.gold, Offset(x, 50f), Size(12f, 62f))
                    drawRect(Chest.goldShade, Offset(x + 8f, 50f), Size(4f, 62f))
                }
            }
            // The shine along the dome.
            drawPath(
                Path().apply { moveTo(50f, 90f); cubicTo(50f, 76f, 62f, 67f, 84f, 64f) },
                Color.White, alpha = 0.4f, style = Stroke(6f, cap = StrokeCap.Round)
            )
            flat(roundRect(36f, 100f, 164f, 114f, 6f), Chest.gold, Chest.goldShade, dx = -5f, dy = -4f)
            // The hasp that drops over the lock.
            flat(roundRect(92f, 100f, 108f, 116f, 4f), Chest.gold, Chest.goldShade, dx = -3f, dy = -3f)
        }
    }
}

/** Thin beams of light fanning out from behind the shut lid. */
private fun DrawScope.leakRays(leak: Float, t: Float) {
    for (i in 0 until 9) {
        val deg = -168f + i * 19.5f + wave(t, 0.5f, i * 0.13f) * 3f
        val len = (70f + 40f * hash01(i * 5)) * leak
        val a0 = polar(100f, 112f, 30f, deg - 2f)
        val a1 = polar(100f, 112f, 30f, deg + 2f)
        val b0 = polar(100f, 112f, 30f + len, deg - 4f)
        val b1 = polar(100f, 112f, 30f + len, deg + 4f)
        drawPath(
            Path().apply { moveTo(a0.x, a0.y); lineTo(b0.x, b0.y); lineTo(b1.x, b1.y); lineTo(a1.x, a1.y); close() },
            Chest.ray, alpha = 0.7f * leak
        )
    }
}

/** The crack of light between lid and body. */
private fun DrawScope.seamGlow(leak: Float) {
    drawLine(Chest.light, Offset(42f, 115f), Offset(158f, 115f), 9f, cap = StrokeCap.Round, alpha = 0.45f * leak)
    drawLine(Color.White, Offset(46f, 115f), Offset(154f, 115f), 3.5f, cap = StrokeCap.Round, alpha = leak)
}

/** The inside of the lid, once it has swung past upright. */
private fun DrawScope.lidInside(o: Float, glow: Float) {
    val k = ((o - LID_FLIP) / (1f - LID_FLIP)).coerceIn(0f, 1.3f)
    val h = 52f * k
    val top = 100f - h
    val face = Path().apply {
        moveTo(46f, 104f)
        lineTo(40f, top + 12f * k)
        quadraticBezierTo(100f, top - 6f * k, 160f, top + 12f * k)
        lineTo(154f, 104f)
        close()
    }
    flat(face, Chest.inner, Chest.innerShade, dx = -6f, dy = 4f)
    if (glow > 0f) clipPath(face) { ovalAt(Chest.light, 100f, 104f, 50f, h * 0.5f, alpha = 0.28f * glow) }
    // The lid's front lip, now at the top.
    drawPath(
        Path().apply { moveTo(40f, top + 12f * k); quadraticBezierTo(100f, top - 6f * k, 160f, top + 12f * k) },
        Chest.gold, style = Stroke(9f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.chestMouth(o: Float, glow: Float) {
    val m = (o / LID_FLIP).coerceIn(0f, 1f)
    // Dark inside at first, then brimming with light.
    drawPath(roundRect(46f, 106f - 12f * m, 154f, 112f, 7f), androidx.compose.ui.graphics.lerp(Chest.dark, Chest.ray, glow))
    ovalAt(Color.White, 100f, 104f, 40f * glow, 4.5f * m, alpha = glow)
}

/** Beams of light fanning up out of the open chest. */
private fun DrawScope.chestBeams(t: Float, glow: Float) {
    for (i in 0 until 3) {
        val angle = (-90f + (i - 1) * 24f) * (PI.toFloat() / 180f)
        val pulse = 0.85f + 0.15f * wave(t, 1.3f, i * 0.23f)
        val len = (i == 1).let { if (it) 104f else 84f } * glow * pulse
        val base = Offset(100f + (i - 1) * 30f, 106f)
        val dir = Offset(cos(angle), sin(angle))
        val side = Offset(-dir.y, dir.x)
        val w0 = 14f
        val w1 = if (i == 1) 34f else 24f
        val tip = base + dir * len
        drawPath(
            Path().apply {
                moveTo(base.x + side.x * w0, base.y + side.y * w0)
                lineTo(tip.x + side.x * w1, tip.y + side.y * w1)
                lineTo(tip.x - side.x * w1, tip.y - side.y * w1)
                lineTo(base.x - side.x * w0, base.y - side.y * w0)
                close()
            },
            Chest.light, alpha = 0.5f * glow
        )
    }
}

/** A slow sunburst behind the open chest. */
private fun DrawScope.chestRays(t: Float, glow: Float) {
    drawCircle(Chest.light, 80f * (0.6f + 0.4f * glow), Offset(100f, 104f), alpha = 0.6f * glow)
    rotate(t * 16f, pivot = Offset(100f, 104f)) {
        for (i in 0 until 12) {
            val p1 = polar(100f, 104f, 112f, i * 30f - 6f)
            val p2 = polar(100f, 104f, 112f, i * 30f + 6f)
            drawPath(Path().apply { moveTo(100f, 104f); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }, Chest.ray, alpha = 0.42f * glow)
        }
    }
}

private fun DrawScope.chestSparkles(t: Float, glow: Float) {
    for (i in 0 until 7) {
        val p = polar(100f, 96f, 70f + hash01(i * 7) * 30f, -170f + i * 27f + hash01(i) * 10f)
        val tw = max(0f, wave(t, 0.9f + hash01(i * 3) * 0.6f, hash01(i * 11)))
        val r = (7f + 7f * hash01(i * 13)) * tw * glow
        if (r < 0.5f) continue
        drawPath(sparklePath(p.x, p.y, r), Chest.gold)
        drawPath(sparklePath(p.x, p.y, r * 0.55f), Color.White)
    }
}

// -- medals ---------------------------------------------------------------------------

class MedalStyle(val base: Color, val shade: Color, val light: Color, val ribbon: Color, val ribbonShade: Color)

private val blueRibbon = Color(0xFF1CB0F6) to Color(0xFF1690D0)
private val redRibbon = Color(0xFFFF4B4B) to Color(0xFFD93030)
private val goldRibbon = Color(0xFFFFC800) to Color(0xFFE5A100)
private val purpleRibbon = Color(0xFFA560FF) to Color(0xFF8040E0)
private val orangeRibbon = Color(0xFFFF9600) to Color(0xFFE07A00)

private fun medal(base: Long, shade: Long, light: Long, ribbon: Pair<Color, Color>) =
    MedalStyle(Color(base), Color(shade), Color(light), ribbon.first, ribbon.second)

/** One look per goal, climbing from bronze up to rainbow-ribboned diamond. */
private val medalStyles = listOf(
    medal(0xFFE38B4A, 0xFFBF6A2E, 0xFFF7BC8E, blueRibbon),   // 3: bronze
    medal(0xFFC5D0DC, 0xFF98A7B8, 0xFFF1F5F9, redRibbon),    // 7: silver
    medal(0xFFFFC800, 0xFFE5A100, 0xFFFFE58A, blueRibbon),   // 14: gold
    medal(0xFF1CB0F6, 0xFF1690D0, 0xFF9BE0FF, goldRibbon),   // 28: azure
    medal(0xFFA560FF, 0xFF8040E0, 0xFFD9B3FF, orangeRibbon), // 50: amethyst
    medal(0xFFFF4B4B, 0xFFD93030, 0xFFFFA8A8, goldRibbon),   // 100: ruby
    medal(0xFF58CC02, 0xFF46A302, 0xFFAEEB7A, purpleRibbon), // 150: emerald
    medal(0xFFFF7A1A, 0xFFD95F00, 0xFFFFC08A, redRibbon),    // 200: phoenix
    medal(0xFF7FE3FF, 0xFF45C2EA, 0xFFE8FBFF, purpleRibbon), // 365: diamond
    medal(0xFFFFC800, 0xFFE5A100, 0xFFFFF0B3, purpleRibbon), // 500
    medal(0xFFA560FF, 0xFF8040E0, 0xFFE6CCFF, goldRibbon),   // 730
    medal(0xFF3C3C3C, 0xFF262626, 0xFF6E6E6E, goldRibbon)    // 1000: obsidian
)

private val lockedMedal = MedalStyle(
    Color(0xFFE5E5E5), Color(0xFFCFCFCF), Color(0xFFF4F4F4), Color(0xFFD9D9D9), Color(0xFFC7C7C7)
)

fun medalStyle(goal: Int): MedalStyle =
    medalStyles[Economy.GOALS.indexOf(goal).coerceAtLeast(0) % medalStyles.size]

/** Where the middle of the medal's face sits, in art units. */
const val MEDAL_CENTER_Y = 90f

/**
 * A goal medal: ribbon tails, a scalloped rim and a raised star. Locked goals
 * are grey. Leave out the [emblem] to print a number on the face instead;
 * [shine] 0..1 sweeps a glint across it.
 */
fun DrawScope.drawGoalMedal(goal: Int, done: Boolean, t: Float, emblem: Boolean = true, shine: Float = 0f) {
    inArtUnits {
        val st = if (done) medalStyle(goal) else lockedMedal
        val cx = 100f
        val cy = MEDAL_CENTER_Y
        // Ribbon tails, notched at the ends.
        listOf(-1f, 1f).forEach { s ->
            drawPath(
                Path().apply {
                    moveTo(cx + s * 6f, cy + 30f)
                    lineTo(cx + s * 38f, cy + 40f)
                    lineTo(cx + s * 46f, cy + 104f)
                    lineTo(cx + s * 32f, cy + 92f)
                    lineTo(cx + s * 18f, cy + 102f)
                    close()
                },
                if (s < 0f) st.ribbon else st.ribbonShade
            )
        }
        val rim = Path().apply {
            val n = 128
            for (i in 0..n) {
                val th = i / n.toFloat() * 2f * PI.toFloat()
                val r = 72f + 3.5f * cos(18f * th)
                val x = cx + cos(th) * r
                val y = cy + sin(th) * r
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        flat(rim, st.base, st.shade, dx = -7f, dy = -8f)
        // A recessed ring, then the face with light catching its top-left edge.
        drawCircle(st.shade, 58f, Offset(cx - 3f, cy - 4f))
        val face = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - 53f, cy - 57f, cx + 47f, cy + 43f)) }
        flat(face, st.base, st.light, dx = 4f, dy = 4f)
        if (emblem) {
            val star = starPath(cx - 3f, cy - 6f, 32f, 15f)
            flat(star, st.light, st.shade, dx = -3f, dy = -4f)
        }
        clipPath(rim) {
            // A fixed highlight up the rim's top-left, plus the sweeping glint.
            drawPath(
                Path().apply {
                    val r = 64f
                    val a0 = 200f * (PI.toFloat() / 180f)
                    moveTo(cx + cos(a0) * r, cy + sin(a0) * r)
                    quadraticBezierTo(cx - 52f, cy - 50f, cx - 20f, cy - 64f)
                },
                Color.White, alpha = 0.55f, style = Stroke(6f, cap = StrokeCap.Round)
            )
            if (shine > 0f && shine < 1f) {
                val x = -20f + shine * 240f
                drawPath(
                    Path().apply { moveTo(x, 200f); lineTo(x + 30f, 200f); lineTo(x + 90f, 0f); lineTo(x + 60f, 0f); close() },
                    Color.White, alpha = 0.5f
                )
            }
        }
        if (done) {
            val tw = max(0f, wave(t, 2f))
            drawPath(sparklePath(156f, 34f, 15f * tw), Color.White, alpha = tw)
            drawPath(sparklePath(156f, 34f, 9f * tw), st.light, alpha = tw)
        }
    }
}
