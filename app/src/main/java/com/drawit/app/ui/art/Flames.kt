package com.drawit.app.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.drawit.app.model.FlameTier
import kotlin.math.max

/*
 * The streak flame, one look per tier. Flat colour with a hard shadow like
 * everything else; every tier flickers, and the higher ones bring their own
 * flourishes: embers, sparks, stars, sun rays, wings, a shifting prism.
 */

class FlamePalette(val outer: Color, val outerShade: Color, val mid: Color, val core: Color, val accent: Color)

fun flamePalette(tier: FlameTier, t: Float = 0f): FlamePalette = when (tier) {
    FlameTier.Ember -> FlamePalette(Color(0xFFC9C0B8), Color(0xFFB0A69D), Color(0xFFDDD6CF), Color(0xFFEEE9E4), Color(0xFFB0A69D))
    FlameTier.Spark -> FlamePalette(Color(0xFFFF9600), Color(0xFFE67A00), Color(0xFFFFC200), Color(0xFFFFE680), Color(0xFFFFC200))
    FlameTier.Blaze -> FlamePalette(Color(0xFFFF6A00), Color(0xFFE25300), Color(0xFFFF9C00), Color(0xFFFFD84D), Color(0xFFFFC200))
    FlameTier.Inferno -> FlamePalette(Color(0xFFFF3D1F), Color(0xFFD92A10), Color(0xFFFF8A00), Color(0xFFFFE14D), Color(0xFFFFC200))
    FlameTier.Azure -> FlamePalette(Color(0xFF1CA8F6), Color(0xFF128AD1), Color(0xFF6FD9FF), Color(0xFFE6FAFF), Color(0xFF9FEBFF))
    FlameTier.Cosmic -> FlamePalette(Color(0xFF9150FF), Color(0xFF7234E0), Color(0xFFD27BFF), Color(0xFFFFE6FF), Color(0xFFFFE680))
    FlameTier.Solar -> FlamePalette(Color(0xFFFFB300), Color(0xFFE89400), Color(0xFFFFD84D), Color(0xFFFFFFFF), Color(0xFFFFF0A8))
    FlameTier.Phoenix -> FlamePalette(Color(0xFFFF4B1F), Color(0xFFD9320F), Color(0xFFFF9C00), Color(0xFFFFE14D), Color(0xFFFFC200))
    FlameTier.Eternal -> {
        val h = (t * 40f) % 360f
        FlamePalette(hsv(h, 0.72f, 1f), hsv(h, 0.8f, 0.86f), hsv(h + 40f, 0.5f, 1f), hsv(h + 80f, 0.15f, 1f), hsv(h + 180f, 0.6f, 1f))
    }
}

data class FlameLook(
    val tier: FlameTier,
    /** False draws the tier greyed out: today hasn't been drawn yet. */
    val lit: Boolean = true,
    /** Small sizes skip the flourishes so the icon stays readable. */
    val detail: Boolean = true,
    /** Big white eyes, for celebrations. */
    val face: Boolean = false,
    /** 0..1 happy-eyes when the face is shown. */
    val joy: Float = 0f,
    /** Extra flicker and size during the "ignite" beat of a celebration. */
    val intensity: Float = 1f
)

fun DrawScope.drawFlame(look: FlameLook, t: Float) {
    inArtUnits { flameUnits(look, t) }
}

private fun DrawScope.flameUnits(look: FlameLook, t: Float) {
    val tier = look.tier
    val pal = if (look.lit) flamePalette(tier, t) else flamePalette(FlameTier.Ember)
    val energy = look.intensity
    val rich = look.detail && look.lit

    // Behind the flame.
    if (rich) when (tier) {
        FlameTier.Solar -> sunRays(t, pal.accent)
        FlameTier.Phoenix -> flameWings(t, pal)
        FlameTier.Eternal -> prismRing(t)
        else -> Unit
    }

    val size = 0.86f + tierScale(tier) * 0.14f
    scale(size * (0.92f + energy * 0.08f), size * (0.92f + energy * 0.08f), pivot = Offset(100f, 184f)) {
        val flicker = if (look.lit) energy else 0.3f
        // From Blaze on the fire grows two little flames at its feet.
        if (look.lit) when (tier) {
            FlameTier.Blaze -> sideFlames(t, flicker, pal, 0.42f)
            FlameTier.Inferno -> sideFlames(t, flicker, pal, 0.52f)
            else -> Unit
        }
        val outer = flamePath(t, flicker, seed = 1)
        flat(outer, pal.outer, pal.outerShade, dx = -9f, dy = -4f)
        scale(0.7f, 0.68f, pivot = Offset(104f, 182f)) {
            flat(flamePath(t + 0.37f, flicker, seed = 2), pal.mid, lerp(pal.mid, pal.outer, 0.35f), dx = -10f, dy = -4f)
        }
        scale(0.4f, 0.38f, pivot = Offset(106f, 180f)) {
            drawPath(flamePath(t + 0.71f, flicker, seed = 3), pal.core)
        }
        if (look.lit) {
            // The shine: a light stroke up the left flank and a dot on the side tongue.
            val shine = pal.outer.lighten(0.5f)
            val sway = wave(t, 0.71f, hash01(5)) * 2f * flicker
            clipPath(outer) {
                drawPath(
                    Path().apply { moveTo(52f, 150f); cubicTo(46f, 130f, 50f, 110f, 60f + sway, 94f) },
                    shine, style = Stroke(7f, cap = StrokeCap.Round)
                )
                drawCircle(shine, 4.5f, Offset(69f + sway, 76f))
            }
            // And a glint on the core, where the face would otherwise sit.
            if (!look.face) drawPath(sparklePath(96f, 150f, 6f), Color.White, alpha = 0.85f)
        }
        if (tier == FlameTier.Cosmic && rich) {
            clipPath(outer) { drawStarfield(t, 40f, 20f, 120f, 160f, seed = 5, count = 10) }
        }
        if (look.face) flameFace(look.joy, t)
    }

    if (rich) when (tier) {
        FlameTier.Blaze -> embers(t, pal.accent, 4)
        FlameTier.Inferno, FlameTier.Phoenix -> embers(t, pal.accent, 7)
        FlameTier.Azure -> embers(t, pal.accent, 6, sparkle = true)
        FlameTier.Cosmic -> orbitStars(t, pal.accent)
        FlameTier.Eternal -> embers(t, Color.White, 8, sparkle = true)
        else -> Unit
    }
    if (!look.lit && look.detail) smoke(t)
}

