package com.drawit.app.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate

/*
 * Hair comes in two layers: the back layer sits behind the head (long hair,
 * buns, the afro) and the front layer is the fringe over the forehead.
 */

private fun DrawScope.hairShape(path: Path, paint: HairPaint, t: Float) {
    val brush = paint.brushOrNull(t)
    if (brush == null) {
        flat(path, paint.base, paint.shade, dx = -4f, dy = -4f)
    } else {
        drawPath(path, paint.shade)
        clipPath(path) { translate(-4f, -4f) { drawPath(path, brush) } }
    }
}

/** A glossy streak that follows the crown of the head. */
private fun DrawScope.shine(paint: HairPaint, cx: Float = 84f, cy: Float = 40f, sweep: Float = 50f) {
    drawArc(
        paint.light, startAngle = 200f, sweepAngle = sweep, useCenter = false,
        topLeft = Offset(cx - 26f, cy - 4f), size = androidx.compose.ui.geometry.Size(52f, 40f),
        style = Stroke(4f, cap = StrokeCap.Round), alpha = 0.9f
    )
}

fun DrawScope.drawHairBack(id: String?, paint: HairPaint, t: Float) {
    val sway = wave(t, 3.2f) * 1.5f
    when (id) {
        "hair_long" -> {
            val p = svg(
                "M54 70 C52 36 74 24 100 24 C126 24 148 36 146 70 C148 98 152 124 150 142 " +
                    "C138 150 120 148 112 136 L88 136 C80 148 62 150 50 142 C48 124 52 98 54 70 Z"
            )
            rotate(sway * 0.6f, pivot = Offset(100f, 40f)) { hairShape(p, paint, t) }
        }
        "hair_bob" -> hairShape(
            svg(
                "M54 72 C52 38 74 25 100 25 C126 25 148 38 146 72 C148 88 150 100 148 110 " +
                    "C140 116 128 114 122 108 L78 108 C72 114 60 116 52 110 C50 100 52 88 54 72 Z"
            ), paint, t
        )
        "hair_curly" -> {
            for (i in 0..12) {
                val a = 165f + i * (210f / 12f)
                val c = polar(100f, 76f, 48f, a)
                val r = 13f + hash01(i * 7) * 3f
                drawCircle(paint.shade, r, c)
            }
            // Ringlets down the sides.
            listOf(-1f, 1f).forEach { s ->
                for (k in 0..2) drawCircle(paint.shade, 10f - k, Offset(100f + s * (50f - k * 2f), 92f + k * 14f + sway * s))
            }
        }
        "hair_afro" -> {
            for (i in 0..17) {
                val a = i * 20f
                val c = polar(100f, 62f, 50f, a)
                drawCircle(paint.base, 18f, c)
            }
            drawCircle(paint.base, 52f, Offset(100f, 62f))
            for (i in 0..5) drawCircle(paint.shade, 16f, polar(100f, 62f, 50f, 20f + i * 24f))
        }
        "hair_ponytail" -> {
            val swing = wave(t, 1.9f) * 5f
            rotate(swing, pivot = Offset(132f, 46f)) {
                hairShape(
                    svg("M126 40 C150 38 162 58 158 86 C155 106 162 122 170 136 C150 136 136 118 137 96 C138 76 134 58 120 46 Z"),
                    paint, t
                )
                drawPath(svg("M150 70 C150 92 154 110 162 126"), paint.light, alpha = 0.4f, style = Stroke(2.5f, cap = StrokeCap.Round))
            }
            drawCircle(Color(0xFFFF5A8A), 6f, Offset(130f, 44f))
        }
        "hair_buns" -> {
            listOf(66f, 134f).forEach { x ->
                drawCircle(paint.base, 17f, Offset(x, 34f))
                drawCircle(paint.shade, 17f, Offset(x, 34f), style = Stroke(1.5f), alpha = 0.6f)
                drawArc(paint.shade, 200f, 220f, false, Offset(x - 10f, 24f), androidx.compose.ui.geometry.Size(20f, 20f),
                    style = Stroke(2.2f, cap = StrokeCap.Round), alpha = 0.7f)
                drawCircle(paint.light, 4f, Offset(x - 6f, 27f), alpha = 0.5f)
            }
        }
        "hair_pigtails" -> {
            listOf(-1f, 1f).forEach { s ->
                val swing = wave(t, 2.1f, if (s < 0) 0f else 0.25f) * 4f
                rotate(swing * s, pivot = Offset(100f + s * 44f, 80f)) {
                    val tail = Path().apply {
                        moveTo(100f + s * 42f, 74f)
                        cubicTo(100f + s * 60f, 80f, 100f + s * 66f, 104f, 100f + s * 60f, 128f)
                        cubicTo(100f + s * 58f, 138f, 100f + s * 50f, 144f, 100f + s * 44f, 146f)
                        cubicTo(100f + s * 50f, 130f, 100f + s * 50f, 110f, 100f + s * 40f, 92f)
                        close()
                    }
                    hairShape(tail, paint, t)
                }
                drawCircle(Color(0xFF44C2FF), 5.5f, Offset(100f + s * 44f, 80f))
            }
        }
        else -> Unit
    }
}

