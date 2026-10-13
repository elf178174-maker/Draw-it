package com.drawit.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.drawit.app.ui.art.medalStyle
import com.drawit.app.ui.components.ChunkyProgress
import com.drawit.app.ui.components.MedalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.StreakState
import com.drawit.app.model.Economy
import com.drawit.app.model.FlameTier
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyCard
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.DayMark
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.Eyebrow
import com.drawit.app.ui.components.FlameView
import com.drawit.app.ui.components.InkAmount
import com.drawit.app.ui.components.ScreenHeader
import com.drawit.app.ui.components.TierBadge
import com.drawit.app.ui.components.WeekStrip
import com.drawit.app.ui.components.markFor
import com.drawit.app.ui.theme.DI
import java.time.DayOfWeek
import java.time.LocalDate

private const val HISTORY_WEEKS = 10

@Composable
fun StreakScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val state by viewModel.streak.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val drawn = remember(drawings) { viewModel.drawnDays(drawings) }
    val today = remember { LocalDate.now() }
    val drewToday = viewModel.drewToday(state)
    val tier = FlameTier.forStreak(state.current)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Streak", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Entrance(delayMillis = 30) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    FlameView(tier, Modifier.size(170.dp), lit = drewToday, face = drewToday, joy = { 1f })
                    Text("${state.current}", style = DI.type.hero, color = if (drewToday) DI.colors.flame else DI.colors.textFaint)
                    Text("day streak", style = DI.type.headline, color = if (drewToday) DI.colors.flame else DI.colors.textMuted)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tier.next?.let { n ->
                            val left = n.minStreak - state.current
                            "${tier.title} · ${if (left == 1) "1 day" else "$left days"} until ${n.title}"
                        } ?: tier.title,
                        style = DI.type.bodyStrong,
                        color = DI.colors.textMuted
                    )
                    if (!drewToday) {
                        Spacer(Modifier.height(6.dp))
                        Text("Draw today to light your flame.", style = DI.type.small, color = DI.colors.brand)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Entrance(delayMillis = 80) {
                ChunkyCard {
                    WeekStrip(drawn = drawn, frozen = state.frozenDays, today = today)
                }
            }

            Spacer(Modifier.height(14.dp))
            Entrance(delayMillis = 120) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Longest", "${state.best}", R.drawable.ic_trophy, DI.colors.gold, Modifier.weight(1f))
                    StatTile("Drawings", "${drawings.size}", R.drawable.ic_album, DI.colors.purple, Modifier.weight(1f))
                    StatTile("Freezes used", "${state.freezesUsedTotal}", R.drawable.ic_snowflake, DI.colors.ice, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(14.dp))
            Entrance(delayMillis = 160) { FreezeCard(state, wallet.balance) { viewModel.buyFreeze() } }

            Spacer(Modifier.height(22.dp))
            Text("Streak goals", style = DI.type.headline, color = DI.colors.text)
            Text("Reach each one for a chest of ink. A broken streak starts the path again.", style = DI.type.small, color = DI.colors.textMuted)
            Spacer(Modifier.height(14.dp))
            GoalPath(state.current)

            Spacer(Modifier.height(22.dp))
            Text("Flame evolution", style = DI.type.headline, color = DI.colors.text)
            Text("Your flame changes as your streak grows. Best so far: ${FlameTier.forStreak(state.best).title}.", style = DI.type.small, color = DI.colors.textMuted)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FlameTier.entries.drop(1).forEach { t -> TierBadge(t, reached = state.best >= t.minStreak) }
            }

            Spacer(Modifier.height(22.dp))
            ChunkyCard {
                Eyebrow("Last $HISTORY_WEEKS weeks")
                Spacer(Modifier.height(12.dp))
                HistoryGrid(drawn, state.frozenDays, today)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(DI.colors.flame, "drew")
                    LegendDot(DI.colors.ice, "frozen")
                    LegendDot(DI.colors.sunken, "missed")
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(32.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: Int, color: Color, modifier: Modifier) {
    ChunkyCard(modifier = modifier, padding = PaddingValues(12.dp)) {
        Icon(painterResource(icon), null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, style = DI.type.headline, color = DI.colors.text)
        Text(label, style = DI.type.small, color = DI.colors.textMuted)
    }
}

@Composable
private fun FreezeCard(state: StreakState, balance: Int, onBuy: () -> Unit) {
    val full = state.freezes >= StreakState.MAX_FREEZES
    ChunkyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Streak freezes", style = DI.type.title, color = DI.colors.text)
                Text(
                    "Miss a day and a freeze keeps your streak alive. You get one free each week.",
                    style = DI.type.small, color = DI.colors.textMuted
                )
            }
            Spacer(Modifier.width(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until StreakState.MAX_FREEZES).forEach { i ->
                    val on = i < state.freezes
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(if (on) DI.colors.ice else DI.colors.sunken),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_snowflake), null, tint = if (on) Color.White else DI.colors.textFaint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ChunkyButton(
            text = if (full) "Freezes full" else "Buy a freeze",
            onClick = onBuy,
            enabled = !full && balance >= Economy.FREEZE_PRICE,
            chunk = Chunks.ice(),
            modifier = Modifier.fillMaxWidth(),
            trailing = if (full) null else ({ InkAmount(Economy.FREEZE_PRICE, color = Color.White) })
        )
    }
}