/** A small flame either side of the big one, each leaning outwards. */
private fun DrawScope.sideFlames(t: Float, energy: Float, pal: FlamePalette, size: Float) {
    listOf(-1f, 1f).forEach { s ->
        val sc = if (s < 0f) size else size * 0.86f
        translate(s * 50f, 2f) {
            scale(s * sc, sc, pivot = Offset(100f, 182f)) {
                val p = flamePath(t * 1.25f + s * 0.31f, energy, seed = if (s < 0f) 4 else 6)
                // Mirroring flips the shadow too, so flip its offset back.
                flat(p, pal.outer, pal.outerShade, dx = -9f * s, dy = -2f)
                scale(0.62f, 0.6f, pivot = Offset(104f, 182f)) {
                    drawPath(flamePath(t * 1.25f + 0.5f, energy, seed = if (s < 0f) 5 else 7), pal.mid)
                }
            }
        }
    }
}

/** Higher tiers draw a touch bigger. */
private fun tierScale(tier: FlameTier): Float = (tier.ordinal / (FlameTier.entries.size - 1f))

/**
 * The flame silhouette: a round base, a main tip leaning right and a smaller
 * tongue on the left. Both tips wander on their own clocks.
 */
private fun flamePath(t: Float, energy: Float, seed: Int): Path {
    val e = 0.6f + energy * 0.6f
    val tipX = 116f + wave(t, 0.9f, hash01(seed)) * 6f * e
    val tipY = 18f + wave(t, 0.47f, hash01(seed * 3)) * 6f * e
    val sideX = 70f + wave(t, 0.71f, hash01(seed * 5)) * 4f * e
    val sideY = 54f + wave(t, 0.53f, hash01(seed * 7)) * 7f * e
    val bulge = wave(t, 1.3f, hash01(seed * 11)) * 2f * e
    return Path().apply {
        moveTo(100f, 182f)
        cubicTo(64f - bulge, 182f, 42f - bulge, 158f, 42f - bulge, 126f)
        cubicTo(42f, 98f, 60f, 80f, sideX, sideY)
        cubicTo(sideX + 8f, 66f, 82f, 78f, 84f, 88f)
        cubicTo(92f, 68f, 102f, 42f, tipX, tipY)
        cubicTo(tipX + 4f, 46f, 140f, 62f, 152f, 84f)
        cubicTo(160f + bulge, 98f, 160f + bulge, 112f, 158f + bulge, 126f)
        cubicTo(158f + bulge, 158f, 136f, 182f, 100f, 182f)
        close()
    }
}

private fun DrawScope.flameFace(joy: Float, t: Float) {
    val y = 136f
    listOf(84f, 116f).forEach { x ->
        if (joy < 0.5f) {
            ovalAt(Color.White, x, y, 11f, 12.5f)
            val gx = wander(t, 2.8f, 4) * 3f
            drawCircle(FoxyPalette.eye, 6.5f, Offset(x + gx, y + 1f))
            drawCircle(Color.White, 2.2f, Offset(x + gx + 2.4f, y - 2f))
        } else {
            val arc = Path().apply { moveTo(x - 9f, y + 4f); quadraticBezierTo(x, y - 10f, x + 9f, y + 4f) }
            drawPath(arc, FoxyPalette.eye, style = Stroke(5.5f, cap = StrokeCap.Round))
        }
    }
    val m = Path().apply {
        moveTo(90f, 154f)
        lineTo(110f, 154f)
        cubicTo(110f, 166f + joy * 4f, 90f, 166f + joy * 4f, 90f, 154f)
        close()
    }
    drawPath(m, FoxyPalette.mouth)
    clipPath(m) { ovalAt(FoxyPalette.tongue, 100f, 166f + joy * 3f, 7f, 5f) }
}

