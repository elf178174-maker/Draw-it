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
import kotlin.math.cos
import kotlin.math.PI

private val gold = Color(0xFFFFC800)
private val goldLight = Color(0xFFFFE680)
private val goldDeep = Color(0xFFE09A00)

private fun DrawScope.goldShape(path: Path) = flat(path, gold, goldDeep, dx = -4f, dy = -3f)

fun DrawScope.drawHat(id: String?, t: Float) {
    when (id) {
        "hat_beret" -> rotate(-9f, pivot = Offset(100f, 36f)) {
            ovalAt(Color(0xFFB0152A), 96f, 40f, 46f, 12f)
            flat(svg("M54 40 C56 18 80 10 100 12 C124 14 142 24 140 40 Z"), Color(0xFFFF4B4B), Color(0xFFD93030), dx = -4f, dy = -4f)
            drawRoundRect(Color(0xFFFF8A8A), Offset(70f, 20f), Size(22f, 5f), CornerRadius(2.5f, 2.5f))
            drawRoundRect(Color(0xFFB0152A), Offset(97f, 4f), Size(5f, 9f), CornerRadius(2.5f, 2.5f))
        }
        "hat_cap" -> {
            flat(svg("M57 50 C56 24 76 14 100 14 C124 14 144 24 143 50 Z"), Color(0xFF1CB0F6), Color(0xFF1287C7), dx = -5f, dy = -3f)
            listOf(80f, 100f, 120f).forEach { x ->
                drawLine(Color(0xFF1287C7), Offset(x, 16f), Offset(x + (x - 100f) * 0.5f, 50f), 1.6f)
            }
            drawPath(svg("M52 50 C70 62 130 62 148 50 C136 44 64 44 52 50 Z"), Color(0xFF0F6FA3))
            drawCircle(Color(0xFF0F6FA3), 4f, Offset(100f, 15f))
            drawCircle(Color.White, 8f, Offset(100f, 34f))
            drawPath(starPath(100f, 34f, 6f), Color(0xFFFF6A1F))
        }
        "hat_beanie" -> {
            val bob = wave(t, 3.6f) * 1.2f
            flat(svg("M56 52 C54 22 76 12 100 12 C124 12 146 22 144 52 Z"), Color(0xFF2BCB8F), Color(0xFF1DA673), dx = -5f, dy = -3f)
            for (i in 0..8) drawLine(Color(0xFF128A67), Offset(64f + i * 9f, 20f + abs(4 - i) * 2f), Offset(64f + i * 9f, 40f), 1.4f, alpha = 0.5f)
            drawRoundRect(Color(0xFF128A67), Offset(54f, 40f), Size(92f, 14f), CornerRadius(7f, 7f))
            for (i in 0..14) drawLine(Color(0xFF0E6F53), Offset(58f + i * 6f, 42f), Offset(58f + i * 6f, 52f), 1.6f, alpha = 0.55f)
            for (i in 0..9) {
                val a = i * 36f
                drawCircle(Color(0xFFFFF4E0), 4.2f, polar(100f, 8f - bob, 6f, a))
            }
            drawCircle(Color(0xFFFFF4E0), 7f, Offset(100f, 8f - bob))
        }
        "hat_party" -> rotate(12f, pivot = Offset(104f, 40f)) {
            val cone = svg("M78 42 L106 -6 L132 40 C116 46 94 47 78 42 Z")
            flat(cone, Color(0xFFFF6FC0), Color(0xFFD94FA0), dx = -5f, dy = -2f)
            clipPath(cone) {
                for (i in 0..4) drawLine(Color(0xFFFFE066), Offset(70f, 44f - i * 11f), Offset(140f, 30f - i * 11f), 3.6f)
                listOf(Offset(98f, 28f), Offset(114f, 14f), Offset(92f, 6f)).forEach { drawCircle(Color.White, 2f, it) }
            }
            for (i in 0..7) drawCircle(hsv(i * 45f + t * 90f), 2.6f, polar(106f, -8f, 5f, i * 45f))
            drawCircle(Color(0xFFFFE066), 4.5f, Offset(106f, -8f))
        }
        "hat_headphones" -> {
            drawArc(Color(0xFF2B2B3A), 190f, 160f, false, Offset(52f, 18f), Size(96f, 90f), style = Stroke(7f, cap = StrokeCap.Round))
            drawArc(Color(0xFF4A4A60), 200f, 140f, false, Offset(54f, 20f), Size(92f, 86f), style = Stroke(2f, cap = StrokeCap.Round))
            listOf(-1f, 1f).forEach { s ->
                val x = 100f + s * 46f
                drawRoundRect(Color(0xFF2B2B3A), Offset(x - 9f, 64f), Size(18f, 30f), CornerRadius(9f, 9f))
                drawRoundRect(Color(0xFFFF4B4B), Offset(x - 7f + s * 3f, 67f), Size(14f, 24f), CornerRadius(7f, 7f))
                drawRoundRect(Color(0xFFD93030), Offset(x - 7f + s * 3f, 82f), Size(14f, 9f), CornerRadius(4.5f, 4.5f))
                val glow = 0.4f + 0.4f * (0.5f + 0.5f * wave(t, 0.8f))
                drawCircle(Color.White, 2f, Offset(x + s * 3f, 74f), alpha = glow)
            }
        }
        "hat_cat" -> {
            drawArc(Color(0xFF2B2B3A), 200f, 140f, false, Offset(58f, 26f), Size(84f, 70f), style = Stroke(4f, cap = StrokeCap.Round))
            listOf(-1f, 1f).forEach { s ->
                val twitch = bump(t % 4.5f, 2.2f + (if (s < 0) 0f else 0.15f), 0.25f) * 6f
                rotate(s * (8f + twitch), pivot = Offset(100f + s * 26f, 36f)) {
                    val ear = Path().apply {
                        moveTo(100f + s * 12f, 36f); lineTo(100f + s * 32f, 4f); lineTo(100f + s * 42f, 40f); close()
                    }
                    drawPath(ear, Color(0xFF2B2B3A))
                    val inner = Path().apply {
                        moveTo(100f + s * 18f, 34f); lineTo(100f + s * 31f, 13f); lineTo(100f + s * 37f, 36f); close()
                    }
                    drawPath(inner, Color(0xFFFF9EC4))
                }
            }
        }
        "hat_flowers" -> {
            drawArc(Color(0xFF5BAE4A), 195f, 150f, false, Offset(56f, 30f), Size(88f, 40f), style = Stroke(4f, cap = StrokeCap.Round))
            val colors = listOf(Color(0xFFFF6FA5), Color(0xFFFFD23F), Color(0xFFFFFFFF), Color(0xFFB57BFF), Color(0xFFFF8A3D), Color(0xFF7FD6FF), Color(0xFFFF6FA5))
            colors.forEachIndexed { i, c ->
                val p = polar(100f, 50f, 46f, 195f + i * 25f)
                val sway = wave(t, 2.6f, i * 0.13f) * 3f
                if (i % 2 == 0) {
                    rotate(sway - 30f, pivot = p) { ovalAt(Color(0xFF4C9A3C), p.x + 8f, p.y + 2f, 6f, 3f) }
                }
                for (k in 0..4) {
                    val q = polar(p.x, p.y, 5.2f, k * 72f + sway * 5f)
                    drawCircle(c, 4.4f, q)
                }
                drawCircle(Color(0xFFFFC21A).takeIf { c != Color(0xFFFFD23F) } ?: Color(0xFFFF8A3D), 3f, p)
            }
        }
        "hat_horns" -> listOf(-1f, 1f).forEach { s ->
            val horn = Path().apply {
                moveTo(100f + s * 18f, 40f)
                quadraticBezierTo(100f + s * 20f, 18f, 100f + s * 36f, 10f)
                quadraticBezierTo(100f + s * 30f, 26f, 100f + s * 32f, 42f)
                close()
            }
            flat(horn, Color(0xFFFF4B4B), Color(0xFFC72626), dx = s * 3f, dy = -2f)
        }
        "hat_propeller" -> {
            val dome = svg("M58 50 C56 22 78 12 100 12 C122 12 144 22 142 50 Z")
            drawPath(dome, Color(0xFFFFD23F))
            clipPath(dome) {
                val seg = listOf(Color(0xFFFF4F6D), Color(0xFF2FA8FF), Color(0xFF4CD964), Color(0xFFFFD23F))
                for (i in 0..5) {
                    drawPath(
                        Path().apply { moveTo(100f, 6f); lineTo(30f + i * 28f, 54f); lineTo(44f + i * 28f, 54f); close() },
                        seg[i % seg.size]
                    )
                }
            }
            drawPath(svg("M54 50 C70 56 130 56 146 50 C138 46 62 46 54 50 Z"), Color(0xFFE0A800))
            drawLine(Color(0xFF6B6B7A), Offset(100f, 12f), Offset(100f, 0f), 2.6f, StrokeCap.Round)
            // Two blades seen from the side: their length follows cos(spin).
            val spin = t * 7f
            val reach = cos(spin) * 30f
            val reach2 = cos(spin + PI.toFloat() / 2f) * 30f
            ovalAt(Color(0xFFFF4F6D), 100f + reach / 2f, -1f, abs(reach) / 2f + 1.5f, 3.4f)
            ovalAt(Color(0xFF2FA8FF), 100f - reach / 2f, -1f, abs(reach) / 2f + 1.5f, 3.4f)
            ovalAt(Color(0xFF4CD964), 100f + reach2 / 2f, -1f, abs(reach2) / 2f + 1.5f, 3.4f, alpha = 0.85f)
            drawCircle(Color(0xFF4A4A58), 3.4f, Offset(100f, -1f))
        }
        "hat_wizard" -> {
            val sway = wave(t, 2.8f) * 4f
            val cone = Path().apply {
                moveTo(66f, 44f)
                cubicTo(80f, 30f, 96f, 10f, 110f + sway * 0.4f, -14f)
                quadraticBezierTo(130f + sway, -8f, 138f + sway, 4f)
                quadraticBezierTo(122f + sway * 0.5f, 6f, 118f, 14f)
                cubicTo(122f, 26f, 128f, 34f, 134f, 44f)
                close()
            }
            flat(cone, Color(0xFF7C4DFF), Color(0xFF5A2FD6), dx = -6f, dy = -2f)
            clipPath(cone) {
                drawStarfield(t, 66f, -14f, 72f, 56f, seed = 21, count = 9, color = Color(0xFFFFE27A))
                drawRect(gold, Offset(60f, 33f), Size(80f, 6f))
            }
            ovalAt(Color(0xFF4A22B8), 100f, 46f, 52f, 9f)
            ovalAt(Color(0xFF6A3DF0), 100f, 44.5f, 49f, 6.5f)
            drawPath(starPath(140f + sway, 4f, 6f, rotation = t * 40f), Color(0xFFFFE27A))
        }
        "hat_tophat" -> {
            flat(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(72f, -6f, 128f, 44f, 5f, 5f)) }, Color(0xFF34343F), Color(0xFF1E1E26), dx = -7f, dy = 0f)
            drawRect(Color(0xFFE0243C), Offset(72f, 30f), Size(56f, 8f))
            ovalAt(Color(0xFF16161E), 100f, 46f, 48f, 7f)
            drawRoundRect(Color(0xFF4A4A58), Offset(78f, 0f), Size(5f, 24f), CornerRadius(2.5f, 2.5f))
            drawPath(starPath(116f, 34f, 4f), Color(0xFFFFC53A))
        }
        "hat_halo" -> {
            val bob = wave(t, 2.4f) * 2.5f
            val y = 16f + bob
            drawOval(goldDeep, Offset(66f, y - 6f), Size(68f, 16f), style = Stroke(7f))
            drawOval(gold, Offset(66f, y - 8f), Size(68f, 16f), style = Stroke(6f))
            drawArc(goldLight, 200f, 70f, false, Offset(66f, y - 8f), Size(68f, 16f), style = Stroke(2.4f, cap = StrokeCap.Round))
            val glint = (t * 0.6f) % 1f
            drawCircle(Color.White, 2.4f, Offset(66f + glint * 68f, y - 7f + abs(glint - 0.5f) * 4f), alpha = 0.9f)
        }
        "hat_crown" -> {
            val crown = svg("M64 50 L60 16 L80 30 L90 6 L100 26 L110 6 L120 30 L140 16 L136 50 Z")
            goldShape(crown)
            clipPath(crown) {
                // A glint sweeps across every couple of seconds.
                val sweep = ((t / 2.4f) % 1f) * 140f - 20f
                drawPath(
                    Path().apply { moveTo(40f + sweep, 60f); lineTo(56f + sweep, 60f); lineTo(76f + sweep, 0f); lineTo(60f + sweep, 0f); close() },
                    Color.White, alpha = 0.55f
                )
            }
            drawRoundRect(goldDeep, Offset(62f, 39f), Size(76f, 12f), CornerRadius(4f, 4f))
            drawRoundRect(gold, Offset(62f, 38f), Size(76f, 8f), CornerRadius(4f, 4f))
            listOf(Offset(80f, 44f) to Color(0xFF2FA8FF), Offset(100f, 44f) to Color(0xFFE8233F), Offset(120f, 44f) to Color(0xFF2ECC71)).forEach { (p, c) ->
                drawCircle(c.darken(0.25f), 4.6f, p)
                drawCircle(c, 3.6f, p)
                drawCircle(Color.White, 1.2f, p + Offset(-1.2f, -1.2f), alpha = 0.9f)
            }
            listOf(Offset(60f, 16f), Offset(90f, 6f), Offset(110f, 6f), Offset(140f, 16f)).forEachIndexed { i, p ->
                drawCircle(goldLight, 3f, p)
                val tw = (0.5f + 0.5f * wave(t, 1.6f, i * 0.25f))
                drawPath(sparklePath(p.x + 3f, p.y - 6f, 5f * tw), Color.White, alpha = tw)
            }
        }
        "hat_flamecrown" -> {
            drawRoundRect(goldDeep, Offset(62f, 38f), Size(76f, 12f), CornerRadius(5f, 5f))
            drawRoundRect(gold, Offset(62f, 38f), Size(76f, 8f), CornerRadius(4f, 4f))
            for (i in 0..4) {
                val x = 70f + i * 15f
                val h = 26f + (if (i == 2) 12f else if (i % 2 == 1) 4f else 0f)
                drawFlameTongue(x, 40f, 9f, h, t, i)
            }
            drawCircle(Color(0xFFE8233F), 3.6f, Offset(100f, 44f))
        }
        else -> Unit
    }
}

