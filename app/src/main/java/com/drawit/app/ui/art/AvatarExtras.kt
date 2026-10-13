package com.drawit.app.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.abs

// -- face pieces --------------------------------------------------------------

fun DrawScope.drawFaceItem(id: String?, t: Float) {
    val y = Rig.EYE_Y
    when (id) {
        "face_glasses" -> {
            listOf(-1f, 1f).forEach { s -> drawCircle(Color(0xFF3A2A20), 11f, Offset(100f + s * Rig.EYE_DX, y), style = Stroke(3f)) }
            listOf(-1f, 1f).forEach { s -> drawCircle(Color.White, 10f, Offset(100f + s * Rig.EYE_DX, y), alpha = 0.12f) }
            drawArc(Color(0xFF3A2A20), 200f, 140f, false, Offset(95f, y - 4f), Size(10f, 8f), style = Stroke(2.4f))
            drawLine(Color.White, Offset(100f - 20f, y - 4f), Offset(100f - 16f, y - 7f), 1.6f, StrokeCap.Round, alpha = 0.8f)
        }
        "face_freckles" -> listOf(-1f, 1f).forEach { s ->
            listOf(Offset(0f, 0f), Offset(5f, -2f), Offset(3f, 4f), Offset(-4f, 3f)).forEach { o ->
                drawCircle(Color(0xFFB0643A), 1.3f, Offset(100f + s * 25f + o.x * s, 90f + o.y), alpha = 0.75f)
            }
        }
        "face_smudge" -> {
            drawPath(svg("M112 92 C118 88 128 90 130 94 C128 98 120 99 113 97 Z"), Color(0xFF2FA8FF), alpha = 0.85f)
            drawCircle(Color(0xFFFF4F6D), 2.2f, Offset(124f, 100f), alpha = 0.85f)
        }
        "face_shades" -> {
            listOf(-1f, 1f).forEach { s ->
                val lens = Path().apply {
                    val cx = 100f + s * Rig.EYE_DX
                    moveTo(cx - 12f, y - 7f); lineTo(cx + 12f, y - 7f)
                    quadraticBezierTo(cx + 12f, y + 9f, cx, y + 9f)
                    quadraticBezierTo(cx - 12f, y + 9f, cx - 12f, y - 7f); close()
                }
                drawPath(lens, Color(0xFF26262F))
                clipPath(lens) {
                    drawLine(Color.White, Offset(100f + s * Rig.EYE_DX - 10f, y + 4f), Offset(100f + s * Rig.EYE_DX + 4f, y - 8f), 3f, alpha = 0.22f)
                }
            }
            drawLine(Color(0xFF101018), Offset(88f, y - 6f), Offset(112f, y - 6f), 3f)
            drawLine(Color(0xFF101018), Offset(57f, y - 6f), Offset(76f, y - 6f), 2.4f)
            drawLine(Color(0xFF101018), Offset(124f, y - 6f), Offset(143f, y - 6f), 2.4f)
        }
        "face_hearts" -> {
            val pulse = 1f + abs(wave(t, 1.2f)) * 0.06f
            listOf(-1f, 1f).forEach { s ->
                val cx = 100f + s * Rig.EYE_DX
                drawPath(heartPath(cx, y + 1f, 15f * pulse), Color(0xFFE0185A))
                drawPath(heartPath(cx, y + 1f, 12f * pulse), Color(0xFFFF4B8B))
                drawCircle(Color.White, 2.2f, Offset(cx - 4f, y - 3f), alpha = 0.7f)
            }
            drawLine(Color(0xFFE0185A), Offset(93f, y - 3f), Offset(107f, y - 3f), 2.4f)
        }
        "face_monocle" -> {
            val cx = 100f + Rig.EYE_DX
            drawCircle(Color(0xFFFFC800), 11.5f, Offset(cx, y), style = Stroke(3.2f))
            drawCircle(Color(0xFFBFE6FF), 10f, Offset(cx, y), alpha = 0.18f)
            drawArc(Color.White, 200f, 60f, false, Offset(cx - 8f, y - 8f), Size(16f, 16f), style = Stroke(1.6f, cap = StrokeCap.Round), alpha = 0.8f)
            val chain = Path().apply { moveTo(cx + 9f, y + 7f); cubicTo(cx + 14f, y + 22f, cx + 6f, y + 34f, cx + 12f, y + 48f) }
            drawPath(chain, Color(0xFFD48A00), style = Stroke(1.6f, cap = StrokeCap.Round))
        }
        "face_mask" -> {
            val mask = svg("M60 72 C70 64 88 66 100 74 C112 66 130 64 140 72 C142 84 132 92 120 90 C110 88 104 84 100 84 C96 84 90 88 80 90 C68 92 58 84 60 72 Z")
            flat(mask, Color(0xFFA560FF), Color(0xFF7C3FD6), dx = -5f, dy = -3f)
            clipPath(mask) { drawStarfield(t, 60f, 64f, 80f, 28f, seed = 33, count = 6, color = Color(0xFFFFE27A)) }
            listOf(-1f, 1f).forEach { s -> ovalAt(Color(0xFF2A1A3A), 100f + s * Rig.EYE_DX, y - 1f, 8f, 6f) }
            listOf(-1f, 1f).forEach { s -> drawCircle(Color.White, 1.8f, Offset(100f + s * Rig.EYE_DX + 2f, y - 3f)) }
            drawPath(svg("M60 72 C56 66 52 62 46 62"), Color(0xFFFFC53A), style = Stroke(2f, cap = StrokeCap.Round))
            drawPath(svg("M140 72 C148 60 158 58 164 48"), Color(0xFFFFC53A), style = Stroke(2f, cap = StrokeCap.Round))
            listOf(0f, 1f, 2f).forEach { k ->
                rotate(-30f + k * 18f + wave(t, 2f, k * 0.2f) * 6f, pivot = Offset(140f, 66f)) {
                    ovalAt(listOf(Color(0xFFFF4F6D), Color(0xFF2FA8FF), Color(0xFFFFD23F))[k.toInt()], 156f, 66f, 12f, 3f)
                }
            }
        }
        "face_visor" -> {
            val visor = svg("M56 70 C70 64 130 64 144 70 L142 88 C128 94 72 94 58 88 Z")
            drawPath(visor, Color(0xFF1CB0F6))
            clipPath(visor) { drawRect(Color(0xFF1287C7), Offset(56f, 82f), Size(88f, 14f)) }
            clipPath(visor) {
                val scan = 60f + ((t * 0.8f) % 1f) * 90f
                drawRect(Color.White, Offset(scan - 4f, 60f), Size(8f, 40f), alpha = 0.35f)
                for (i in 0..5) drawLine(Color.White, Offset(56f, 70f + i * 4f), Offset(144f, 70f + i * 4f), 0.6f, alpha = 0.18f)
            }
            drawPath(visor, Color(0xFF1B1B2A), style = Stroke(2.4f))
            listOf(-1f, 1f).forEach { s -> drawCircle(Color.White, 2.4f, Offset(100f + s * Rig.EYE_DX, y), alpha = 0.6f + 0.4f * abs(wave(t, 1.5f))) }
        }
        else -> Unit
    }
}


