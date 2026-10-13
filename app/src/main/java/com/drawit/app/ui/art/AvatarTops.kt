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
import androidx.compose.ui.graphics.drawscope.translate

class TopStyle(val sleeveColor: Color, val sleeve: Float)

fun topStyle(id: String?): TopStyle = when (id) {
    "top_tee_navy" -> TopStyle(Color(0xFF2E3A66), 0.42f)
    "top_hoodie" -> TopStyle(Color(0xFFFF5A1F), 0.92f)
    "top_stripes" -> TopStyle(Color(0xFFF7F4EE), 0.85f)
    "top_smock" -> TopStyle(Color(0xFF9DC3E6), 0.5f)
    "top_overalls" -> TopStyle(Color(0xFFFFD45C), 0.42f)
    "top_varsity" -> TopStyle(Color(0xFFF4E9D4), 0.95f)
    "top_tux" -> TopStyle(Color(0xFF23232E), 0.95f)
    "top_space" -> TopStyle(Color(0xFFE9EEF5), 0.95f)
    "top_flame" -> TopStyle(Color(0xFF2B2230), 0.92f)
    "top_royal" -> TopStyle(Color(0xFF7A2252), 0.95f)
    else -> TopStyle(Color(0xFFFF7A59), 0.42f)
}

private val torso = "M76 118 C68 122 66 132 66 142 L66 170 C66 176 70 178 76 178 L124 178 " +
    "C130 178 134 176 134 170 L134 142 C134 132 132 122 124 118 C116 114 84 114 76 118 Z"

fun torsoPath(): Path = svg(torso)

private fun DrawScope.fillTorso(base: Color, shade: Color = base.darken(0.15f)) {
    flat(torsoPath(), base, shade, dx = -7f, dy = -3f)
}

private fun DrawScope.roundNeck(color: Color) {
    drawArc(color, 20f, 140f, false, Offset(88f, 106f), Size(24f, 18f), style = Stroke(3.2f, cap = StrokeCap.Round))
}

