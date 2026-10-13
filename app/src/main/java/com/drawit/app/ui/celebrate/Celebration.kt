package com.drawit.app.ui.celebrate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drawit.app.data.Celebration
import com.drawit.app.feedback.Feedback
import com.drawit.app.feedback.Feedback.Buzz
import com.drawit.app.feedback.Feedback.Sound
import com.drawit.app.model.Economy
import com.drawit.app.model.FlameTier
import com.drawit.app.model.RewardKind
import com.drawit.app.ui.art.AvatarItem
import com.drawit.app.ui.art.AvatarLook
import com.drawit.app.ui.art.FoxyPose
import com.drawit.app.ui.art.flamePalette
import com.drawit.app.ui.components.AvatarView
import com.drawit.app.ui.components.ChestView
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyProgress
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Confetti
import com.drawit.app.ui.components.FlameView
import com.drawit.app.ui.components.FoxyView
import com.drawit.app.ui.components.InkDrop
import com.drawit.app.ui.components.LocalArtClock
import com.drawit.app.ui.components.LocalFeedback
import com.drawit.app.ui.components.MedalView
import com.drawit.app.ui.components.RollingNumber
import com.drawit.app.ui.components.SpeechBubble
import com.drawit.app.ui.components.WeekStrip
import com.drawit.app.ui.theme.DI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The moment after a drawing is saved. It plays as a few stages, each with
 * its own beats timed against sound and haptics:
 *
 *   Streak  the flame ignites, the number rolls, the week fills in
 *   Evolve  the flame shakes, flashes and comes back as the next tier
 *   Goal    a chest rattles, bursts open and pours out ink
 *   Rewards every ink line ticks in, and any unlocked avatar pieces appear
 *
 * Extra drawings on a day that already counted get a short "saved" stage
 * instead of the streak one. Foxy is in every stage. A first tap skips to
 * the end of the current beat; the button moves on.
 */

private sealed interface Stage {
    data object Streak : Stage
    data object Saved : Stage
    data class Evolve(val tier: FlameTier) : Stage
    data class Goal(val goal: Int) : Stage
    data object Rewards : Stage
}

private fun stagesFor(c: Celebration): List<Stage> = buildList {
    add(if (c.extended) Stage.Streak else Stage.Saved)
    c.reward.evolvedTo?.let { add(Stage.Evolve(it)) }
    c.reward.goalReached?.let { add(Stage.Goal(it)) }
    if (c.reward.total > 0 || c.unlocked.isNotEmpty()) add(Stage.Rewards)
}

@Composable
fun CelebrationOverlay(
    celebration: Celebration,
    look: AvatarLook,
    drawn: Set<Long>,
    frozen: Set<Long>,
    onDone: () -> Unit
) {
    val stages = remember(celebration) { stagesFor(celebration) }
    var index by remember(celebration) { mutableIntStateOf(0) }
    val next: () -> Unit = { if (index < stages.lastIndex) index++ else onDone() }
    BackHandler { next() }

    val stage = stages[index]
    val bg = when (stage) {
        is Stage.Evolve -> Color(0xFF1B1027)
        is Stage.Goal -> DI.colors.goldSoft
        else -> DI.colors.background
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            // Swallow taps so nothing underneath reacts.
            .clickable(remember { MutableInteractionSource() }, null) {}
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it / 4 } + fadeIn(tween(220)))
                    .togetherWith(fadeOut(tween(160)) + scaleOut(tween(220), targetScale = 0.92f))
            },
            label = "stage"
        ) { s ->
            when (s) {
                Stage.Streak -> StreakStage(celebration, drawn, frozen, next)
                Stage.Saved -> SavedStage(celebration, next)
                is Stage.Evolve -> EvolveStage(s.tier, celebration, look, next)
                is Stage.Goal -> GoalStage(s.goal, next)
                Stage.Rewards -> RewardsStage(celebration, look, next, last = true)
            }
        }
    }
}

// -- shared scaffolding ---------------------------------------------------------------

/**
 * Runs [beats] once; a tap before it finishes cancels the beats and calls
 * [finish] to snap everything to its end state.
 */
@Composable
private fun rememberBeats(
    finish: suspend CoroutineScope.() -> Unit,
    beats: suspend CoroutineScope.() -> Unit
): Pair<Boolean, () -> Unit> {
    var skipped by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(skipped) {
        if (!skipped) beats() else finish()
        ready = true
    }
    // A skip enables the button straight away, so a quick second tap is never lost
    // while the animations are still snapping to their ends.
    return ready to { if (!ready) { skipped = true; ready = true } }
}