// -- flourishes -------------------------------------------------------------------

private fun DrawScope.embers(t: Float, color: Color, count: Int, sparkle: Boolean = false) {
    for (i in 0 until count) {
        val speed = 0.35f + hash01(i * 13) * 0.3f
        val life = ((t * speed + hash01(i * 7)) % 1f)
        val x = 60f + hash01(i * 3) * 80f + wave(t, 1.4f, hash01(i)) * 10f
        val y = 150f - life * 150f
        val a = (1f - life)
        if (sparkle) drawPath(sparklePath(x, y, 6f * a + 1f), color, alpha = a)
        else drawCircle(color, 2f + 3.5f * a, Offset(x, y), alpha = a)
    }
}

private fun DrawScope.orbitStars(t: Float, color: Color) {
    for (i in 0..2) {
        val p = polar(100f, 110f, 86f, t * 50f + i * 120f)
        drawPath(starPath(p.x, p.y, 8f, rotation = t * 90f), color)
    }
}

private fun DrawScope.sunRays(t: Float, color: Color) {
    rotate(t * 20f, pivot = Offset(100f, 110f)) {
        for (i in 0..11) {
            val a = i * 30f
            val p1 = polar(100f, 110f, 98f, a - 6f)
            val p2 = polar(100f, 110f, 98f, a + 6f)
            drawPath(Path().apply { moveTo(100f, 110f); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }, color, alpha = 0.85f)
        }
    }
}

private fun DrawScope.flameWings(t: Float, pal: FlamePalette) {
    val beat = wave(t, 1.4f) * 6f
    listOf(-1f, 1f).forEach { s ->
        rotate(s * beat, pivot = Offset(100f + s * 30f, 130f)) {
            for (k in 0..3) {
                val base = Offset(100f + s * (34f + k * 4f), 140f - k * 6f)
                val wing = Path().apply {
                    moveTo(base.x, base.y + 10f)
                    quadraticBezierTo(base.x + s * 40f, base.y - 10f - k * 8f, base.x + s * (60f - k * 6f), base.y - 46f - k * 10f)
                    quadraticBezierTo(base.x + s * 30f, base.y - 14f, base.x, base.y - 6f)
                    close()
                }
                drawPath(wing, if (k % 2 == 0) pal.outer else pal.mid)
            }
        }
    }
}

private fun DrawScope.prismRing(t: Float) {
    for (i in 0 until 12) {
        val a = t * 30f + i * 30f
        val p = polar(100f, 112f, 90f, a)
        drawCircle(hsv(a * 2f, 0.7f, 1f), 7f, p)
    }
}

private fun DrawScope.smoke(t: Float) {
    for (i in 0..2) {
        val life = ((t * 0.25f + i / 3f) % 1f)
        val x = 112f + wave(t, 2.2f, i * 0.3f) * 8f
        val y = 24f - life * 30f
        drawCircle(Color(0xFFC9C0B8), 5f + life * 8f, Offset(x, y + 20f), alpha = (1f - life) * 0.6f)
    }
}

// -- the currency ------------------------------------------------------------------

/**
 * Ink: a fat, faceted drop. [shine] 0..1 sweeps a glint across it, used when
 * ink is earned.
 */
fun DrawScope.drawInkDrop(t: Float, shine: Float = 0f) {
    inArtUnits {
        val drop = svg("M100 14 C112 40 162 88 162 128 C162 164 134 190 100 190 C66 190 38 164 38 128 C38 88 88 40 100 14 Z")
        flat(drop, Color(0xFF4F7BFF), Color(0xFF3557DB), dx = -10f, dy = -6f)
        // The facet: a lighter wedge on the left.
        clipPath(drop) {
            drawPath(svg("M100 14 C92 40 60 84 56 120 C54 140 60 160 72 172 C52 160 40 144 38 126 C38 88 88 40 100 14 Z"), Color(0xFF7FA0FF))
            if (shine > 0f) {
                val x = -40f + shine * 260f
                drawPath(Path().apply { moveTo(x, 200f); lineTo(x + 26f, 200f); lineTo(x + 86f, 0f); lineTo(x + 60f, 0f); close() }, Color.White, alpha = 0.55f)
            }
        }
        // One curved highlight hugging the left side.
        drawPath(
            Path().apply {
                moveTo(66f, 104f)
                quadraticBezierTo(58f, 128f, 70f, 154f)
            },
            Color.White, alpha = 0.9f, style = Stroke(11f, cap = StrokeCap.Round)
        )
        val tw = 0.5f + 0.5f * wave(t, 1.7f)
        drawPath(sparklePath(140f, 70f, 14f * tw), Color.White, alpha = tw)
    }
}