// -- held items -----------------------------------------------------------------

fun DrawScope.drawHeld(id: String?, t: Float) {
    val h = Rig.holdR
    when (id) {
        "held_brush" -> rotate(16f, pivot = h) {
            drawRoundRect(Color(0xFFB07A44), Offset(h.x - 3f, h.y - 38f), Size(6f, 52f), CornerRadius(3f, 3f))
            drawRect(Color(0xFFC9CED6), Offset(h.x - 3.5f, h.y - 44f), Size(7f, 8f))
            drawPath(Path().apply {
                moveTo(h.x - 4f, h.y - 44f); quadraticBezierTo(h.x - 5f, h.y - 56f, h.x, h.y - 62f)
                quadraticBezierTo(h.x + 5f, h.y - 56f, h.x + 4f, h.y - 44f); close()
            }, Color(0xFF2E2A28))
            drawPath(Path().apply {
                moveTo(h.x - 2.5f, h.y - 54f); quadraticBezierTo(h.x, h.y - 61f, h.x + 2.5f, h.y - 54f); close()
            }, Color(0xFFFF4F6D))
        }
        "held_pencil" -> rotate(14f, pivot = h) {
            drawRect(Color(0xFFFFC21A), Offset(h.x - 5f, h.y - 44f), Size(10f, 58f))
            drawRect(Color(0xFFFFD95C), Offset(h.x - 5f, h.y - 44f), Size(3.5f, 58f))
            drawRect(Color(0xFFC9CED6), Offset(h.x - 5f, h.y + 12f), Size(10f, 6f))
            drawRoundRect(Color(0xFFFF8FA3), Offset(h.x - 5f, h.y + 17f), Size(10f, 7f), CornerRadius(2f, 2f))
            drawPath(Path().apply { moveTo(h.x - 5f, h.y - 44f); lineTo(h.x, h.y - 60f); lineTo(h.x + 5f, h.y - 44f); close() }, Color(0xFFF2D3A8))
            drawPath(Path().apply { moveTo(h.x - 1.8f, h.y - 54f); lineTo(h.x, h.y - 60f); lineTo(h.x + 1.8f, h.y - 54f); close() }, Color(0xFF3A2A20))
        }
        "held_mug" -> {
            drawRoundRect(Color(0xFFFF6A1F), Offset(h.x - 11f, h.y - 22f), Size(22f, 24f), CornerRadius(4f, 4f))
            drawArc(Color(0xFFFF6A1F), -90f, 180f, false, Offset(h.x + 6f, h.y - 18f), Size(12f, 14f), style = Stroke(4f))
            drawPath(heartPath(h.x, h.y - 10f, 7f), Color.White)
            for (i in 0..2) {
                val rise = ((t * 0.5f + i / 3f) % 1f)
                val x = h.x - 6f + i * 6f + wave(t, 1.4f, i * 0.3f) * 2f
                drawCircle(Color.White, 2.6f * (1f - rise * 0.6f), Offset(x, h.y - 26f - rise * 20f), alpha = 0.55f * (1f - rise))
            }
        }
        "held_palette" -> rotate(8f, pivot = h) {
            val palette = svg("M${h.x - 26} ${h.y - 6} C${h.x - 30} ${h.y - 26} ${h.x + 4} ${h.y - 36} ${h.x + 20} ${h.y - 24} C${h.x + 30} ${h.y - 16} ${h.x + 22} ${h.y - 6} ${h.x + 14} ${h.y - 8} C${h.x + 6} ${h.y - 10} ${h.x + 4} ${h.y + 2} ${h.x - 6} ${h.y + 4} C${h.x - 16} ${h.y + 6} ${h.x - 24} ${h.y + 2} ${h.x - 26} ${h.y - 6} Z")
            flat(palette, Color(0xFFE5B577), Color(0xFFC9955A), dx = -3f, dy = -3f)
            listOf(Color(0xFFFF4F6D), Color(0xFFFFC21A), Color(0xFF2FA8FF), Color(0xFF4CD964), Color(0xFFA35CFF)).forEachIndexed { i, c ->
                val p = h + listOf(Offset(-18f, -8f), Offset(-14f, -20f), Offset(-2f, -26f), Offset(10f, -22f), Offset(-6f, -10f))[i]
                drawCircle(c, 4.2f, p)
                drawCircle(Color.White, 1.2f, p + Offset(-1.2f, -1.2f), alpha = 0.6f)
            }
        }
        "held_balloon" -> {
            val bob = wave(t, 2.2f) * 4f
            val sway = wave(t, 3.1f) * 3f
            val top = Offset(h.x + 8f + sway, h.y - 72f + bob)
            drawPath(Path().apply { moveTo(h.x, h.y - 4f); cubicTo(h.x + 6f, h.y - 24f, top.x - 6f, top.y + 40f, top.x, top.y + 24f) },
                Color(0xFF8A8A9A), style = Stroke(1.3f))
            ovalAt(Color(0xFFFF3E6C), top.x, top.y, 18f, 22f)
            drawPath(Path().apply { moveTo(top.x - 3f, top.y + 22f); lineTo(top.x + 3f, top.y + 22f); lineTo(top.x, top.y + 26f); close() }, Color(0xFFD9204E))
            ovalAt(Color.White, top.x - 7f, top.y - 8f, 4f, 7f, alpha = 0.5f)
        }
        "held_wand" -> rotate(18f, pivot = h) {
            drawRoundRect(Color(0xFF2B2230), Offset(h.x - 2.5f, h.y - 40f), Size(5f, 50f), CornerRadius(2.5f, 2.5f))
            drawRect(Color.White, Offset(h.x - 2.5f, h.y - 40f), Size(5f, 6f))
            val spin = t * 60f
            drawPath(starPath(h.x, h.y - 50f, 12f, rotation = spin), Color(0xFFFFD23F))
            drawPath(starPath(h.x, h.y - 50f, 6f, rotation = spin), Color(0xFFFFF4C2))
            for (i in 0..5) {
                val life = ((t * 0.9f + i / 6f) % 1f)
                val p = Offset(h.x + wave(t, 1.3f, i * 0.4f) * 14f, h.y - 50f + life * 34f)
                drawPath(sparklePath(p.x, p.y, 4f * (1f - life)), hsv(i * 60f + t * 120f, 0.5f), alpha = 1f - life)
            }
        }
        "held_sparkler" -> rotate(14f, pivot = h) {
            drawLine(Color(0xFF8A8A9A), h + Offset(0f, 8f), h + Offset(0f, -40f), 2.2f, StrokeCap.Round)
            val tip = h + Offset(0f, -42f)
            drawPath(starPath(tip.x, tip.y, 13f, inner = 5f, rotation = t * 200f), Color(0xFFFFC800))
            drawCircle(Color.White, 4f, tip)
            for (i in 0..13) {
                val a = hash01(i * 7 + (t * 12f).toInt()) * 360f
                val len = 6f + hash01(i * 13 + (t * 12f).toInt()) * 14f
                val e = polar(tip.x, tip.y, len, a)
                drawLine(Color(0xFFFFE27A), tip, e, 1.2f, StrokeCap.Round, alpha = 0.9f)
                drawCircle(Color.White, 1f, e)
            }
        }
        "held_glowbrush" -> rotate(16f, pivot = h) {
            val hue = t * 80f
            drawRoundRect(Color(0xFF1B1B2A), Offset(h.x - 3f, h.y - 38f), Size(6f, 52f), CornerRadius(3f, 3f))
            val tip = h + Offset(0f, -50f)
            drawPath(starPath(tip.x, tip.y, 16f, inner = 7f, rotation = t * 90f), hsv(hue, 0.55f))
            drawPath(Path().apply {
                moveTo(h.x - 4f, h.y - 40f); quadraticBezierTo(h.x - 5f, h.y - 52f, h.x, h.y - 60f)
                quadraticBezierTo(h.x + 5f, h.y - 52f, h.x + 4f, h.y - 40f); close()
            }, hsv(hue, 0.5f))
            // A ribbon of light trailing behind.
            val ribbon = Path().apply {
                moveTo(tip.x, tip.y)
                for (k in 1..12) {
                    val p = Offset(tip.x - k * 4f, tip.y + k * 2.2f + wave(t - k * 0.05f, 0.8f) * 6f)
                    lineTo(p.x, p.y)
                }
            }
            drawPath(ribbon, hsv(hue + 120f, 0.7f), alpha = 0.7f, style = Stroke(3f, cap = StrokeCap.Round))
        }
        "held_trophy" -> {
            drawRoundRect(Color(0xFF6B4A2E), Offset(h.x - 10f, h.y - 2f), Size(20f, 8f), CornerRadius(2f, 2f))
            drawRect(Color(0xFFD48A00), Offset(h.x - 3f, h.y - 12f), Size(6f, 11f))
            val cup = svg("M${h.x - 14} ${h.y - 40} L${h.x + 14} ${h.y - 40} C${h.x + 14} ${h.y - 22} ${h.x + 8} ${h.y - 12} ${h.x} ${h.y - 12} C${h.x - 8} ${h.y - 12} ${h.x - 14} ${h.y - 22} ${h.x - 14} ${h.y - 40} Z")
            listOf(-1f, 1f).forEach { s ->
                drawArc(Color(0xFFD48A00), if (s < 0) 90f else -90f, 180f, false, Offset(h.x + s * 14f - 6f, h.y - 38f), Size(12f, 14f), style = Stroke(3.2f))
            }
            flat(cup, Color(0xFFFFC800), Color(0xFFE09A00), dx = -4f, dy = -2f)
            drawPath(starPath(h.x, h.y - 28f, 5.5f), Color.White, alpha = 0.85f)
            val sweep = ((t / 2.6f) % 1f) * 40f - 10f
            clipPath(cup) { drawRect(Color.White, Offset(h.x - 20f + sweep, h.y - 42f), Size(5f, 32f), alpha = 0.4f) }
        }
        else -> Unit
    }
}