@Composable
private fun StageScaffold(
    ready: Boolean,
    onSkip: () -> Unit,
    button: String,
    onContinue: () -> Unit,
    dark: Boolean = false,
    buttonChunk: com.drawit.app.ui.components.Chunk = Chunks.brand(),
    content: @Composable BoxScope.() -> Unit
) {
    val buttonIn = remember { Animatable(0f) }
    LaunchedEffect(ready) { if (ready) buttonIn.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onSkip)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        content()
        ChunkyButton(
            text = button,
            onClick = onContinue,
            chunk = if (dark) Chunks.plain() else buttonChunk,
            enabled = ready,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .graphicsLayer {
                    alpha = buttonIn.value.coerceIn(0f, 1f)
                    translationY = (1f - buttonIn.value) * 60.dp.toPx()
                }
        )
    }
}

/** A jump with squash on take-off and landing. */
private suspend fun hop(hop: Animatable<Float, *>, squash: Animatable<Float, *>, height: Float = 30f) {
    squash.animateTo(0.8f, tween(90))
    kotlinx.coroutines.coroutineScope {
        launch { squash.animateTo(-0.7f, tween(120)); squash.animateTo(0f, tween(160)) }
        hop.animateTo(height, tween(230, easing = FastOutSlowInEasing))
    }
    hop.animateTo(0f, tween(220, easing = androidx.compose.animation.core.CubicBezierEasing(0.5f, 0f, 1f, 1f)))
    squash.animateTo(1f, tween(70))
    squash.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
}

private fun Feedback?.beat(sound: Sound?, buzz: Buzz?, semitones: Float = 0f) {
    if (this == null) return
    sound?.let { play(it, semitones) }
    buzz?.let { buzz(it) }
}

private val pentatonic = floatArrayOf(0f, 2f, 4f, 7f, 9f, 12f, 14f)

// -- streak ------------------------------------------------------------------------