/** Goals laid out as a winding path of medals, like a level map. */
@Composable
private fun GoalPath(streak: Int) {
    val pulse = rememberInfiniteTransition(label = "goal").animateFloat(
        1f, 1.08f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "goalPulse"
    )
    val current = Economy.nextGoal(streak)
    Column(Modifier.fillMaxWidth()) {
        Economy.GOALS.forEachIndexed { i, goal ->
            val done = streak >= goal
            val isCurrent = goal == current
            // Zig-zag: left, centre, right, centre...
            val bias = when (i % 4) { 0 -> -0.5f; 1 -> 0f; 2 -> 0.5f; else -> 0f }
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    Modifier.align(androidx.compose.ui.BiasAlignment(bias, 0f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoalMedal(
                        goal, done, isCurrent,
                        Modifier.graphicsLayer {
                            val s = if (isCurrent) pulse.value else 1f
                            scaleX = s
                            scaleY = s
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("$goal days", style = DI.type.title, color = if (done || isCurrent) DI.colors.text else DI.colors.textFaint)
                        InkAmount(Economy.goalReward(goal), size = 14.dp, color = if (done) DI.colors.textFaint else DI.colors.ink)
                        if (isCurrent) {
                            val prev = Economy.previousGoal(streak)
                            Spacer(Modifier.height(6.dp))
                            ChunkyProgress(
                                progress = ((streak - prev).toFloat() / (goal - prev)).coerceIn(0f, 1f),
                                color = DI.colors.flame,
                                modifier = Modifier.width(96.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A medal with the goal's number on its face: in colour once earned, grey until then. */
@Composable
private fun GoalMedal(goal: Int, done: Boolean, current: Boolean, modifier: Modifier = Modifier) {
    val style = medalStyle(goal)
    val digits = goal.toString().length
    val size = when { digits <= 2 -> 20.sp; digits == 3 -> 15.sp; else -> 12.sp }
    val shadow = if (done) style.shade else Color(0xFFC4C4C4)
    Box(modifier.size(72.dp)) {
        MedalView(goal, done = done, emblem = false, modifier = Modifier.size(72.dp))
        Text(
            "$goal",
            style = DI.type.number.copy(fontSize = size, shadow = Shadow(shadow, Offset(0f, 5f))),
            color = when {
                done -> Color.White
                current -> DI.colors.flame
                else -> Color.White
            },
            textAlign = TextAlign.Center,
            // The medal's face sits a little above and left of the middle.
            modifier = Modifier.align(Alignment.Center).offset(x = (-1).dp, y = (-6).dp)
        )
    }
}

@Composable
private fun HistoryGrid(drawn: Set<Long>, frozen: Set<Long>, today: LocalDate) {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        (HISTORY_WEEKS - 1 downTo 0).forEach { weeksAgo ->
            val monday = thisMonday.minusWeeks(weeksAgo.toLong())
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (0..6).forEach { offset ->
                    val day = monday.plusDays(offset.toLong())
                    val mark = markFor(day, today, drawn, frozen)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (mark) {
                                    DayMark.Drawn -> DI.colors.flame
                                    DayMark.Frozen -> DI.colors.ice
                                    DayMark.Missed -> DI.colors.sunken
                                    DayMark.Today -> DI.colors.flameSoft
                                    DayMark.Future -> DI.colors.sunken.copy(alpha = 0.4f)
                                }
                            )
                            .then(if (mark == DayMark.Today) Modifier.border(2.dp, DI.colors.flame, RoundedCornerShape(6.dp)) else Modifier)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = DI.type.small, color = DI.colors.textMuted)
    }
}