// -- back pieces ----------------------------------------------------------------

fun DrawScope.drawBack(id: String?, t: Float) {
    when (id) {
        "back_backpack" -> {
            drawRoundRect(Color(0xFFFFB020), Offset(64f, 118f), Size(72f, 56f), CornerRadius(16f, 16f))
            drawRoundRect(Color(0xFFE08F00), Offset(60f, 140f), Size(10f, 26f), CornerRadius(5f, 5f))
            drawRoundRect(Color(0xFFE08F00), Offset(130f, 140f), Size(10f, 26f), CornerRadius(5f, 5f))
        }
        "back_cape" -> {
            val billow = wave(t, 2.4f) * 6f
            val cape = Path().apply {
                moveTo(78f, 118f)
                cubicTo(64f, 140f, 50f - billow, 168f, 46f - billow, 188f)
                quadraticBezierTo(70f, 182f + billow * 0.5f, 100f, 190f)
                quadraticBezierTo(130f, 182f - billow * 0.5f, 154f + billow, 188f)
                cubicTo(150f + billow, 168f, 136f, 140f, 122f, 118f)
                close()
            }
            flat(cape, Color(0xFFFF4B4B), Color(0xFFC72626), dx = -8f, dy = 0f)
        }
        "back_batwings" -> wings(t, flap = 2.2f) { s ->
            val w = Path().apply {
                moveTo(100f + s * 18f, 128f)
                cubicTo(100f + s * 50f, 96f, 100f + s * 84f, 98f, 100f + s * 94f, 116f)
                quadraticBezierTo(100f + s * 82f, 120f, 100f + s * 80f, 134f)
                quadraticBezierTo(100f + s * 68f, 128f, 100f + s * 62f, 142f)
                quadraticBezierTo(100f + s * 48f, 134f, 100f + s * 40f, 150f)
                quadraticBezierTo(100f + s * 32f, 138f, 100f + s * 18f, 146f)
                close()
            }
            flat(w, Color(0xFF5B3C8C), Color(0xFF3F2766), dx = s * 4f, dy = -4f)
            listOf(0.45f, 0.7f).forEach { k ->
                drawLine(Color(0xFF1B0F2E), Offset(100f + s * 20f, 130f), Offset(100f + s * 94f * k, 104f + k * 10f), 1.4f, alpha = 0.6f)
            }
        }
        "back_angel" -> wings(t, flap = 2.8f) { side -> angelWing(t, side) }
        "back_butterfly" -> wings(t, flap = 1.6f) { s ->
            val hue = t * 30f
            val upper = Path().apply {
                moveTo(100f + s * 10f, 130f)
                cubicTo(100f + s * 40f, 70f, 100f + s * 96f, 74f, 100f + s * 88f, 116f)
                cubicTo(100f + s * 84f, 132f, 100f + s * 50f, 138f, 100f + s * 10f, 132f)
                close()
            }
            val lower = Path().apply {
                moveTo(100f + s * 10f, 134f)
                cubicTo(100f + s * 50f, 136f, 100f + s * 78f, 150f, 100f + s * 66f, 172f)
                cubicTo(100f + s * 52f, 184f, 100f + s * 24f, 160f, 100f + s * 10f, 136f)
                close()
            }
            drawPath(upper, hsv(hue + 200f, 0.7f))
            clipPath(upper) { drawCircle(hsv(hue + 270f, 0.75f), 26f, Offset(100f + s * 74f, 96f)) }
            drawPath(lower, hsv(hue + 330f, 0.65f))
            clipPath(lower) { drawCircle(hsv(hue + 20f, 0.7f), 14f, Offset(100f + s * 60f, 166f)) }
            drawCircle(Color.White, 5f, Offset(100f + s * 66f, 104f))
            drawCircle(Color.White, 3f, Offset(100f + s * 48f, 160f))
        }
        "back_phoenix" -> wings(t, flap = 1.9f) { side -> phoenixWing(t, side) }
        else -> Unit
    }
}