@Composable
private fun StreakStage(c: Celebration, drawn: Set<Long>, frozen: Set<Long>, onContinue: () -> Unit) {
    val feedback = LocalFeedback.current
    val before = c.streakBefore
    val after = c.outcome.state.current
    // If this drawing evolves the flame, the reveal belongs to the next stage.
    val tier = if (c.reward.evolvedTo != null) FlameTier.forStreak(before).coerceAtLeastSpark() else FlameTier.forStreak(after)
    val today = LocalDate.now()
    val todayIndex = today.dayOfWeek.value - DayOfWeek.MONDAY.value

    val goal = Economy.nextGoal(before) ?: after
    val prevGoal = Economy.previousGoal(before)
    val fracBefore = ((before - prevGoal).toFloat() / (goal - prevGoal).coerceAtLeast(1)).coerceIn(0f, 1f)
    val fracAfter = ((after - prevGoal).toFloat() / (goal - prevGoal).coerceAtLeast(1)).coerceIn(0f, 1f)

    val flameIn = remember { Animatable(0f) }
    var lit by remember { mutableStateOf(false) }
    val intensity = remember { Animatable(0.6f) }
    val pop = remember { Animatable(1f) }
    val rays = remember { Animatable(0f) }
    var count by remember { mutableIntStateOf(before) }
    val textIn = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    val goalProgress = remember { Animatable(fracBefore) }
    val confetti = remember { Animatable(0f) }
    val cheer = remember { Animatable(0f) }
    val hopA = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val foxIn = remember { Animatable(0f) }
    val bubbleIn = remember { Animatable(0f) }

    val (ready, skip) = rememberBeats(
        finish = {
            flameIn.snapTo(1f); lit = true; intensity.snapTo(1f); pop.snapTo(1f); rays.snapTo(1f)
            count = after; textIn.snapTo(1f); reveal.snapTo(todayIndex + 1f); goalProgress.snapTo(fracAfter)
            confetti.snapTo(1f); cheer.snapTo(1f); foxIn.snapTo(1f); bubbleIn.snapTo(1f); hopA.snapTo(0f); squash.snapTo(0f)
        }
    ) {
        feedback.beat(Sound.Whoosh, null)
        launch { flameIn.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
        launch { delay(150); foxIn.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) }
        delay(480)
        // Ignite.
        lit = true
        feedback.beat(Sound.Ignite, Buzz.Thud)
        launch { intensity.snapTo(1.9f); intensity.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)) }
        launch { pop.snapTo(1.3f); pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow)) }
        launch { rays.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { confetti.animateTo(1f, tween(2200, easing = LinearEasing)) }
        launch { cheer.animateTo(1f, spring(dampingRatio = 0.6f)) }
        launch { delay(80); hop(hopA, squash, 34f) }
        delay(260)
        // The number rolls over.
        count = after
        feedback.beat(Sound.Streak, Buzz.Success)
        launch { textIn.animateTo(1f, spring(dampingRatio = 0.7f)) }
        delay(500)
        // Each day of the week pops in, climbing the scale.
        for (i in 0..todayIndex) {
            reveal.animateTo(i + 1f, tween(110))
            val day = today.with(DayOfWeek.MONDAY).plusDays(i.toLong()).toEpochDay()
            if (day in drawn || day in frozen) feedback.beat(Sound.DayFill, Buzz.Tick, pentatonic[i])
        }
        launch { bubbleIn.animateTo(1f, spring(dampingRatio = 0.55f)) }
        launch { delay(120); hop(hopA, squash, 22f) }
        delay(200)
        goalProgress.animateTo(fracAfter, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
    }

    val palette = flamePalette(tier)
    val clock = LocalArtClock.current
    StageScaffold(ready, skip, "Continue", onContinue) {
        Confetti(progress = { confetti.value }, modifier = Modifier.fillMaxSize(), seed = after, count = if (c.isRecord) 90 else 55)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.4f))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                // Light rays burst out behind the flame as it catches.
                Canvas(Modifier.fillMaxSize()) {
                    val p = rays.value
                    if (p in 0.01f..0.99f) {
                        val r = size.minDimension / 2f
                        for (k in 0 until 12) {
                            val a = Math.toRadians(k * 30.0 + clock.value * 10)
                            val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                            drawLine(
                                palette.accent.copy(alpha = 1f - p),
                                center + dir * (r * (0.35f + 0.4f * p)), center + dir * (r * (0.5f + 0.5f * p)),
                                strokeWidth = 10.dp.toPx() * (1f - p * 0.6f),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                    drawCircle(palette.outer.copy(alpha = 0.12f * flameIn.value), size.minDimension * 0.46f)
                }
                FlameView(
                    tier = tier,
                    lit = lit,
                    face = true,
                    joy = { if (lit) 1f else 0f },
                    intensity = { intensity.value },
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer {
                            val s = flameIn.value * pop.value
                            scaleX = s
                            scaleY = s
                        }
                )
            }
            RollingNumber(
                count, DI.type.hero.copy(fontSize = DI.type.hero.fontSize * 1.25f),
                if (lit) DI.colors.flame else DI.colors.textFaint,
                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            )
            Text(
                if (after == 1) "day streak!" else "day streak!",
                style = DI.type.headline,
                color = DI.colors.flame,
                modifier = Modifier.graphicsLayer { alpha = textIn.value.coerceIn(0f, 1f) }
            )
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = textIn.value.coerceIn(0f, 1f)
                        translationY = (1f - textIn.value) * 24.dp.toPx()
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(DI.colors.surface)
                    .padding(16.dp)
            ) {
                Column {
                    WeekStrip(drawn = drawn, frozen = frozen, today = today, revealUpTo = { reveal.value })
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Day $after of your ${goal}-day goal", style = DI.type.small, color = DI.colors.textMuted, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    ChunkyProgress(progress = goalProgress.value, color = DI.colors.flame, animate = false)
                }
            }
            Spacer(Modifier.weight(1f))
        }
        FoxyCorner(
            text = foxyStreakLine(c),
            foxIn = { foxIn.value },
            bubbleIn = { bubbleIn.value },
            pose = { FoxyPose(cheer = cheer.value, hop = hopA.value, squash = squash.value, lookX = -0.6f, lookWeight = 0.7f) }
        )
    }
}

private fun FlameTier.coerceAtLeastSpark(): FlameTier = if (this == FlameTier.Ember) FlameTier.Spark else this

private fun foxyStreakLine(c: Celebration): String {
    val n = c.outcome.state.current
    return when {
        n == 1 && c.streakBefore == 0 && c.outcome.state.best > 1 -> "Back on it! Every streak starts with one."
        n == 1 -> "A brand new streak! Come back tomorrow to keep it lit."
        c.isRecord -> "That's your longest streak ever!"
        else -> listOf(
            "You're on fire!",
            "Look at that flame go!",
            "$n days. Unstoppable!",
            "Your sketchbook is proud of you.",
            "Same time tomorrow?",
            "That's how artists are made."
        )[n % 6]
    }
}

// -- saved (an extra drawing on a day that already counted) ------------------------

