package com.drawit.app.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.floor
import kotlin.math.max

/*
 * The player's avatar: a chibi character built from catalogue pieces. The
 * figure is drawn in a fixed 200-unit rig so that every hat, pair of glasses
 * and set of wings lands in the same place on every face.
 */

/** Fixed points of the rig, in art units. */
object Rig {
    const val HEAD_X = 100f
    const val HEAD_Y = 74f
    const val HEAD_RX = 43f
    const val HEAD_RY = 41f
    const val EYE_Y = 80f
    const val EYE_DX = 16f
    const val MOUTH_Y = 98f
    const val HEAD_TOP = HEAD_Y - HEAD_RY
    val shoulderL = Offset(77f, 126f)
    val shoulderR = Offset(123f, 126f)
    val handL = Offset(70f, 160f)
    val handR = Offset(130f, 160f)
    /** Where the right hand goes when it is holding something up. */
    val holdR = Offset(145f, 143f)
    const val GROUND = 190f
}

class SkinPaint(val base: Color, val shade: Color, val light: Color, val blush: Color)

class HairPaint(
    val base: Color,
    val shade: Color,
    val light: Color,
    /** Optional fill override for gradient and animated colours. */
    val brush: ((t: Float) -> Brush)? = null
) {
    /** Special colours (rainbow, flame) paint with a brush; everything else is flat. */
    fun brushOrNull(t: Float): Brush? = brush?.invoke(t)
}

data class AvatarPose(
    /** Height off the ground for celebratory hops. */
    val hop: Float = 0f,
    /** Squeezes the eyes into a smile, whatever eyes are equipped. */
    val joy: Float = 0f
)

fun skinPaint(id: String?): SkinPaint = when (id) {
    "skin_1" -> skin(0xFFFFE5D1)
    "skin_2" -> skin(0xFFF8D2B4)
    "skin_3" -> skin(0xFFEDB98A)
    "skin_4" -> skin(0xFFD49A6A)
    "skin_5" -> skin(0xFFB27548)
    "skin_6" -> skin(0xFF8A5634)
    "skin_7" -> skin(0xFF5E3923)
    "skin_mint" -> SkinPaint(Color(0xFF8EE6A8), Color(0xFF5CC77E), Color(0xFFC4F5D2), Color(0xFF4FB8A0))
    "skin_ocean" -> SkinPaint(Color(0xFF7CC4FF), Color(0xFF4F9BE8), Color(0xFFBFE3FF), Color(0xFF6A7BFF))
    "skin_galaxy" -> SkinPaint(Color(0xFF5B3FA8), Color(0xFF3A2577), Color(0xFF8C6BE0), Color(0xFFE77BFF))
    else -> skin(0xFFF8D2B4)
}

private fun skin(argb: Long): SkinPaint {
    val c = Color(argb)
    return SkinPaint(c, c.darken(0.13f), c.lighten(0.2f), Color(0xFFFF8A8A))
}

fun hairPaint(id: String?): HairPaint = when (id) {
    "hc_black" -> hair(0xFF2B2420)
    "hc_brown" -> hair(0xFF6B4226)
    "hc_auburn" -> hair(0xFF9A3B22)
    "hc_blonde" -> hair(0xFFF2C14E)
    "hc_ginger" -> hair(0xFFE07A2E)
    "hc_silver" -> hair(0xFFD4D8DE)
    "hc_pink" -> hair(0xFFFF8FC8)
    "hc_blue" -> hair(0xFF3E8BFF)
    "hc_mint" -> hair(0xFF5FE0B5)
    "hc_purple" -> hair(0xFF8A4DFF)
    "hc_sunset" -> HairPaint(Color(0xFFFF7A59), Color(0xFFC2407F), Color(0xFFFFC65C)) { _ ->
        bands(listOf(Color(0xFFFFC65C), Color(0xFFFF7A59), Color(0xFFE0508F), Color(0xFF8A49D6)), 20f, 150f)
    }
    "hc_rainbow" -> HairPaint(Color(0xFFFF5E8A), Color(0xFF7B3FBF), Color(0xFFFFE066)) { t ->
        // Hard colour bands that slowly scroll through the hair.
        val shift = (t * 14f) % 120f
        bands(
            listOf(Color(0xFFFF4B4B), Color(0xFFFF9600), Color(0xFFFFC800), Color(0xFF58CC02), Color(0xFF1CB0F6), Color(0xFFA560FF)),
            -100f + shift, 140f + shift, repeat = true
        )
    }
    "hc_flame" -> HairPaint(Color(0xFFFF6A1F), Color(0xFFD9300A), Color(0xFFFFC800)) { t ->
        val f = wave(t, 0.8f) * 5f
        bands(listOf(Color(0xFFFFD84D), Color(0xFFFF9600), Color(0xFFFF5A12), Color(0xFFD9300A)), 18f + f, 140f + f)
    }
    else -> hair(0xFF6B4226)
}

