package com.drawit.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drawit.app.R
import com.drawit.app.data.StreakState
import com.drawit.app.model.Economy
import com.drawit.app.model.FlameTier
import com.drawit.app.ui.theme.DI
import java.time.DayOfWeek
import java.time.LocalDate

enum class DayMark { Drawn, Frozen, Missed, Today, Future }

fun markFor(day: LocalDate, today: LocalDate, drawn: Set<Long>, frozen: Set<Long>): DayMark {
    val key = day.toEpochDay()
    return when {
        key in drawn -> DayMark.Drawn
        key in frozen -> DayMark.Frozen
        day.isAfter(today) -> DayMark.Future
        day == today -> DayMark.Today
        else -> DayMark.Missed
    }
}

/**
 * Monday to Sunday. Drawn days join into one flame-coloured run, frozen days
 * are ice, and today breathes until it's filled in. [revealUpTo] lets the
 * celebration fill days in one at a time (index 0..7).
 */
@Composable
fun WeekStrip(
    drawn: Set<Long>,
    frozen: Set<Long>,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    revealUpTo: () -> Float = { 7f }
) {
    val monday = today.with(DayOfWeek.MONDAY)
    val marks = (0..6).map { markFor(monday.plusDays(it.toLong()), today, drawn, frozen) }
    val flame = DI.colors.flame
    val ice = DI.colors.ice
    val pulse = rememberInfiniteTransition(label = "today").animateFloat(
        0.92f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "todayPulse"
    )
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { i ->
                val day = monday.plusDays(i.toLong())
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        StreakState.initial(day.dayOfWeek),
                        style = DI.type.label,
                        color = if (day == today) DI.colors.text else DI.colors.textMuted
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            // The bar joining consecutive drawn days.
            Canvas(Modifier.matchParentSize()) {
                val cell = size.width / 7f
                val h = size.height * 0.42f
                val reveal = revealUpTo()
                var i = 0
                while (i < 7) {
                    if (marks[i] == DayMark.Drawn || marks[i] == DayMark.Frozen) {
                        var j = i
                        while (j + 1 < 7 && (marks[j + 1] == DayMark.Drawn || marks[j + 1] == DayMark.Frozen)) j++
                        val end = minOf(j.toFloat(), reveal - 1f)
                        if (j > i && end > i) {
                            drawRoundRect(
                                flame.copy(alpha = 0.28f),
                                topLeft = Offset(cell * (i + 0.5f), (size.height - h) / 2f),
                                size = Size(cell * (end - i), h),
                                cornerRadius = CornerRadius(h / 2f)
                            )
                        }
                        i = j + 1
                    } else i++
                }
            }
            Row(Modifier.fillMaxWidth()) {
                marks.forEachIndexed { i, mark ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DayDot(
                            mark = mark,
                            shown = { (revealUpTo() - i).coerceIn(0f, 1f) },
                            pulse = { if (mark == DayMark.Today) pulse.value else 1f },
                            flame = flame,
                            ice = ice
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDot(mark: DayMark, shown: () -> Float, pulse: () -> Float, flame: Color, ice: Color) {
    val base = DI.colors.sunken
    val line = DI.colors.lineStrong
    Box(
        Modifier
            .size(34.dp)
            .graphicsLayer {
                val s = shown()
                // Filled days pop in with a little overshoot as they're revealed.
                val pop = if (mark == DayMark.Drawn || mark == DayMark.Frozen) 0.6f + 0.4f * s + 0.25f * kotlin.math.sin(s * Math.PI.toFloat()) else 1f
                scaleX = pop * pulse()
                scaleY = pop * pulse()
            }
            .clip(CircleShape)
            .background(
                when (mark) {
                    DayMark.Drawn -> flame
                    DayMark.Frozen -> ice
                    DayMark.Future -> base.copy(alpha = 0.6f)
                    else -> base
                }
            )
            .then(if (mark == DayMark.Today) Modifier.border(3.dp, flame, CircleShape) else if (mark == DayMark.Missed) Modifier.border(2.dp, line, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when (mark) {
            DayMark.Drawn -> Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(18.dp).graphicsLayer { alpha = shown() })
            DayMark.Frozen -> Icon(painterResource(R.drawable.ic_snowflake), null, tint = Color.White, modifier = Modifier.size(18.dp).graphicsLayer { alpha = shown() })
            DayMark.Today -> Icon(painterResource(R.drawable.ic_flame), null, tint = flame.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
            else -> Unit
        }
    }
}

/** Progress from the last goal reached to the next one, with a chest at the end. */
@Composable
fun GoalProgress(streak: Int, modifier: Modifier = Modifier, progressOverride: (() -> Float)? = null) {
    val next = Economy.nextGoal(streak)
    val prev = Economy.previousGoal(streak)
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (next != null) "${next}-day goal" else "Every goal complete",
                style = DI.type.title,
                color = DI.colors.text,
                modifier = Modifier.weight(1f)
            )
            if (next != null) {
                val left = next - streak
                Text(
                    if (left == 1) "1 day to go" else "$left days to go",
                    style = DI.type.small,
                    color = DI.colors.textMuted
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val fraction = if (next == null) 1f else (streak - prev).toFloat() / (next - prev).toFloat()
            ChunkyProgress(
                progress = progressOverride?.invoke() ?: fraction,
                color = DI.colors.flame,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            ChestView(Modifier.size(34.dp), open = { 0f }, shake = { 0f })
        }
        if (next != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Day $streak of $next · reward", style = DI.type.small, color = DI.colors.textMuted)
                Spacer(Modifier.width(4.dp))
                InkAmount(Economy.goalReward(next), size = 14.dp)
            }
        }
    }
}

/** The big tier badge used on the streak screen and profile. */
@Composable
fun TierBadge(tier: FlameTier, reached: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (reached) DI.colors.flameSoft else DI.colors.sunken),
            contentAlignment = Alignment.Center
        ) {
            FlameView(tier, Modifier.size(52.dp), lit = reached, detail = reached)
            if (!reached) Icon(painterResource(R.drawable.ic_lock), null, tint = DI.colors.textFaint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(tier.title, style = DI.type.label, color = if (reached) DI.colors.text else DI.colors.textFaint)
        Text("${tier.minStreak}+", style = DI.type.small, color = DI.colors.textFaint)
    }
}