@Composable
private fun SavedStage(c: Celebration, onContinue: () -> Unit) {
    val feedback = LocalFeedback.current
    val foxIn = remember { Animatable(0f) }
    val cheer = remember { Animatable(0f) }
    val hopA = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    val (ready, skip) = rememberBeats(
        finish = { foxIn.snapTo(1f); cheer.snapTo(1f); check.snapTo(1f); confetti.snapTo(1f); hopA.snapTo(0f); squash.snapTo(0f) }
    ) {
        launch { foxIn.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
        delay(250)
        feedback.beat(Sound.Ding, Buzz.Success)
        launch { check.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch { cheer.animateTo(1f, spring(dampingRatio = 0.6f)) }
        launch { confetti.animateTo(1f, tween(1800, easing = LinearEasing)) }
        hop(hopA, squash, 36f)
        delay(120)
        hop(hopA, squash, 20f)
    }
    val extra = c.reward.total
    StageScaffold(ready, skip, if (extra > 0) "Continue" else "Done", onContinue) {
        Confetti(progress = { confetti.value }, modifier = Modifier.fillMaxSize(), seed = 7, count = 40)
        Column(
            Modifier.fillMaxSize().padding(24.dp).padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SpeechBubble(
                if (extra > 0) "Another one for the album! Your streak's already safe today."
                else "Saved! You've had today's ink for extra drawings, but every drawing counts.",
                modifier = Modifier.graphicsLayer { alpha = check.value.coerceIn(0f, 1f) }
            )
            FoxyView(
                Modifier.size(240.dp).graphicsLayer {
                    alpha = foxIn.value.coerceIn(0f, 1f)
                    translationY = (1f - foxIn.value) * 120.dp.toPx()
                },
                fit = 0.82f
            ) { FoxyPose(cheer = cheer.value, hop = hopA.value, squash = squash.value) }
            Spacer(Modifier.height(10.dp))
            CheckStamp(progress = { check.value })
            Spacer(Modifier.height(10.dp))
            Text("Saved to your album", style = DI.type.headline, color = DI.colors.text)
        }
    }
}

@Composable
private fun CheckStamp(progress: () -> Float) {
    val green = DI.colors.green
    Canvas(Modifier.size(72.dp)) {
        val p = progress()
        drawCircle(green, size.minDimension / 2f * (0.6f + 0.4f * p), alpha = p.coerceIn(0f, 1f))
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.68f)
            lineTo(size.width * 0.74f, size.height * 0.36f)
        }
        val measure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
        val seg = Path()
        measure.getSegment(0f, measure.length * ((p - 0.3f) / 0.7f).coerceIn(0f, 1f), seg, true)
        drawPath(seg, Color.White, style = Stroke(7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

// -- evolution ------------------------------------------------------------------------

@Composable
private fun EvolveStage(tier: FlameTier, c: Celebration, look: AvatarLook, onContinue: () -> Unit) {
    val feedback = LocalFeedback.current
    val from = FlameTier.entries[(tier.ordinal - 1).coerceAtLeast(1)]
    var shown by remember { mutableStateOf(from) }
    val charge = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val shock = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    val foxIn = remember { Animatable(0f) }
    val cheer = remember { Animatable(0f) }
    val surprise = remember { Animatable(0f) }
    val hopA = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val unlocks = c.unlocked.filter { it.unlockStreak == c.outcome.state.current || it.unlockStreak == tier.minStreak }

    val (ready, skip) = rememberBeats(
        finish = {
            shown = tier; charge.snapTo(0f); flash.snapTo(0f); shock.snapTo(1f); title.snapTo(1f); confetti.snapTo(1f)
            foxIn.snapTo(1f); cheer.snapTo(1f); surprise.snapTo(0f); hopA.snapTo(0f); squash.snapTo(0f)
        }
    ) {
        feedback.beat(Sound.EvolveRise, null)
        // Haptic ticks speed up as the charge builds.
        launch {
            var gap = 230L
            var elapsed = 0L
            while (elapsed < 1550) {
                feedback.beat(null, Buzz.Tick)
                delay(gap)
                elapsed += gap
                gap = (gap * 0.82f).toLong().coerceAtLeast(45L)
            }
        }
        charge.animateTo(1f, tween(1650, easing = LinearEasing))
        // Flash and reveal.
        flash.animateTo(1f, tween(70))
        shown = tier
        charge.snapTo(0f)
        feedback.beat(Sound.EvolveBoom, Buzz.Big)
        launch { flash.animateTo(0f, tween(650)) }
        launch { shock.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
        launch { confetti.animateTo(1f, tween(2600, easing = LinearEasing)) }
        launch { title.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        delay(650)
        launch { foxIn.animateTo(1f, spring(dampingRatio = 0.6f)) }
        surprise.animateTo(1f, tween(200))
        delay(350)
        launch { surprise.animateTo(0f, tween(300)) }
        launch { cheer.animateTo(1f, spring(dampingRatio = 0.6f)) }
        launch { hop(hopA, squash, 34f) }
        delay(300)
        if (unlocks.isNotEmpty()) feedback.beat(Sound.Unlock, Buzz.Pop)
    }

    val clock = LocalArtClock.current
    val palette = flamePalette(tier)
    StageScaffold(ready, skip, "Continue", onContinue, dark = true) {
        Confetti(
            progress = { confetti.value }, modifier = Modifier.fillMaxSize(), seed = tier.ordinal * 13, count = 90,
            colors = listOf(palette.outer, palette.mid, palette.core, palette.accent, Color.White)
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 36.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (shown == tier) "Your flame evolved!" else "Your flame is evolving...",
                style = DI.type.headline, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(0.5f))
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    // Charging rings pulse faster and faster.
                    val ch = charge.value
                    if (ch > 0f) {
                        val k = sin(clock.value * (6f + ch * 30f))
                        val glowR = r * (0.6f + 0.1f * k * ch)
                        drawCircle(
                            Brush.radialGradient(listOf(palette.core.copy(alpha = 0.55f * ch), palette.mid.copy(alpha = 0.25f * ch), Color.Transparent), center, glowR),
                            glowR
                        )
                    }
                    // Shockwave rings after the flash.
                    val s = shock.value
                    if (s in 0.01f..0.99f) {
                        listOf(0f, 0.2f, 0.4f).forEach { o ->
                            val p = ((s - o) / (1f - o)).coerceIn(0f, 1f)
                            if (p > 0f) drawCircle(palette.accent.copy(alpha = (1f - p) * 0.9f), r * (0.3f + p * 1.1f), style = Stroke((10f - 8f * p) * density))
                        }
                    }
                    if (shown == tier && ch == 0f) {
                        // A slow sunburst and a warm glow behind the new flame.
                        val settle = s.coerceIn(0f, 1f)
                        rotate(clock.value * 12f) {
                            for (i in 0 until 12) {
                                val a0 = Math.toRadians(i * 30.0 - 7.0)
                                val a1 = Math.toRadians(i * 30.0 + 7.0)
                                val ray = Path().apply {
                                    moveTo(center.x, center.y)
                                    lineTo(center.x + kotlin.math.cos(a0).toFloat() * r, center.y + kotlin.math.sin(a0).toFloat() * r)
                                    lineTo(center.x + kotlin.math.cos(a1).toFloat() * r, center.y + kotlin.math.sin(a1).toFloat() * r)
                                    close()
                                }
                                drawPath(ray, Brush.radialGradient(listOf(palette.mid.copy(alpha = 0.32f * settle), Color.Transparent), center, r))
                            }
                        }
                        drawCircle(
                            Brush.radialGradient(listOf(palette.core.copy(alpha = 0.4f * settle), palette.mid.copy(alpha = 0.18f * settle), Color.Transparent), center, r * 0.72f),
                            r * 0.72f
                        )
                    }
                }
                FlameView(
                    tier = shown,
                    face = true,
                    joy = { if (shown == tier) 1f else 0f },
                    intensity = { 1f + charge.value * 0.8f },
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            val ch = charge.value
                            rotationZ = sin(clock.value * (18f + ch * 40f)) * 9f * ch
                            val sc = 1f + 0.18f * ch
                            scaleX = sc
                            scaleY = sc
                        }
                )
            }
            Text(
                tier.title.uppercase(),
                style = DI.type.display.copy(fontSize = DI.type.display.fontSize * 1.3f),
                color = palette.mid,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value.coerceIn(0f, 1f)
                    scaleX = 0.5f + 0.5f * title.value
                    scaleY = 0.5f + 0.5f * title.value
                }
            )
            Text(
                tier.tagline,
                style = DI.type.bodyStrong,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = title.value.coerceIn(0f, 1f) }
            )
            if (unlocks.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                UnlockChip(unlocks.first(), look, Modifier.graphicsLayer { alpha = foxIn.value.coerceIn(0f, 1f) })
            }
            Spacer(Modifier.weight(1f))
        }
        FoxyCorner(
            text = "Whoa! ${tier.title}!",
            foxIn = { foxIn.value },
            bubbleIn = { cheer.value },
            pose = { FoxyPose(cheer = cheer.value, surprise = surprise.value, hop = hopA.value, squash = squash.value, lookX = -0.5f, lookWeight = 0.6f) }
        )
        // The white flash sits over everything.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flash.value }.background(Color.White))
    }
}