/**
 * Mirrored wings that flap gently around the shoulder blades. [wing] draws
 * the right-hand wing; the left one is the same drawing mirrored.
 */
private inline fun DrawScope.wings(t: Float, flap: Float, crossinline wing: DrawScope.(side: Float) -> Unit) {
    val beat = wave(t, flap)
    listOf(-1f, 1f).forEach { s ->
        rotate(s * beat * 7f, pivot = Offset(100f + s * 14f, 132f)) {
            scale(1f + beat * 0.03f, 1f, pivot = Offset(100f, 132f)) { wing(s) }
        }
    }
}

private val angelWingPath = "M116 132 C122 104 144 80 174 66 C186 60 194 68 188 80 " +
    "C184 90 180 98 176 104 Q182 112 170 116 Q176 126 160 128 Q164 138 148 138 Q148 148 132 146 Q128 152 116 146 Z"

/** A soft feathered silhouette, drawn as the right wing and mirrored for the left. */
private fun DrawScope.angelWing(t: Float, side: Float) {
    run {
        val draw: DrawScope.() -> Unit = {
            val wing = svg(angelWingPath)
            flat(wing, Color.White, Color(0xFFD7E3F4), dx = 5f, dy = -5f)
            clipPath(wing) {
                // Feather rows.
                listOf(Offset(150f, 92f) to 34f, Offset(140f, 112f) to 30f, Offset(132f, 130f) to 24f).forEach { (c, r) ->
                    drawArc(Color(0xFFB9CCE8), 200f, 120f, false, Offset(c.x - r, c.y - r * 0.5f), Size(r * 2f, r), style = Stroke(1.6f, cap = StrokeCap.Round))
                }
            }
            drawPath(wing, Color(0xFFB9CCE8), style = Stroke(1.4f))
            drawCircle(Color.White, 3f, Offset(176f, 74f), alpha = 0.6f + 0.4f * wave(t, 1.8f))
        }
        if (side < 0f) scale(-1f, 1f, pivot = Offset(100f, 0f)) { draw() } else draw()
    }
}

