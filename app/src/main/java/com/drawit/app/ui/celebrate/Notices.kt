package com.drawit.app.ui.celebrate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drawit.app.R
import com.drawit.app.data.StreakOutcome
import com.drawit.app.data.WelcomeGift
import com.drawit.app.feedback.Feedback.Buzz
import com.drawit.app.feedback.Feedback.Sound
import com.drawit.app.model.FlameTier
import com.drawit.app.ui.art.FoxyPose
import com.drawit.app.ui.components.ChestView
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Confetti
import com.drawit.app.ui.components.FlameView
import com.drawit.app.ui.components.FoxyView
import com.drawit.app.ui.components.InkDrop
import com.drawit.app.ui.components.LocalFeedback
import com.drawit.app.ui.components.RollingNumber
import com.drawit.app.ui.components.SpeechBubble
import com.drawit.app.ui.theme.DI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The first time the new version opens: Foxy says hello, and a chest pays out
 * the welcome gift plus ink for every drawing already in the album.
 */
@Composable
fun WelcomeOverlay(gift: WelcomeGift, onShop: () -> Unit, onDone: () -> Unit) {
    val feedback = LocalFeedback.current
    BackHandler { onDone() }
    var step by remember { mutableIntStateOf(0) }
    val wave = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val leak = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    var amount by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        feedback?.play(Sound.Whoosh)
        wave.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(step) {
        if (step != 1) return@LaunchedEffect
        repeat(3) { i ->
            feedback?.play(Sound.Rattle, i * 2f); feedback?.buzz(Buzz.Tick)
            launch { leak.animateTo((i + 1) / 3f, tween(120)) }
            shake.animateTo(0.5f + 0.17f * (i + 1), tween(80)); shake.animateTo(0f, tween(200))
            delay(130L - i * 40)
        }
        feedback?.play(Sound.Fanfare); feedback?.buzz(Buzz.Big)
        opened = true
        leak.snapTo(0f)
        launch { open.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium)) }
        launch { glow.animateTo(1f, tween(500)) }
        launch { confetti.animateTo(1f, tween(2400, easing = LinearEasing)) }
        delay(300)
        val steps = 12
        for (k in 1..steps) {
            amount = gift.total * k / steps
            feedback?.play(Sound.Coin, k * 1f)
            delay(60)
        }
        feedback?.buzz(Buzz.Success)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(DI.colors.background)
            .clickable(remember { MutableInteractionSource() }, null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Confetti(progress = { confetti.value }, modifier = Modifier.fillMaxSize(), seed = 99, count = 80)
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (step == 0) {
                SpeechBubble("Hi! I'm Foxy. I'll cheer you on every time you draw.")
                FoxyView(Modifier.size(260.dp), fit = 0.85f) { FoxyPose(wave = wave.value, cheer = 0.2f) }
                Spacer(Modifier.height(12.dp))
                Text("Draw it got a glow-up", style = DI.type.display, color = DI.colors.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your streak now evolves as it grows, goals pay out chests of ink, and you can spend ink " +
                        "dressing up your own avatar.",
                    style = DI.type.body, color = DI.colors.textMuted, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                ChunkyButton("I have a gift for you", { step = 1 }, modifier = Modifier.fillMaxWidth())
            } else {
                Text(if (opened) "Welcome gift!" else "Open it!", style = DI.type.display, color = DI.colors.text)
                Spacer(Modifier.height(8.dp))
                ChestView(Modifier.size(220.dp), open = { open.value }, shake = { shake.value }, glow = { glow.value }, leak = { leak.value })
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = if (amount == 0) 0f else open.value.coerceIn(0f, 1f) }) {
                    Text("+", style = DI.type.display, color = DI.colors.ink)
                    RollingNumber(amount, DI.type.display, DI.colors.ink)
                    Spacer(Modifier.width(8.dp))
                    InkDrop(Modifier.size(40.dp))
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = open.value.coerceIn(0f, 1f) }
                        .clip(RoundedCornerShape(18.dp))
                        .background(DI.colors.surface)
                        .padding(16.dp)
                ) {
                    GiftLine("Welcome gift", gift.welcome)
                    if (gift.backPay > 0) GiftLine(if (gift.drawings == 1) "Your past drawing" else "Your ${gift.drawings} past drawings", gift.backPay)
                    if (gift.unlocked.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Plus ${gift.unlocked.size} streak reward${if (gift.unlocked.size == 1) "" else "s"} for your avatar: " +
                                gift.unlocked.joinToString { it.name },
                            style = DI.type.small, color = DI.colors.brand
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                ChunkyButton("Make my avatar", onShop, chunk = Chunks.ink(), enabled = opened, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                ChunkyButton("Later", onDone, chunk = Chunks.plain(), enabled = opened, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GiftLine(label: String, amount: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = DI.type.bodyStrong, color = DI.colors.text, modifier = Modifier.weight(1f))
        Text("+$amount", style = DI.type.number, color = DI.colors.ink)
    }
}

/**
 * Shown when the app opens after missed days: either a freeze held the
 * streak, or the streak ended. Either way Foxy is there.
 */
@Composable
fun StreakNoticeOverlay(outcome: StreakOutcome, onDone: () -> Unit) {
    val feedback = LocalFeedback.current
    val froze = outcome.freezesSpent > 0
    BackHandler { onDone() }
    val enter = remember { Animatable(0f) }
    val ice = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { enter.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) }
        delay(350)
        if (froze) {
            feedback?.play(Sound.Freeze); feedback?.buzz(Buzz.Soft)
            ice.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        } else {
            feedback?.play(Sound.Lost); feedback?.buzz(Buzz.Soft)
            fade.animateTo(0f, tween(1200, easing = FastOutSlowInEasing))
        }
    }

    val ink = DI.colors.ice
    Box(
        Modifier
            .fillMaxSize()
            .background(if (froze) DI.colors.iceSoft else DI.colors.background)
            .clickable(remember { MutableInteractionSource() }, null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp).graphicsLayer { alpha = enter.value.coerceIn(0f, 1f); translationY = (1f - enter.value) * 80f },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                val tier = FlameTier.forStreak(if (froze) outcome.state.current else outcome.lostStreak)
                FlameView(
                    tier,
                    Modifier.size(180.dp).graphicsLayer { alpha = if (froze) 1f else 0.25f + 0.75f * fade.value },
                    lit = froze || fade.value > 0.5f,
                    face = true
                )
                if (froze) {
                    // A block of ice forming around the flame.
                    Box(
                        Modifier
                            .size(150.dp)
                            .graphicsLayer { alpha = ice.value * 0.55f; scaleX = 0.6f + 0.4f * ice.value; scaleY = 0.6f + 0.4f * ice.value }
                            .clip(RoundedCornerShape(36.dp))
                            .background(ink)
                    )
                    Icon(
                        painterResource(R.drawable.ic_snowflake), null, tint = Color.White,
                        modifier = Modifier.size(56.dp).graphicsLayer { alpha = ice.value; rotationZ = ice.value * 60f }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (froze) "Your streak froze!" else "Your streak ended",
                style = DI.type.display, color = if (froze) DI.colors.iceLip else DI.colors.text, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (froze) {
                    val n = outcome.freezesSpent
                    "You missed ${if (n == 1) "a day" else "$n days"}, so ${if (n == 1) "a freeze" else "$n freezes"} kept your " +
                        "${outcome.state.current}-day streak safe. ${outcome.state.freezes} left."
                } else {
                    "The days added up and there were no freezes left. One drawing today starts a new flame."
                },
                style = DI.type.body, color = DI.colors.textMuted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                FoxyView(Modifier.size(120.dp), fit = 0.85f) {
                    if (froze) FoxyPose(surprise = 0.5f, cheer = 0.2f * ice.value) else FoxyPose(sad = 1f - fade.value * 0.3f)
                }
                SpeechBubble(
                    if (froze) "Phew! That was close." else "It's okay. Let's start again together.",
                    modifier = Modifier.padding(bottom = 30.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            ChunkyButton(
                if (froze) "Keep going" else "Start a new streak",
                onDone,
                chunk = if (froze) Chunks.ice() else Chunks.brand(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