// -- goal -------------------------------------------------------------------------------

@Composable
private fun GoalStage(goal: Int, onContinue: () -> Unit) {
    val feedback = LocalFeedback.current
    val reward = Economy.goalReward(goal)
    val textIn = remember { Animatable(0f) }
    val medalIn = remember { Animatable(0f) }
    val medalShine = remember { Animatable(0f) }
    val drop = remember { Animatable(0f) }
    val dust = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val leak = remember { Animatable(0f) }
    val stretchX = remember { Animatable(1f) }
    val stretchY = remember { Animatable(1f) }
    val open = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val fountain = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    var amount by remember { mutableIntStateOf(0) }
    val cheer = remember { Animatable(0f) }
    val hopA = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }

    /** Squash or stretch the chest about its base. */
    suspend fun CoroutineScope.squashTo(x: Float, y: Float, ms: Int) {
        launch { stretchX.animateTo(x, tween(ms, easing = FastOutSlowInEasing)) }
        stretchY.animateTo(y, tween(ms, easing = FastOutSlowInEasing))
    }
    fun CoroutineScope.springBack() {
        launch { stretchX.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium)) }
        launch { stretchY.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium)) }
    }

    val (ready, skip) = rememberBeats(
        finish = {
            textIn.snapTo(1f); medalIn.snapTo(1f); medalShine.snapTo(1f); drop.snapTo(1f); dust.snapTo(1f)
            shake.snapTo(0f); leak.snapTo(0f); stretchX.snapTo(1f); stretchY.snapTo(1f)
            open.snapTo(1f); glow.snapTo(1f); flash.snapTo(0f); fountain.snapTo(1f); confetti.snapTo(1f)
            amount = reward; cheer.snapTo(1f); hopA.snapTo(0f); squash.snapTo(0f)
        }
    ) {
        // The medal spins in and catches the light.
        launch { textIn.animateTo(1f, tween(300)) }
        launch { medalIn.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
        feedback.beat(Sound.Pop, Buzz.Pop)
        launch { delay(260); medalShine.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        delay(320)
        // The chest drops in and lands with a puff of dust.
        launch { drop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)) }
        delay(300)
        feedback.beat(null, Buzz.Thud)
        launch { dust.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        launch { stretchX.snapTo(1.1f); stretchY.snapTo(0.86f); springBack() }
        delay(420)
        // Three rattles, each harder, with more light squeezing out.
        repeat(3) { i ->
            val k = (i + 1) / 3f
            feedback.beat(Sound.Rattle, Buzz.Tick, i * 2f)
            launch { leak.animateTo(k, tween(120)) }
            shake.animateTo(0.45f + 0.55f * k, tween(70))
            shake.animateTo(0f, tween(200))
            delay(150L - i * 45)
        }
        // Wind up...
        squashTo(1.13f, 0.82f, 160)
        // ...and burst open.
        feedback.beat(Sound.Fanfare, Buzz.Big)
        leak.snapTo(0f)
        launch {
            launch { stretchX.animateTo(0.9f, tween(90)) }
            stretchY.animateTo(1.16f, tween(90))
            springBack()
        }
        launch { open.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium)) }
        launch { flash.snapTo(1f); flash.animateTo(0f, tween(480)) }
        launch { glow.animateTo(1f, tween(450)) }
        launch { fountain.animateTo(1f, tween(1500, easing = LinearEasing)) }
        launch { confetti.animateTo(1f, tween(2400, easing = LinearEasing)) }
        launch { cheer.animateTo(1f, spring(dampingRatio = 0.6f)) }
        launch { hop(hopA, squash, 30f) }
        delay(380)
        // Count the reward up with a run of blings.
        val steps = 8
        for (k in 1..steps) {
            amount = reward * k / steps
            feedback.beat(Sound.Coin, if (k == steps) Buzz.Success else Buzz.Tick, k * 1.2f)
            delay(70)
        }
    }

    val dustColor = Color(0xFFD9C3A0)
    StageScaffold(ready, skip, "Claim", onContinue, buttonChunk = Chunks.gold()) {
        Confetti(progress = { confetti.value }, modifier = Modifier.fillMaxSize(), seed = goal, count = 80)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MedalView(
                goal, done = true,
                shine = { medalShine.value },
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        val m = medalIn.value
                        alpha = m.coerceIn(0f, 1f)
                        scaleX = m
                        scaleY = m
                        rotationZ = (1f - m) * -40f
                    }
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = textIn.value.coerceIn(0f, 1f) }) {
                Text("${goal}-day goal!", style = DI.type.display, color = DI.colors.text)
                Text("Goal complete · ${Economy.goalLabel(goal)}", style = DI.type.bodyStrong, color = DI.colors.textMuted)
            }
            Spacer(Modifier.weight(0.4f))
            Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
                InkFountain(progress = { fountain.value }, modifier = Modifier.fillMaxSize())
                // Dust kicked up where the chest lands.
                Canvas(Modifier.fillMaxSize()) {
                    val p = dust.value
                    if (p <= 0f || p >= 1f) return@Canvas
                    val baseY = size.height * 0.86f
                    for (i in 0 until 6) {
                        val side = if (i % 2 == 0) -1f else 1f
                        val lane = (i / 2) / 2f
                        val x = size.width / 2f + side * size.width * (0.22f + 0.2f * p + lane * 0.06f)
                        val y = baseY - p * size.height * (0.04f + lane * 0.05f)
                        drawCircle(dustColor, (6f + 12f * p - lane * 3f) * density, Offset(x, y), alpha = (1f - p) * 0.8f)
                    }
                }
                ChestView(
                    modifier = Modifier
                        .size(236.dp)
                        .offset { IntOffset(0, ((1f - drop.value) * -900).roundToInt()) }
                        .graphicsLayer {
                            scaleX = stretchX.value
                            scaleY = stretchY.value
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f)
                        },
                    open = { open.value },
                    shake = { shake.value },
                    glow = { glow.value },
                    leak = { leak.value }
                )
                // The white pop as the lid flies open.
                Canvas(Modifier.fillMaxSize()) {
                    val f = flash.value
                    if (f <= 0f) return@Canvas
                    val c = Offset(size.width / 2f, size.height * 0.46f)
                    drawCircle(Color.White, size.minDimension * (0.18f + 0.36f * (1f - f)), c, alpha = f * 0.7f)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = if (amount == 0) 0f else open.value.coerceIn(0f, 1f) }) {
                Text("+", style = DI.type.display, color = DI.colors.ink)
                RollingNumber(amount, DI.type.display, DI.colors.ink)
                Spacer(Modifier.width(8.dp))
                InkDrop(Modifier.size(40.dp))
            }
            Economy.nextGoal(goal)?.let { next ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Next goal: $next days · worth ${Economy.goalReward(next)} ink",
                    style = DI.type.bodyStrong,
                    color = DI.colors.textMuted,
                    modifier = Modifier.graphicsLayer { alpha = open.value.coerceIn(0f, 1f) }
                )
            }
            Spacer(Modifier.weight(1f))
        }
        FoxyCorner(
            text = "Treasure! You earned it.",
            foxIn = { drop.value },
            bubbleIn = { cheer.value },
            pose = { FoxyPose(cheer = cheer.value, surprise = leak.value * 0.6f, hop = hopA.value, squash = squash.value, lookX = -0.6f, lookWeight = 0.7f) }
        )
    }
}