private fun hair(argb: Long): HairPaint {
    val c = Color(argb)
    return HairPaint(c, c.darken(0.2f), c.lighten(0.25f))
}

/**
 * Hard-edged horizontal colour bands between [y0] and [y1]: a gradient with
 * no blending, which keeps special hair colours in the flat house style.
 */
fun bands(colors: List<Color>, y0: Float, y1: Float, repeat: Boolean = false): Brush {
    val stops = ArrayList<Pair<Float, Color>>()
    val n = colors.size
    colors.forEachIndexed { i, c ->
        stops += (i.toFloat() / n) to c
        stops += ((i + 1).toFloat() / n - 0.0001f) to c
    }
    return Brush.verticalGradient(
        *stops.toTypedArray(),
        startY = y0, endY = y1,
        tileMode = if (repeat) androidx.compose.ui.graphics.TileMode.Repeated else androidx.compose.ui.graphics.TileMode.Clamp
    )
}

/** The whole avatar at [t] seconds. [background] false draws the figure alone. */
fun DrawScope.drawAvatar(look: AvatarLook, t: Float, pose: AvatarPose = AvatarPose(), background: Boolean = true) {
    inArtUnits {
        if (background) {
            clipRect(0f, 0f, ART_UNITS, ART_UNITS) { avatarUnits(look, t, pose, true) }
        } else {
            avatarUnits(look, t, pose, false)
        }
    }
}

private fun DrawScope.avatarUnits(look: AvatarLook, t: Float, pose: AvatarPose, background: Boolean) {
    val skin = skinPaint(look[AvatarSlot.Skin])
    val hair = hairPaint(look[AvatarSlot.HairColor])
    val holding = look[AvatarSlot.Held] != null
    val breath = wave(t, 3.6f)

    if (background) drawAvatarBackground(look[AvatarSlot.Background], t)
    drawAura(look[AvatarSlot.Aura], t, front = false)

    // Ground shadow.
    val lift = (pose.hop / 50f).coerceIn(0f, 1f)
    ovalAt(Color.Black, 100f, Rig.GROUND + 3f, 34f * (1f - lift * 0.4f), 5f, alpha = 0.13f * (1f - lift * 0.5f))

    translate(0f, -pose.hop) {
        drawBack(look[AvatarSlot.Back], t)
        translate(0f, breath * 0.7f) {
            drawHairBack(look[AvatarSlot.Hair], hair, t)
        }
        drawLegs(look[AvatarSlot.Top])
        drawArmsAndTop(look[AvatarSlot.Top], skin, holding, t)
        translate(0f, breath * 0.7f) {
            drawHeadBase(skin, look[AvatarSlot.Skin], t)
            drawFace(look, skin, t, pose)
            drawFaceItem(look[AvatarSlot.Face], t)
            drawHairFront(look[AvatarSlot.Hair], hair, t, hatOn = look[AvatarSlot.Hat] != null)
            drawHat(look[AvatarSlot.Hat], t)
        }
        if (holding) {
            drawHeld(look[AvatarSlot.Held], t)
            drawCircle(skin.base, radius = 7.5f, center = Rig.holdR)
            drawCircle(skin.shade, radius = 7.5f, center = Rig.holdR, style = Stroke(1.2f), alpha = 0.5f)
        }
    }
    drawAura(look[AvatarSlot.Aura], t, front = true)
}

// -- body -------------------------------------------------------------------