private fun DrawScope.phoenixWing(t: Float, side: Float) {
    run {
        val draw: DrawScope.() -> Unit = {
            val wing = svg(angelWingPath)
            // Flame plumes licking off the leading edge.
            for (i in 0..4) {
                val p = Offset(130f + i * 12f, 104f - i * 9f)
                rotate(40f + i * 6f, pivot = p) { drawFlameTongue(p.x, p.y, 7f, 22f + i * 3f, t, i + 40) }
            }
            flat(wing, Color(0xFFFF9600), Color(0xFFE5600A), dx = 5f, dy = -5f)
            clipPath(wing) {
                listOf(Offset(150f, 92f) to 34f, Offset(140f, 112f) to 30f, Offset(132f, 130f) to 24f).forEach { (c, r) ->
                    drawArc(Color(0xFFFFF4C2), 200f, 120f, false, Offset(c.x - r, c.y - r * 0.5f), Size(r * 2f, r), style = Stroke(2f, cap = StrokeCap.Round), alpha = 0.7f)
                }
            }
            for (i in 0..4) {
                val life = ((t * 0.7f + i / 5f) % 1f)
                drawCircle(Color(0xFFFFC53A), 2.2f * (1f - life), Offset(150f + i * 8f, 110f - life * 50f), alpha = 1f - life)
            }
        }
        if (side < 0f) scale(-1f, 1f, pivot = Offset(100f, 0f)) { draw() } else draw()
    }
}

