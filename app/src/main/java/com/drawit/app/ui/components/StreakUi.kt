package com.drawit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawit.app.R
import com.drawit.app.data.StreakOutcome
import com.drawit.app.data.StreakState
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

/** Monday to Sunday for the week containing [today]. */
@Composable
fun WeekStrip(
    drawn: Set<Long>,
    frozen: Set<Long>,
    today: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier
) {
    val monday = today.with(DayOfWeek.MONDAY)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        (0..6).forEach { offset ->
            val day = monday.plusDays(offset.toLong())
            DayCell(
                letter = StreakState.initial(day.dayOfWeek),
                mark = markFor(day, today, drawn, frozen),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(letter: String, mark: DayMark, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = when (mark) {
            DayMark.Drawn -> scheme.primary
            DayMark.Frozen -> scheme.primaryContainer
            else -> Color.Transparent
        },
        animationSpec = tween(320),
        label = "dayFill"
    )
    val content = when (mark) {
        DayMark.Drawn -> scheme.onPrimary
        DayMark.Frozen -> scheme.onPrimaryContainer
        DayMark.Today -> scheme.primary
        DayMark.Future -> scheme.outline
        DayMark.Missed -> scheme.onSurfaceVariant
    }
    val outline = when (mark) {
        DayMark.Today -> scheme.primary
        DayMark.Drawn, DayMark.Frozen -> Color.Transparent
        else -> scheme.outline
    }

    // Today's cell breathes until it is filled in.
    val pulse = if (mark == DayMark.Today) {
        val transition = rememberInfiniteTransition(label = "todayPulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "todayPulseScale"
        ).value
    } else {
        1f
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(fill)
                .border(1.5.dp, outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (mark) {
                DayMark.Drawn -> Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(15.dp)
                )
                DayMark.Frozen -> Icon(
                    painter = painterResource(R.drawable.ic_snowflake),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(15.dp)
                )
                else -> Text(
                    text = letter,
                    style = MaterialTheme.typography.labelMedium,
                    color = content
                )
            }
        }
    }
}

/** The small snowflake pips showing how many freezes are banked. */
@Composable
fun FreezePips(available: Int, modifier: Modifier = Modifier, tint: Color? = null) {
    val active = tint ?: MaterialTheme.colorScheme.primary
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (0 until StreakState.MAX_FREEZES).forEach { index ->
            val on = index < available
            val scale by animateFloatAsState(
                targetValue = if (on) 1f else 0.82f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "pip"
            )
            Icon(
                painter = painterResource(R.drawable.ic_snowflake),
                contentDescription = null,
                tint = if (on) active else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

/** The big streak card on the Today screen. */
@Composable
fun StreakHeroCard(
    state: StreakState,
    drawn: Set<Long>,
    drewToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flameScale by animateFloatAsState(
        targetValue = if (drewToday) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "flame"
    )

    PaperCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(22.dp),
        fill = if (drewToday) MaterialTheme.colorScheme.primaryContainer else null,
        onClick = onClick
    ) {
        val onFill = if (drewToday) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
        val muted = if (drewToday) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
        else MaterialTheme.colorScheme.onSurfaceVariant

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (drewToday) "Done today" else "Your streak", color = muted)
            Spacer(Modifier.weight(1f))
            FreezePips(
                available = state.freezes,
                tint = if (drewToday) MaterialTheme.colorScheme.onPrimaryContainer else null
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_flame),
                contentDescription = null,
                tint = if (state.hasStreak) MaterialTheme.colorScheme.primary else muted,
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = flameScale
                        scaleY = flameScale
                    }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${state.current}",
                fontSize = 52.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                color = onFill
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (state.current == 1) "day\nin a row" else "days\nin a row",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }

        Spacer(Modifier.height(18.dp))

        WeekStrip(drawn = drawn, frozen = state.frozenDays)

        Spacer(Modifier.height(14.dp))

        Text(
            text = when {
                !drewToday && state.hasStreak ->
                    "Draw today to keep it going."
                !drewToday ->
                    "Draw today to start one."
                else -> StreakState.nextMilestone(state.current)?.let {
                    val away = it - state.current
                    if (away == 1) "One more day to ${StreakState.milestoneLabel(it).lowercase()}."
                    else "$away days to ${StreakState.milestoneLabel(it).lowercase()}."
                } ?: "You are past every milestone."
            },
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
    }
}

/** Shown once when a freeze was spent, or when the streak ran out. */
@Composable
fun StreakNoticeCard(
    outcome: StreakOutcome,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val froze = outcome.freezesSpent > 0
    PaperCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        fill = if (froze) MaterialTheme.colorScheme.primaryContainer else null
    ) {
        val onFill = if (froze) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(
                    if (froze) R.drawable.ic_snowflake else R.drawable.ic_flame
                ),
                contentDescription = null,
                tint = onFill,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (froze) "A freeze saved your streak" else "Your streak ended",
                style = MaterialTheme.typography.titleMedium,
                color = onFill
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (froze) {
                val n = outcome.freezesSpent
                val days = if (n == 1) "a day" else "$n days"
                "You missed $days, so ${if (n == 1) "a freeze" else "$n freezes"} covered it. " +
                    "Your ${outcome.state.current}-day streak is intact, with " +
                    "${outcome.state.freezes} left. You earn another next week."
            } else {
                "The days added up and there were no freezes left. Draw today and " +
                    "you are back at one."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (froze) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Got it",
            style = MaterialTheme.typography.labelLarge,
            color = if (froze) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .pressable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