/** Ink drops bursting up out of the chest and raining down. */
@Composable
private fun InkFountain(progress: () -> Float, modifier: Modifier) {
    val ink = DI.colors.ink
    val inkLight = Color(0xFF7FA0FF)
    val drops = remember {
        val rng = kotlin.random.Random(42)
        List(22) { Triple(-90f + (rng.nextFloat() - 0.5f) * 110f, 0.5f + rng.nextFloat() * 0.6f, rng.nextFloat() * 0.25f) }
    }
    Canvas(modifier) {
        val p0 = progress()
        if (p0 <= 0f || p0 >= 1f) return@Canvas
        val origin = Offset(size.width / 2f, size.height * 0.55f)
        drops.forEachIndexed { i, (angle, speed, delayFrac) ->
            val p = ((p0 - delayFrac) / (1f - delayFrac)).coerceIn(0f, 1f)
            if (p <= 0f) return@forEachIndexed
            val rad = Math.toRadians(angle.toDouble())
            val v = size.minDimension * 0.9f * speed
            val x = origin.x + kotlin.math.cos(rad).toFloat() * v * p
            val y = origin.y + kotlin.math.sin(rad).toFloat() * v * p + p * p * size.height * 0.9f
            val s = (9f + (i % 3) * 3f) * density
            val drop = Path().apply {
                moveTo(x, y - s)
                cubicTo(x + s * 0.2f, y - s * 0.5f, x + s * 0.8f, y, x + s * 0.8f, y + s * 0.35f)
                cubicTo(x + s * 0.8f, y + s, x - s * 0.8f, y + s, x - s * 0.8f, y + s * 0.35f)
                cubicTo(x - s * 0.8f, y, x - s * 0.2f, y - s * 0.5f, x, y - s)
                close()
            }
            val a = (1f - p * p).coerceIn(0f, 1f)
            drawPath(drop, if (i % 4 == 0) inkLight.copy(alpha = a) else ink.copy(alpha = a))
        }
    }
}

