package com.drawit.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.StreakState
import com.drawit.app.ui.components.DayMark
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.FreezePips
import com.drawit.app.ui.components.PaperCard
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.components.WeekStrip
import com.drawit.app.ui.components.markFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val HISTORY_WEEKS = 10

@Composable
fun StreakScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val state by viewModel.streak.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val drawn = remember(drawings) { viewModel.drawnDays(drawings) }
    val today = remember { LocalDate.now() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.statusBarsPadding().height(16.dp))

        CircleIconButton(
            iconRes = R.drawable.ic_arrow_left,
            contentDescription = "Back",
            onClick = onBack
        )

        Spacer(Modifier.height(20.dp))

        Entrance(delayMillis = 40) {
            Column {
                SectionLabel("Your streak")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlameBadge(active = state.hasStreak)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "${state.current}",
                            fontSize = 62.sp,
                            lineHeight = 66.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (state.current == 1) "day in a row" else "days in a row",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Entrance(delayMillis = 100) {
            PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                SectionLabel("This week")
                Spacer(Modifier.height(14.dp))
                WeekStrip(drawn = drawn, frozen = state.frozenDays, today = today)
            }
        }

        Spacer(Modifier.height(16.dp))

        Entrance(delayMillis = 150) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatTile("Longest", "${state.best}", if (state.best == 1) "day" else "days", Modifier.weight(1f))
                StatTile("Drawings", "${drawings.size}", "saved", Modifier.weight(1f))
                StatTile("Freezes used", "${state.freezesUsedTotal}", "so far", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))

        Entrance(delayMillis = 200) {
            PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Streak freezes", Modifier.weight(1f))
                    FreezePips(available = state.freezes)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = buildString {
                        append(
                            when (state.freezes) {
                                0 -> "No freezes banked right now."
                                1 -> "One freeze banked."
                                else -> "${state.freezes} freezes banked."
                            }
                        )
                        append(" You earn one every week, up to ${StreakState.MAX_FREEZES}. ")
                        append("Miss a day and a freeze is spent automatically — your streak holds, ")
                        append("it just doesn't grow that day.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = nextFreezeLine(state, today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Entrance(delayMillis = 250) {
            PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                SectionLabel("Last $HISTORY_WEEKS weeks")
                Spacer(Modifier.height(16.dp))
                HistoryGrid(drawn = drawn, frozen = state.frozenDays, today = today)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(MaterialTheme.colorScheme.primary, "drew")
                    LegendDot(MaterialTheme.colorScheme.primaryContainer, "frozen")
                    LegendDot(MaterialTheme.colorScheme.surfaceVariant, "missed")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Entrance(delayMillis = 300) {
            PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                SectionLabel("Milestones")
                Spacer(Modifier.height(14.dp))
                StreakState.MILESTONES.forEachIndexed { index, milestone ->
                    MilestoneRow(
                        milestone = milestone,
                        reached = state.best >= milestone,
                        current = state.current
                    )
                    if (index != StreakState.MILESTONES.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(140.dp))
    }
}

@Composable
private fun FlameBadge(active: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
        label = "flameBadge"
    )
    Box(
        Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flame),
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, modifier: Modifier) {
    PaperCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryGrid(drawn: Set<Long>, frozen: Set<Long>, today: LocalDate) {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (HISTORY_WEEKS - 1 downTo 0).forEach { weeksAgo ->
            val monday = thisMonday.minusWeeks(weeksAgo.toLong())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..6).forEach { offset ->
                    val day = monday.plusDays(offset.toLong())
                    HistoryCell(markFor(day, today, drawn, frozen), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HistoryCell(mark: DayMark, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val color = when (mark) {
        DayMark.Drawn -> scheme.primary
        DayMark.Frozen -> scheme.primaryContainer
        DayMark.Today -> Color.Transparent
        DayMark.Future -> Color.Transparent
        DayMark.Missed -> scheme.surfaceVariant
    }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(5.dp))
            .background(color)
    ) {
        if (mark == DayMark.Today || mark == DayMark.Future) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (mark == DayMark.Today) scheme.primary.copy(alpha = 0.22f)
                        else scheme.surfaceVariant.copy(alpha = 0.45f)
                    )
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MilestoneRow(milestone: Int, reached: Boolean, current: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (reached) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (reached) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = StreakState.milestoneLabel(milestone),
            style = MaterialTheme.typography.bodyMedium,
            color = if (reached) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = when {
                reached -> "done"
                current > 0 -> "${milestone - current} to go"
                else -> "$milestone days"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (reached) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun nextFreezeLine(state: StreakState, today: LocalDate): String {
    if (state.freezes >= StreakState.MAX_FREEZES) return "You are holding as many as you can."
    val nextMonday = today.with(DayOfWeek.MONDAY).plusWeeks(1)
    val days = ChronoUnit.DAYS.between(today, nextMonday)
    return when (days) {
        1L -> "Another arrives tomorrow."
        else -> "Another arrives in $days days."
    }
}