// -- auras ------------------------------------------------------------------------

fun DrawScope.drawAura(id: String?, t: Float, front: Boolean) {
    when (id) {
        "aura_sparkles" -> if (front) {
            for (i in 0..8) {
                val life = ((t * 0.45f + hash01(i * 3)) % 1f)
                val p = Offset(30f + hash01(i * 17) * 140f, 40f + hash01(i * 23) * 130f)
                val s = bump(life, 0f, 1f)
                drawPath(sparklePath(p.x, p.y, 7f * s), Color(0xFFFFE27A), alpha = s)
                drawPath(sparklePath(p.x, p.y, 3f * s), Color.White, alpha = s)
            }
        }
        "aura_hearts" -> if (front) {
            for (i in 0..6) {
                val life = ((t * 0.3f + i / 7f) % 1f)
                val x = 36f + hash01(i * 11) * 128f + wave(t, 2f, i * 0.3f) * 6f
                val y = 180f - life * 150f
                drawPath(heartPath(x, y, 9f + hash01(i) * 5f), Color(0xFFFF5E8A), alpha = bump(life, 0f, 1f) * 0.85f)
            }
        }
        "aura_notes" -> if (front) {
            for (i in 0..4) {
                val life = ((t * 0.35f + i / 5f) % 1f)
                val side = if (i % 2 == 0) -1f else 1f
                val x = 100f + side * (60f + life * 20f) + wave(t, 1.6f, i * 0.2f) * 6f
                val y = 120f - life * 90f
                val a = bump(life, 0f, 1f)
                val c = hsv(i * 70f + 200f, 0.6f)
                drawCircle(c, 4.5f, Offset(x, y), alpha = a)
                drawLine(c, Offset(x + 4f, y), Offset(x + 4f, y - 16f), 2f, alpha = a)
                drawLine(c, Offset(x + 4f, y - 16f), Offset(x + 11f, y - 12f), 2.4f, StrokeCap.Round, alpha = a)
            }
        }
        "aura_snow" -> if (front) {
            for (i in 0..16) {
                val fall = ((t * (0.12f + hash01(i * 7) * 0.08f) + hash01(i * 3)) % 1f)
                val x = hash01(i * 19) * 200f + wave(t, 2.5f, hash01(i)) * 8f
                val y = fall * 210f - 6f
                drawCircle(Color.White, 1.6f + hash01(i * 29) * 2.2f, Offset(x, y), alpha = 0.9f)
            }
        }
        "aura_orbit" -> {
            for (i in 0..6) {
                val a = t * 70f + i * (360f / 7f)
                val rad = Math.toRadians(a.toDouble())
                val depth = kotlin.math.sin(rad).toFloat()
                if ((depth > 0f) != front) continue
                val p = Offset(100f + kotlin.math.cos(rad).toFloat() * 78f, 116f + depth * 18f)
                val r = 5f + depth * 1.6f
                drawCircle(hsv(i * 51f + t * 40f, 0.65f), r, p)
                drawCircle(Color.White, r * 0.35f, p + Offset(-r * 0.3f, -r * 0.3f), alpha = 0.7f)
            }
        }
        "aura_fire" -> if (!front) {
            for (i in 0..8) {
                val x = 44f + i * 14f
                val h = 50f + hash01(i * 5) * 40f - abs(i - 4) * 8f
                drawFlameTongue(x, 190f, 12f, h, t, i + 10)
            }
        } else {
            for (i in 0..6) {
                val life = ((t * 0.8f + i / 7f) % 1f)
                drawCircle(Color(0xFFFFC53A), 2.4f * (1f - life), Offset(40f + hash01(i * 13) * 120f, 186f - life * 120f), alpha = 1f - life)
            }
        }
        else -> Unit
    }
}