/** One flickering flame tongue standing on (x, baseY). Shared by fire items. */
fun DrawScope.drawFlameTongue(x: Float, baseY: Float, halfWidth: Float, height: Float, t: Float, seed: Int) {
    val flick = wave(t, 0.42f + hash01(seed) * 0.25f, hash01(seed * 3)) * height * 0.12f
    val lean = wave(t, 0.9f + hash01(seed * 5) * 0.4f) * halfWidth * 0.35f
    val tipY = baseY - height - flick
    val outer = Path().apply {
        moveTo(x - halfWidth, baseY)
        cubicTo(x - halfWidth * 1.1f, baseY - height * 0.5f, x + lean - halfWidth * 0.2f, baseY - height * 0.75f, x + lean, tipY)
        cubicTo(x + lean + halfWidth * 0.3f, baseY - height * 0.7f, x + halfWidth * 1.15f, baseY - height * 0.45f, x + halfWidth, baseY)
        close()
    }
    drawPath(outer, Color(0xFFFF6A00))
    val core = Path().apply {
        moveTo(x - halfWidth * 0.45f, baseY)
        quadraticBezierTo(x - halfWidth * 0.4f, baseY - height * 0.4f, x + lean * 0.6f, baseY - height * 0.55f)
        quadraticBezierTo(x + halfWidth * 0.45f, baseY - height * 0.3f, x + halfWidth * 0.45f, baseY)
        close()
    }
    drawPath(core, Color(0xFFFFC800))
}

/** Shared helper: scale a whole hat about the crown (used by previews). */
inline fun DrawScope.hatScale(s: Float, block: DrawScope.() -> Unit) = scale(s, s, pivot = Offset(100f, 40f)) { block() }