fun DrawScope.drawTop(id: String?, t: Float) {
    val body = torsoPath()
    when (id) {
        "top_tee_navy" -> {
            fillTorso(Color(0xFF2E3A66))
            roundNeck(Color(0xFF1E2747))
            drawPath(starPath(112f, 146f, 7f), Color(0xFFFFC94D))
        }
        "top_hoodie" -> {
            // Hood bunched behind the neck.
            drawPath(svg("M78 116 C82 104 118 104 122 116 C114 124 86 124 78 116 Z"), Color(0xFFE2470F))
            fillTorso(Color(0xFFFF5A1F))
            drawRoundRect(Color(0xFFE2470F), Offset(82f, 150f), Size(36f, 18f), CornerRadius(8f, 8f))
            drawLine(Color(0xFFE2470F), Offset(100f, 150f), Offset(100f, 168f), 1.5f)
            listOf(94f, 106f).forEach { x ->
                drawLine(Color.White, Offset(x, 120f), Offset(x - (x - 100f) * 0.2f, 136f), 2f, StrokeCap.Round)
                drawCircle(Color.White, 2.2f, Offset(x - (x - 100f) * 0.2f, 137f))
            }
            // Tiny pencil badge.
            drawRoundRect(Color(0xFFFFF4E8), Offset(110f, 126f), Size(12f, 4f), CornerRadius(2f, 2f))
            drawPath(Path().apply { moveTo(122f, 126f); lineTo(126f, 128f); lineTo(122f, 130f); close() }, Color(0xFF3A2A20))
        }
        "top_stripes" -> {
            fillTorso(Color(0xFFF7F4EE))
            clipPath(body) {
                for (i in 0..7) drawRect(Color(0xFF243B73), Offset(60f, 122f + i * 8f), Size(80f, 3.6f))
            }
            roundNeck(Color(0xFF243B73))
        }
        "top_smock" -> {
            fillTorso(Color(0xFF9DC3E6))
            drawPath(svg("M80 124 L120 124 L124 178 L76 178 Z"), Color(0xFFF2E6CF))
            clipPath(svg("M80 124 L120 124 L124 178 L76 178 Z")) {
                listOf(
                    Triple(88f, 136f, Color(0xFFFF4F6D)), Triple(110f, 142f, Color(0xFF2FB8FF)),
                    Triple(96f, 160f, Color(0xFFFFC21A)), Triple(116f, 166f, Color(0xFF6BD36B)),
                    Triple(84f, 170f, Color(0xFFA35CFF))
                ).forEach { (x, y, c) ->
                    drawCircle(c, 4.2f, Offset(x, y))
                    drawCircle(c, 1.6f, Offset(x + 5f, y - 3f))
                    drawCircle(c, 1.2f, Offset(x - 4f, y + 4f))
                }
            }
            drawRoundRect(Color(0xFFE2D2B4), Offset(102f, 150f), Size(16f, 12f), CornerRadius(3f, 3f))
            drawLine(Color(0xFFB07A44), Offset(106f, 152f), Offset(104f, 140f), 2f, StrokeCap.Round)
            drawLine(Color(0xFF6A4A2E), Offset(112f, 152f), Offset(115f, 141f), 2f, StrokeCap.Round)
            drawCircle(Color(0xFFFF4F6D), 1.8f, Offset(104f, 139.5f))
        }
        "top_overalls" -> {
            fillTorso(Color(0xFFFFD45C))
            roundNeck(Color(0xFFE6B83A))
            val bib = svg("M80 138 L120 138 L122 178 L78 178 Z")
            flat(bib, Color(0xFF4F80C9), Color(0xFF3B67AD), dx = -5f, dy = -2f)
            drawLine(Color(0xFF3E6FB8), Offset(82f, 140f), Offset(78f, 120f), 6f, StrokeCap.Round)
            drawLine(Color(0xFF3E6FB8), Offset(118f, 140f), Offset(122f, 120f), 6f, StrokeCap.Round)
            listOf(84f, 116f).forEach { drawCircle(Color(0xFFFFD45C), 2.6f, Offset(it, 141f)) }
            drawRoundRect(Color(0xFF2F5B9E), Offset(90f, 148f), Size(20f, 12f), CornerRadius(3f, 3f))
        }
        "top_varsity" -> {
            fillTorso(Color(0xFFD7263D))
            drawLine(Color(0xFFF4E9D4), Offset(100f, 118f), Offset(100f, 178f), 2f)
            listOf(92f, 108f).forEach { x ->
                for (k in 0..3) drawCircle(Color(0xFFF4E9D4), 1.6f, Offset(x + (if (x < 100) 6f else -6f), 130f + k * 12f))
            }
            drawRect(Color(0xFFF4E9D4), Offset(66f, 172f), Size(68f, 6f))
            clipPath(body) { for (i in 0..1) drawRect(Color(0xFF1F2A44), Offset(66f, 173f + i * 3f), Size(68f, 1f)) }
            // Letter patch.
            drawRoundRect(Color(0xFFF4E9D4), Offset(78f, 130f), Size(15f, 17f), CornerRadius(3f, 3f))
            drawPath(svg("M82 133 L82 144 L86 144 C90 144 91 141 91 138.5 C91 136 90 133 86 133 Z"), Color(0xFFD7263D))
            roundNeck(Color(0xFF1F2A44))
        }
        "top_tux" -> {
            fillTorso(Color(0xFF23232E))
            drawPath(svg("M88 116 L112 116 L100 150 Z"), Color.White)
            drawPath(svg("M84 118 L96 134 L92 158 L80 128 Z"), Color(0xFF34343F))
            drawPath(svg("M116 118 L104 134 L108 158 L120 128 Z"), Color(0xFF34343F))
            // Bow tie.
            drawPath(svg("M100 122 L90 117 L90 127 Z"), Color(0xFFE0243C))
            drawPath(svg("M100 122 L110 117 L110 127 Z"), Color(0xFFE0243C))
            drawCircle(Color(0xFFB0152A), 2.6f, Offset(100f, 122f))
            listOf(136f, 144f).forEach { drawCircle(Color.Black, 1.6f, Offset(100f, it)) }
            drawPath(svg("M114 140 L122 140 L121 143 L115 143 Z"), Color.White)
        }
        "top_space" -> {
            fillTorso(Color(0xFFF2F5FA), Color(0xFFD3DBE7))
            drawRect(Color(0xFFFF6A1F), Offset(66f, 160f), Size(68f, 5f))
            drawRoundRect(Color(0xFF3B4459), Offset(84f, 132f), Size(32f, 20f), CornerRadius(5f, 5f))
            val colors = listOf(Color(0xFF4CFF8F), Color(0xFFFFD23F), Color(0xFFFF4F6D), Color(0xFF39C3FF))
            colors.forEachIndexed { i, c ->
                val on = 0.35f + 0.65f * (0.5f + 0.5f * wave(t, 0.9f + i * 0.37f, i * 0.21f))
                drawCircle(c, 2.6f, Offset(90f + i * 6.7f, 138f), alpha = on)
            }
            drawRoundRect(Color(0xFF4CFF8F), Offset(88f, 144f), Size(24f * (0.5f + 0.5f * wave(t, 2.4f)), 3f), CornerRadius(1.5f, 1.5f), alpha = 0.8f)
            drawArc(Color(0xFFC9D2E0), 10f, 160f, false, Offset(84f, 104f), Size(32f, 24f), style = Stroke(5f, cap = StrokeCap.Round))
        }
        "top_flame" -> {
            fillTorso(Color(0xFF2B2230))
            clipPath(body) {
                val flick = wave(t, 0.7f) * 2.5f
                val flames = Path().apply {
                    moveTo(60f, 180f)
                    for (i in 0..7) {
                        val x = 64f + i * 10f
                        val h = 22f + hash01(i * 3) * 18f + (if (i % 2 == 0) flick else -flick)
                        quadraticBezierTo(x - 4f, 178f - h * 0.5f, x + 2f, 178f - h)
                        quadraticBezierTo(x + 6f, 178f - h * 0.45f, x + 8f, 178f)
                    }
                    lineTo(140f, 182f); close()
                }
                drawPath(flames, Color(0xFFFF6A00))
                translate(0f, 8f) { drawPath(flames, Color(0xFFFFC800)) }
            }
            drawArc(Color(0xFFFF6A1F), 20f, 140f, false, Offset(88f, 106f), Size(24f, 18f), style = Stroke(3.2f, cap = StrokeCap.Round))
        }
        "top_royal" -> {
            fillTorso(Color(0xFF8A2A60), Color(0xFF6A1C48))
            drawPath(svg("M96 118 L104 118 L106 178 L94 178 Z"), Color(0xFFFFC53A))
            listOf(130f, 146f, 162f).forEach { drawCircle(Color(0xFFE8475F), 2.6f, Offset(100f, it)) }
            // Ermine collar.
            drawPath(svg("M70 124 C80 110 120 110 130 124 C120 132 80 132 70 124 Z"), Color(0xFFFAF7F2))
            listOf(80f, 92f, 108f, 120f).forEachIndexed { i, x ->
                drawPath(svg("M${x} ${122 + i % 2} l1.5 3 l-1.5 2 l-1.5 -2 Z"), Color(0xFF231F20))
            }
        }
        else -> {
            fillTorso(Color(0xFFFF7A59))
            roundNeck(Color(0xFFE85D3F))
            drawPath(heartPath(110f, 146f, 7f), Color(0xFFFFF1E6))
        }
    }
}