// -- rewards ----------------------------------------------------------------------------

@Composable
private fun RewardsStage(c: Celebration, look: AvatarLook, onContinue: () -> Unit, last: Boolean) {
    val feedback = LocalFeedback.current
    val lines = c.reward.lines
    var shownLines by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var balance by remember { mutableIntStateOf(c.balanceBefore) }
    val shine = remember { Animatable(0f) }
    val unlockIn = remember { Animatable(0f) }
    val wave = remember { Animatable(0f) }
    val (ready, skip) = rememberBeats(
        finish = { shownLines = lines.size; total = c.reward.total; balance = c.balanceAfter; shine.snapTo(1f); unlockIn.snapTo(1f); wave.snapTo(1f) }
    ) {
        launch { wave.animateTo(1f, tween(400)) }
        delay(250)
        lines.forEachIndexed { i, line ->
            shownLines = i + 1
            total += line.amount
            feedback.beat(Sound.Coin, Buzz.Tick, (i * 2f).coerceAtMost(12f))
            delay(if (line.kind == RewardKind.Goal) 420 else 300)
        }
        launch { shine.animateTo(1f, tween(700)) }
        // The balance ticks up to its new value.
        val from = balance
        val steps = 10
        for (k in 1..steps) {
            balance = from + (c.balanceAfter - from) * k / steps
            if (k % 2 == 0) feedback.beat(Sound.Tick, null)
            delay(40)
        }
        if (c.unlocked.isNotEmpty()) {
            delay(150)
            feedback.beat(Sound.Unlock, Buzz.Success)
            unlockIn.animateTo(1f, spring(dampingRatio = 0.55f))
        }
    }
    StageScaffold(ready, skip, if (last) "Done" else "Continue", onContinue, buttonChunk = Chunks.ink()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 32.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InkDrop(Modifier.size(96.dp), shine = { shine.value })
            Spacer(Modifier.height(6.dp))
            Text("Ink earned", style = DI.type.display, color = DI.colors.text)
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DI.colors.surface)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                lines.forEachIndexed { i, line ->
                    val visible = i < shownLines
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .graphicsLayer { alpha = if (visible) 1f else 0f; translationX = if (visible) 0f else 30f },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(line.label, style = DI.type.bodyStrong, color = DI.colors.text, modifier = Modifier.weight(1f))
                        Text("+${line.amount}", style = DI.type.number, color = DI.colors.ink)
                    }
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(DI.colors.line))
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", style = DI.type.title, color = DI.colors.text, modifier = Modifier.weight(1f))
                    Text("+", style = DI.type.headline, color = DI.colors.ink)
                    RollingNumber(total, DI.type.headline, DI.colors.ink)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Balance ", style = DI.type.bodyStrong, color = DI.colors.textMuted)
                RollingNumber(balance, DI.type.number, DI.colors.ink)
                Spacer(Modifier.width(4.dp))
                InkDrop(Modifier.size(18.dp), animate = false)
            }
            if (c.unlocked.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                c.unlocked.take(2).forEach { item ->
                    UnlockChip(item, look, Modifier.padding(vertical = 4.dp).graphicsLayer {
                        alpha = unlockIn.value.coerceIn(0f, 1f)
                        scaleX = 0.7f + 0.3f * unlockIn.value
                        scaleY = 0.7f + 0.3f * unlockIn.value
                    })
                }
            }
            Spacer(Modifier.weight(1f))
        }
        FoxyCorner(
            text = "Spend it on something fun in the shop!",
            foxIn = { wave.value },
            bubbleIn = { if (ready) 1f else 0f },
            pose = { FoxyPose(wave = 1f, cheer = 0.25f, lookX = -0.4f, lookWeight = 0.5f) }
        )
    }
}