private fun DrawScope.drawLegs(topId: String?) {
    val trouser = when (topId) {
        "top_overalls" -> Color(0xFF3E6FB8)
        "top_tux" -> Color(0xFF23232E)
        "top_space" -> Color(0xFFE9EEF5)
        "top_royal" -> Color(0xFF6C1F3D)
        else -> Color(0xFF3D4A66)
    }
    listOf(91f, 109f).forEach { x ->
        drawRoundRect(
            trouser, topLeft = Offset(x - 8f, 166f), size = androidx.compose.ui.geometry.Size(16f, 22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
    }
    // Sneakers.
    listOf(89f, 111f).forEach { x ->
        val dir = if (x < 100f) -1f else 1f
        ovalAt(Color(0xFFF7F4F0), x + dir * 2f, Rig.GROUND - 2f, 11f, 6.5f)
        ovalAt(Color(0xFFFF5A1F), x + dir * 2f, Rig.GROUND + 1.5f, 11f, 3f)
        ovalAt(Color.White, x + dir * 2f - 3f, Rig.GROUND - 5f, 4f, 1.6f, alpha = 0.9f)
    }
}

/** Arms are thick rounded strokes; the outfit paints sleeves and torso over them. */
private fun DrawScope.drawArmsAndTop(topId: String?, skin: SkinPaint, holding: Boolean, t: Float) {
    val rightHand = if (holding) Rig.holdR else Rig.handR
    val sway = wave(t, 3.6f) * 1.2f
    val leftHand = Rig.handL + Offset(0f, sway)
    val style = topStyle(topId)
    // The neck sits under the collar.
    drawRoundRect(
        skin.shade, topLeft = Offset(93f, 106f), size = androidx.compose.ui.geometry.Size(14f, 16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
    )
    // Bare forearms first, then sleeves on top.
    listOf(Rig.shoulderL to leftHand, Rig.shoulderR to rightHand).forEach { (s, h) ->
        drawLine(skin.base, s, h, strokeWidth = 13f, cap = StrokeCap.Round)
        val sleeveEnd = Offset(mix(s.x, h.x, style.sleeve), mix(s.y, h.y, style.sleeve))
        drawLine(style.sleeveColor, s, sleeveEnd, strokeWidth = 15f, cap = StrokeCap.Round)
    }
    drawTop(topId, t)
    drawCircle(skin.base, radius = 7.5f, center = leftHand)
    if (!holding) drawCircle(skin.base, radius = 7.5f, center = rightHand)
}

private fun DrawScope.drawHeadBase(skin: SkinPaint, skinId: String?, t: Float) {
    // Ears.
    listOf(-1f, 1f).forEach { side ->
        flatOval(skin.base, skin.shade, 100f + side * 43f, 80f, 8f, 10f, dx = side * 2f, dy = -1f)
        ovalAt(skin.shade, 100f + side * 43.5f, 80f, 3.5f, 5f)
    }
    val head = headPath()
    flat(head, skin.base, skin.shade, dx = -6f, dy = -5f)
    if (skinId == "skin_galaxy") {
        clipPath(head) { drawStarfield(t, 40f, 30f, 120f, 90f, seed = 3, count = 14) }
    }
}

/** A soft squircle: wider at the cheeks than a circle, like a cartoon face. */
fun headPath(): Path = svg(
    "M100 33 C131 33 144 49 144 76 C144 103 126 117 100 117 C74 117 56 103 56 76 C56 49 69 33 100 33 Z"
)

private fun DrawScope.drawFace(look: AvatarLook, skin: SkinPaint, t: Float, pose: AvatarPose) {
    // Blush.
    ovalAt(skin.blush, 100f - 25f, 92f, 7.5f, 4.5f, alpha = 0.32f + pose.joy * 0.2f)
    ovalAt(skin.blush, 100f + 25f, 92f, 7.5f, 4.5f, alpha = 0.32f + pose.joy * 0.2f)
    // A hint of nose.
    drawArc(
        skin.shade, startAngle = 20f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(96f, 84f), size = androidx.compose.ui.geometry.Size(8f, 6f),
        style = Stroke(2.2f, cap = StrokeCap.Round)
    )
    drawEyes(look[AvatarSlot.Eyes], t, pose)
    drawBrows(look[AvatarSlot.Eyes], hairPaint(look[AvatarSlot.HairColor]), look[AvatarSlot.Hair] == "hair_bald")
    drawMouth(look[AvatarSlot.Mouth], t)
}

/** Thick, expressive brows in the hair colour, the cartoon way. */
private fun DrawScope.drawBrows(eyesId: String?, hair: HairPaint, bald: Boolean) {
    val color = if (bald) Color(0xFF5A3A28) else hair.shade
    listOf(-1f, 1f).forEach { side ->
        val cx = 100f + side * Rig.EYE_DX
        val tilt = when (eyesId) {
            "eyes_determined" -> 4f
            "eyes_sleepy" -> -1f
            "eyes_star", "eyes_heart", "eyes_anime" -> -3f
            else -> 0f
        }
        val inner = Offset(cx - side * 7f, 66f + tilt)
        val outer = Offset(cx + side * 7f, 66f - tilt * 0.4f)
        val brow = Path().apply {
            moveTo(inner.x, inner.y)
            quadraticBezierTo(cx, 62.5f, outer.x, outer.y)
        }
        drawPath(brow, color, style = Stroke(4.2f, cap = StrokeCap.Round))
    }
}

// -- eyes ---------------------------------------------------------------------

private val eyeInk = Color(0xFF2A1C14)

private fun avatarBlink(t: Float): Float {
    val slot = floor(t / 4.1f).toInt()
    val start = slot * 4.1f + hash01(slot * 13 + 9) * 2.6f
    return bump(t, start, 0.16f)
}

private fun DrawScope.drawEyes(id: String?, t: Float, pose: AvatarPose) {
    val blink = avatarBlink(t)
    val joy = pose.joy
    listOf(-1f, 1f).forEach { side ->
        val cx = 100f + side * Rig.EYE_DX
        val cy = Rig.EYE_Y
        if (joy > 0.5f || id == "eyes_happy") {
            happyArc(cx, cy)
            return@forEach
        }
        when (id) {
            "eyes_sleepy" -> {
                drawArc(eyeInk, 0f, 180f, true, Offset(cx - 7f, cy - 3f), androidx.compose.ui.geometry.Size(14f, 9f))
                drawLine(eyeInk, Offset(cx - 8f, cy + 1f), Offset(cx + 8f, cy + 1f), 2.6f, StrokeCap.Round)
            }
            "eyes_wink" -> if (side > 0) happyArc(cx, cy) else roundEye(cx, cy, blink)
            "eyes_determined" -> roundEye(cx, cy, blink, scale = 0.92f)
            "eyes_lashes" -> {
                roundEye(cx, cy, blink)
                if (blink < 0.5f) {
                    listOf(-1f, 0f).forEach { k ->
                        val base = Offset(cx + side * (7f + k * -3f), cy - 7f + k * 1.5f)
                        drawLine(eyeInk, base, base + Offset(side * 4f, -4f), 2.2f, StrokeCap.Round)
                    }
                }
            }
            "eyes_star" -> {
                val pulse = 1f + wave(t, 1.1f) * 0.08f
                drawPath(starPath(cx, cy, 9.5f * pulse, rotation = -90f + wave(t, 3f) * 6f), Color(0xFFFFC21A))
                drawPath(starPath(cx, cy, 9.5f * pulse), Color(0xFFE08A00), style = Stroke(1.4f))
                drawCircle(Color.White, 2f, Offset(cx + 2.5f, cy - 3f), alpha = 0.9f)
            }
            "eyes_heart" -> {
                val pulse = 1f + max(0f, wave(t, 0.9f)) * 0.14f
                drawPath(heartPath(cx, cy, 10f * pulse), Color(0xFFFF3E6C))
                drawCircle(Color.White, 2f, Offset(cx - 3f, cy - 3f), alpha = 0.9f)
            }
            "eyes_anime" -> animeEye(cx, cy, blink)
            else -> roundEye(cx, cy, blink)
        }
    }
}

private fun DrawScope.happyArc(cx: Float, cy: Float) {
    val p = Path().apply {
        moveTo(cx - 7.5f, cy + 2f)
        quadraticBezierTo(cx, cy - 8f, cx + 7.5f, cy + 2f)
    }
    drawPath(p, eyeInk, style = Stroke(3.4f, cap = StrokeCap.Round))
}

private fun DrawScope.roundEye(cx: Float, cy: Float, blink: Float, scale: Float = 1f) {
    val open = 1f - blink
    if (open < 0.2f) {
        drawLine(eyeInk, Offset(cx - 6f, cy + 1f), Offset(cx + 6f, cy + 1f), 2.8f, StrokeCap.Round)
        return
    }
    ovalAt(eyeInk, cx, cy, 5.6f * scale, 7f * scale * open)
    drawCircle(Color.White, 2.1f * scale, Offset(cx + 1.8f, cy - 2.6f * open), alpha = 0.95f)
}

private fun DrawScope.animeEye(cx: Float, cy: Float, blink: Float) {
    val open = 1f - blink
    if (open < 0.2f) {
        happyArc(cx, cy)
        return
    }
    ovalAt(Color.White, cx, cy, 8.5f, 10.5f * open)
    ovalAt(Color(0xFF7B4DFF), cx, cy, 6.5f, 9f * open)
    ovalAt(Color(0xFF3B1F8F), cx, cy - 2f * open, 6.5f, 6f * open)
    drawCircle(Color.White, 3f, Offset(cx + 2.4f, cy - 4f * open))
    drawCircle(Color.White, 1.6f, Offset(cx - 2.6f, cy + 3.6f * open))
}

// -- mouths -------------------------------------------------------------------

private fun DrawScope.drawMouth(id: String?, t: Float) {
    val y = Rig.MOUTH_Y
    val lip = Color(0xFF7A2E25)
    when (id) {
        "mouth_grin" -> {
            val p = Path().apply {
                moveTo(89f, y - 2f); quadraticBezierTo(100f, y + 1f, 111f, y - 2f)
                quadraticBezierTo(108f, y + 10f, 100f, y + 10f); quadraticBezierTo(92f, y + 10f, 89f, y - 2f); close()
            }
            drawPath(p, lip)
            clipPath(p) { drawRect(Color.White, Offset(86f, y - 4f), androidx.compose.ui.geometry.Size(28f, 5f)) }
        }
        "mouth_small" -> drawLine(lip, Offset(96f, y), Offset(104f, y), 2.6f, StrokeCap.Round)
        "mouth_open" -> {
            val h = 7f + wave(t, 1.6f) * 0.8f
            ovalAt(lip, 100f, y + 2f, 6.5f, h)
            clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(93.5f, y + 2f - h, 106.5f, y + 2f + h)) }) {
                ovalAt(Color(0xFFFF7088), 100f, y + 2f + h, 6f, 4.5f)
            }
        }
        "mouth_tongue" -> {
            smileLine(lip, y)
            val wag = wave(t, 1.4f) * 0.8f
            drawRoundRect(
                Color(0xFFFF6F86), topLeft = Offset(101f + wag, y + 1f), size = androidx.compose.ui.geometry.Size(8f, 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawLine(Color(0xFFE0506A), Offset(105f + wag, y + 2f), Offset(105f + wag, y + 6.5f), 1.2f, StrokeCap.Round)
        }
        "mouth_cat" -> {
            val p = Path().apply {
                moveTo(91f, y - 1f); quadraticBezierTo(95.5f, y + 4f, 100f, y)
                quadraticBezierTo(104.5f, y + 4f, 109f, y - 1f)
            }
            drawPath(p, lip, style = Stroke(2.6f, cap = StrokeCap.Round))
        }
        "mouth_fangs" -> {
            smileLine(lip, y)
            listOf(94f, 106f).forEach { x ->
                val f = Path().apply { moveTo(x - 2.4f, y + 1.5f); lineTo(x + 2.4f, y + 1.5f); lineTo(x, y + 7f); close() }
                drawPath(f, Color.White)
            }
        }
        else -> smileLine(lip, y)
    }
}

private fun DrawScope.smileLine(color: Color, y: Float) {
    val p = Path().apply { moveTo(91f, y - 1f); quadraticBezierTo(100f, y + 7f, 109f, y - 1f) }
    drawPath(p, color, style = Stroke(2.8f, cap = StrokeCap.Round))
}

// -- shared bits --------------------------------------------------------------

/** Twinkling stars scattered in a rectangle. */
fun DrawScope.drawStarfield(t: Float, x: Float, y: Float, w: Float, h: Float, seed: Int, count: Int, color: Color = Color.White) {
    for (i in 0 until count) {
        val px = x + hash01(seed * 97 + i * 3) * w
        val py = y + hash01(seed * 89 + i * 7 + 1) * h
        val r = 0.8f + hash01(seed * 53 + i) * 2.2f
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * wave(t, 1.2f + hash01(i * 11 + seed) * 1.8f, hash01(i * 5)))
        if (r > 2.2f) {
            drawPath(sparklePath(px, py, r * 1.8f * tw), color, alpha = tw)
        } else {
            drawCircle(color, r * 0.7f, Offset(px, py), alpha = tw)
        }
    }
}

/** Helper for scaling a piece about a point. */
inline fun DrawScope.scaledAbout(s: Float, cx: Float, cy: Float, block: DrawScope.() -> Unit) {
    scale(s, s, pivot = Offset(cx, cy)) { block() }
}
