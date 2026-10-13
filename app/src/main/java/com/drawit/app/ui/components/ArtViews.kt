package com.drawit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.drawit.app.model.FlameTier
import com.drawit.app.ui.art.AvatarLook
import com.drawit.app.ui.art.AvatarPose
import com.drawit.app.ui.art.FlameLook
import com.drawit.app.ui.art.FoxyPose
import com.drawit.app.ui.art.drawAvatar
import com.drawit.app.ui.art.drawChest
import com.drawit.app.ui.art.drawGoalMedal
import com.drawit.app.ui.art.drawFlame
import com.drawit.app.ui.art.drawFoxy
import com.drawit.app.ui.art.drawInkDrop

/*
 * One clock drives every drawn character and flame in the app. Views read it
 * inside their draw lambda, so animation only redraws; nothing recomposes,
 * which is what keeps Foxy smooth.
 */

private val zeroClock: State<Float> = mutableFloatStateOf(0f)

val LocalArtClock = staticCompositionLocalOf { zeroClock }

@Composable
fun ProvideArtClock(content: @Composable () -> Unit) {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> clock.floatValue = (now - start) / 1_000_000_000f }
        }
    }
    CompositionLocalProvider(LocalArtClock provides clock, content = content)
}

/** Foxy, animated. [pose] is read every frame, so it can follow animated values without recomposing. */
@Composable
fun FoxyView(modifier: Modifier = Modifier, fit: Float = 1f, shadow: Boolean = true, pose: () -> FoxyPose = { FoxyPose() }) {
    val clock = LocalArtClock.current
    Canvas(modifier) { drawFoxy(pose(), clock.value, shadow = shadow, fit = fit) }
}

@Composable
fun AvatarView(
    look: AvatarLook,
    modifier: Modifier = Modifier,
    background: Boolean = true,
    animate: Boolean = true,
    pose: () -> AvatarPose = { AvatarPose() }
) {
    val clock = LocalArtClock.current
    Canvas(modifier) { drawAvatar(look, if (animate) clock.value else 0.7f, pose(), background) }
}

@Composable
fun FlameView(
    tier: FlameTier,
    modifier: Modifier = Modifier,
    lit: Boolean = true,
    detail: Boolean = true,
    face: Boolean = false,
    joy: () -> Float = { 0f },
    intensity: () -> Float = { 1f }
) {
    val clock = LocalArtClock.current
    Canvas(modifier) {
        drawFlame(FlameLook(tier, lit = lit, detail = detail, face = face, joy = joy(), intensity = intensity()), clock.value)
    }
}

@Composable
fun InkDrop(modifier: Modifier = Modifier, animate: Boolean = true, shine: () -> Float = { 0f }) {
    val clock = LocalArtClock.current
    Canvas(modifier) { drawInkDrop(if (animate) clock.value else 0.4f, shine()) }
}

@Composable
fun ChestView(
    modifier: Modifier = Modifier,
    open: () -> Float,
    shake: () -> Float,
    glow: () -> Float = { 0f },
    leak: () -> Float = { 0f }
) {
    val clock = LocalArtClock.current
    Canvas(modifier) { drawChest(open(), shake(), clock.value, glow(), leak()) }
}

/** A goal medal; with [emblem] off the face is left plain for a number on top. */
@Composable
fun MedalView(goal: Int, done: Boolean, modifier: Modifier = Modifier, emblem: Boolean = true, shine: () -> Float = { 0f }) {
    val clock = LocalArtClock.current
    Canvas(modifier) { drawGoalMedal(goal, done, clock.value, emblem, shine()) }
}