fun DrawScope.drawHairFront(id: String?, paint: HairPaint, t: Float, hatOn: Boolean) {
    when (id) {
        "hair_short" -> {
            hairShape(
                svg(
                    "M55 78 C52 46 72 27 100 27 C128 27 148 46 145 78 C141 70 138 63 132 58 " +
                        "C122 62 110 60 102 52 C96 60 80 64 68 60 C63 65 58 71 55 78 Z"
                ), paint, t
            )
            shine(paint)
        }
        "hair_buzz" -> {
            drawPath(
                svg("M57 72 C57 44 76 31 100 31 C124 31 143 44 143 72 C138 58 124 51 100 51 C76 51 62 58 57 72 Z"),
                paint.base, alpha = 0.92f
            )
            for (i in 0..16) {
                val x = 66f + hash01(i * 3) * 68f
                val y = 36f + hash01(i * 5 + 1) * 12f
                drawCircle(paint.shade, 1f, Offset(x, y), alpha = 0.5f)
            }
        }
        "hair_long" -> {
            hairShape(
                svg(
                    "M55 82 C52 44 74 26 100 26 C126 26 148 44 145 82 C142 66 134 56 122 50 " +
                        "C114 56 106 58 100 50 C94 58 86 56 78 50 C66 56 58 66 55 82 Z"
                ), paint, t
            )
            shine(paint, cx = 82f, cy = 38f)
        }
        "hair_bob" -> {
            hairShape(
                svg("M56 78 C54 44 74 27 100 27 C126 27 146 44 144 78 C142 70 140 64 138 60 L62 60 C60 64 58 70 56 78 Z"),
                paint, t
            )
            // Blunt fringe strands.
            listOf(78f, 92f, 108f, 122f).forEach { x ->
                drawLine(paint.shade, Offset(x, 50f), Offset(x + 1f, 59f), 1.6f, StrokeCap.Round, alpha = 0.5f)
            }
            shine(paint, cy = 37f)
        }
        "hair_curly" -> {
            for (i in 0..6) {
                val x = 66f + i * 11.3f
                val y = 44f + if (i % 2 == 0) 0f else 5f
                drawCircle(paint.base, 11f, Offset(x, y))
                drawCircle(paint.light, 3f, Offset(x - 3f, y - 4f), alpha = 0.45f)
            }
            for (i in 0..3) drawCircle(paint.base, 10f, Offset(74f + i * 17f, 31f))
        }
        "hair_spiky" -> {
            hairShape(
                svg(
                    "M55 80 C54 56 60 42 70 36 L64 18 L82 28 L88 10 L101 26 L113 8 L119 28 L136 18 L131 38 " +
                        "C140 46 146 60 145 80 C140 68 132 60 122 56 L114 64 L106 54 L98 64 L90 54 L82 64 L76 56 " +
                        "C66 60 58 68 55 80 Z"
                ), paint, t
            )
            shine(paint, cy = 34f)
        }
        "hair_ponytail", "hair_pigtails" -> {
            hairShape(
                svg(
                    "M56 78 C53 46 74 28 100 28 C126 28 147 46 144 78 C140 64 128 54 106 50 " +
                        "C104 54 102 56 100 56 C98 56 96 54 94 50 C72 54 60 64 56 78 Z"
                ), paint, t
            )
            shine(paint, cy = 38f)
        }
        "hair_buns" -> {
            hairShape(
                svg("M56 76 C54 46 74 29 100 29 C126 29 146 46 144 76 C138 62 124 54 100 54 C76 54 62 62 56 76 Z"),
                paint, t
            )
            // Little face-framing strands.
            listOf(-1f, 1f).forEach { s ->
                drawPath(
                    Path().apply { moveTo(100f + s * 40f, 66f); quadraticBezierTo(100f + s * 44f, 84f, 100f + s * 38f, 98f) },
                    paint.base, style = Stroke(4f, cap = StrokeCap.Round)
                )
            }
            shine(paint, cy = 38f)
        }
        "hair_afro" -> {
            drawPath(
                svg("M58 74 C56 48 74 34 100 34 C126 34 144 48 142 74 C138 62 126 54 100 54 C74 54 62 62 58 74 Z"),
                paint.base
            )
            for (i in 0..6) drawCircle(paint.base, 7f, Offset(68f + i * 10.7f, 52f + if (i % 2 == 0) 2f else 0f))
        }
        "hair_mohawk" -> {
            drawPath(
                svg("M57 72 C57 44 76 31 100 31 C124 31 143 44 143 72 C138 58 124 51 100 51 C76 51 62 58 57 72 Z"),
                paint.shade, alpha = 0.55f
            )
            if (!hatOn) {
                val flick = wave(t, 1.3f) * 1.5f
                hairShape(
                    svg("M88 52 C84 38 86 24 90 14 L94 24 L98 4 L103 22 L110 6 L112 26 L118 18 C118 32 116 44 112 52 Z"),
                    paint, t
                )
                drawLine(paint.light, Offset(100f + flick, 10f), Offset(101f, 46f), 2.4f, StrokeCap.Round, alpha = 0.45f)
            }
        }
        else -> Unit
    }
}