@Composable
private fun UnlockChip(item: AvatarItem, look: AvatarLook, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DI.colors.brandSoft)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape)) { AvatarView(look.with(item), Modifier.size(56.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("UNLOCKED", style = DI.type.label, color = DI.colors.brand)
            Text(item.name, style = DI.type.title, color = DI.colors.text)
        }
    }
}

// -- Foxy in the corner -------------------------------------------------------------------

@Composable
private fun BoxScope.FoxyCorner(
    text: String,
    foxIn: () -> Float,
    bubbleIn: () -> Float,
    pose: () -> FoxyPose
) {
    Row(
        Modifier
            .align(Alignment.BottomStart)
            .padding(start = 8.dp, bottom = 76.dp, end = 24.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        FoxyView(
            Modifier
                .size(130.dp)
                .graphicsLayer {
                    val p = foxIn()
                    alpha = p.coerceIn(0f, 1f)
                    translationY = (1f - p) * 140.dp.toPx()
                },
            fit = 0.8f,
            pose = pose
        )
        SpeechBubble(
            text,
            modifier = Modifier
                .padding(bottom = 48.dp)
                .graphicsLayer {
                    val b = bubbleIn()
                    alpha = b.coerceIn(0f, 1f)
                    scaleX = 0.6f + 0.4f * b
                    scaleY = 0.6f + 0.4f * b
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                }
        )
    }
}