// -- backdrops ------------------------------------------------------------------

fun DrawScope.drawAvatarBackground(id: String?, t: Float) {
    val full = Size(200f, 200f)
    when (id) {
        "bg_paper" -> {
            drawRect(Color(0xFFFFFBF4), size = full)
            for (x in 0..9) for (y in 0..9) drawCircle(Color(0xFFE9DDCC), 1.2f, Offset(10f + x * 20f, 10f + y * 20f))
        }
        "bg_sky" -> {
            drawRect(Color(0xFF8FD3FF), size = full)
            drawCircle(Color(0xFFB5E3FF), 120f, Offset(100f, 230f))
            drawCircle(Color(0xFFFFE27A), 18f, Offset(160f, 40f))
            listOf(0, 1).forEach { i ->
                val x = ((t * 6f + i * 110f) % 260f) - 40f
                val y = 50f + i * 46f
                listOf(Offset(0f, 0f) to 12f, Offset(14f, -6f) to 15f, Offset(30f, 0f) to 11f).forEach { (o, r) ->
                    drawCircle(Color.White, r, Offset(x + o.x, y + o.y), alpha = 0.95f)
                }
            }
        }
        "bg_mint" -> {
            drawRect(Color(0xFFC9F2DD), size = full)
            for (i in 0..11) drawCircle(Color(0xFF9BE3C0), 6f + hash01(i) * 10f, Offset(hash01(i * 3) * 200f, hash01(i * 7) * 200f), alpha = 0.7f)
        }
        "bg_sunset" -> {
            drawRect(bands(listOf(Color(0xFF7A49C9), Color(0xFFD9549A), Color(0xFFFF8A4D), Color(0xFFFFC65C)), 0f, 200f), size = full)
            drawCircle(Color(0xFFFFE9A8), 40f, Offset(100f, 150f), alpha = 0.9f)
            for (i in 0..3) drawRect(Color(0xFFFF7A59), Offset(0f, 150f + i * 10f), Size(200f, 4f), alpha = 0.5f)
        }
        "bg_grid" -> {
            drawRect(Color(0xFFFDFCF8), size = full)
            for (i in 0..20) {
                drawLine(Color(0xFFD7E6F5), Offset(i * 10f, 0f), Offset(i * 10f, 200f), 0.8f)
                drawLine(Color(0xFFD7E6F5), Offset(0f, i * 10f), Offset(200f, i * 10f), 0.8f)
            }
            drawPath(starPath(30f, 34f, 9f), Color(0xFF8AB4E8), style = Stroke(1.6f))
            drawPath(heartPath(170f, 40f, 12f), Color(0xFFFF8FA3), style = Stroke(1.6f))
            drawPath(svg("M160 150 c6 -6 14 0 8 6 c-8 8 -20 -2 -12 -12 c10 -12 28 0 18 14"), Color(0xFFFFB020), style = Stroke(1.6f, cap = StrokeCap.Round))
        }
        "bg_splash" -> {
            drawRect(Color.White, size = full)
            listOf(
                Triple(Offset(36f, 40f), 30f, Color(0xFFFF4F6D)), Triple(Offset(170f, 60f), 34f, Color(0xFF2FA8FF)),
                Triple(Offset(40f, 170f), 32f, Color(0xFFFFC21A)), Triple(Offset(168f, 168f), 28f, Color(0xFF4CD964)),
                Triple(Offset(100f, 16f), 18f, Color(0xFFA35CFF))
            ).forEachIndexed { i, (c, r, col) ->
                drawCircle(col, r, c, alpha = 0.85f)
                for (k in 0..5) {
                    val p = polar(c.x, c.y, r + 6f + hash01(i * 9 + k) * 12f, k * 60f + i * 20f)
                    drawCircle(col, 2f + hash01(i * 5 + k) * 4f, p, alpha = 0.85f)
                }
            }
        }
        "bg_night" -> {
            drawRect(Color(0xFF1B1F5C), size = full)
            drawCircle(Color(0xFF262A73), 140f, Offset(100f, 250f))
            drawStarfield(t, 0f, 0f, 200f, 140f, seed = 7, count = 26)
            drawCircle(Color(0xFFFFF2C2), 16f, Offset(156f, 42f))
            drawCircle(Color(0xFF26206B), 14f, Offset(164f, 36f))
        }
        "bg_aurora" -> {
            drawRect(Color(0xFF0A1633), size = full)
            drawStarfield(t, 0f, 0f, 200f, 120f, seed = 9, count = 16)
            listOf(Color(0xFF3CFFB0), Color(0xFF7B4DFF), Color(0xFF39C3FF)).forEachIndexed { i, c ->
                val ribbon = Path().apply {
                    moveTo(-10f, 80f + i * 18f)
                    for (k in 0..11) {
                        val x = k * 20f
                        val y = 70f + i * 18f + wave(t + k * 0.25f, 4f + i, i * 0.3f) * 14f
                        lineTo(x, y)
                    }
                    lineTo(210f, 200f); lineTo(-10f, 200f); close()
                }
                drawPath(ribbon, c, alpha = 0.75f)
            }
        }
        "bg_rays" -> {
            drawRect(Color(0xFFFFF4D6), size = full)
            val spin = t * 12f
            for (i in 0..15) {
                val a0 = spin + i * 22.5f
                val p1 = polar(100f, 110f, 220f, a0)
                val p2 = polar(100f, 110f, 220f, a0 + 11.25f)
                drawPath(Path().apply { moveTo(100f, 110f); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }, hsv(i * 22.5f, 0.45f), alpha = 0.6f)
            }
            drawCircle(Color(0xFFFFF4D6), 46f, Offset(100f, 110f))
        }
        "bg_embers" -> {
            drawRect(Color(0xFF5A1408), size = full)
            drawCircle(Color(0xFF7A1E0A), 130f, Offset(100f, 250f))
            for (i in 0..18) {
                val life = ((t * (0.25f + hash01(i * 3) * 0.2f) + hash01(i * 11)) % 1f)
                val x = hash01(i * 17) * 200f + wave(t, 1.8f, hash01(i)) * 10f
                val y = 210f - life * 220f
                drawCircle(Color(0xFFFFC53A), 1.5f + hash01(i * 23) * 2.5f, Offset(x, y), alpha = (1f - life) * 0.9f)
            }
        }
        else -> {
            drawRect(Color(0xFFFFE2CC), size = full)
            drawCircle(Color(0xFFFFD3B6), 110f, Offset(100f, 240f))
        }
    }
}
