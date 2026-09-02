package com.drawit.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.Drawing
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.PaperCard
import com.drawit.app.ui.components.pressable
import com.drawit.app.ui.components.PrimaryButton
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.formatCountdown
import com.drawit.app.ui.formatDate
import com.drawit.app.ui.formatTime
import com.drawit.app.ui.greeting
import com.drawit.app.ui.relativeDayLabel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

private val prompts = listOf(
    "Draw the first object you can reach without standing up.",
    "Fill a page with hands in five different positions.",
    "Draw the view out of the nearest window, badly and quickly.",
    "One face, no eraser, ten minutes.",
    "Draw something you ate today.",
    "Copy a small piece of a painting you like.",
    "Draw the same mug three times, each one looser.",
    "Sketch a person from memory, then check how wrong you were.",
    "Draw only shadows — no outlines allowed.",
    "Fill a page with folds of fabric.",
    "Draw your own shoes from an awkward angle.",
    "Twenty tiny thumbnails of anything, one minute each.",
    "Draw a plant leaf by leaf until you run out of patience.",
    "Draw a scene using only straight lines.",
    "Sketch someone's back while they aren't looking."
)

@Composable
fun TodayScreen(
    viewModel: AppViewModel,
    onAdd: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenInspiration: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenDrawing: (String) -> Unit
) {
    val context = LocalContext.current
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Drives the countdown so it stays honest without being a ticking clock.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    val nextReminder = remember(settings, now) { viewModel.nextReminderAt(settings) }
    val streak = remember(drawings, now) { viewModel.streak(drawings) }
    val drewToday = remember(drawings, now) { viewModel.drewToday(drawings) }
    val prompt = remember(now / 86_400_000L) {
        prompts[((now / 86_400_000L) % prompts.size).toInt()]
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Entrance(delayMillis = 60) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel(formatDate(now))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = greeting(now),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.logo_mark),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(46.dp)
                    )
                }
            }
        }

        item {
            Entrance(delayMillis = 130) {
                ReminderHeroCard(
                    enabled = settings.enabled,
                    nextReminder = nextReminder,
                    everyDay = settings.isEveryDay,
                    dayCount = settings.days.size,
                    timeLabel = formatTime(context, settings.hour, settings.minute),
                    now = now,
                    onClick = onOpenReminder
                )
            }
        }

        item {
            Entrance(delayMillis = 200) {
                StatusCard(drewToday = drewToday, streak = streak, total = drawings.size)
            }
        }

        item {
            Entrance(delayMillis = 260) {
                PaperCard(
                    modifier = Modifier.fillMaxWidth(),
                    fill = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onOpenInspiration
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(
                            "Today's nudge",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            painter = painterResource(R.drawable.ic_spark),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Need more? Browse Pinterest inside the app →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        item {
            Entrance(delayMillis = 320) {
                PrimaryButton(
                    text = if (drewToday) "Add another drawing" else "Add today's drawing",
                    icon = painterResource(R.drawable.ic_camera),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAdd
                )
            }
        }

        if (drawings.isNotEmpty()) {
            item {
                Entrance(delayMillis = 380) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Recently drawn", Modifier.weight(1f))
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .pressable { onOpenAlbum() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            item {
                Entrance(delayMillis = 420) {
                    RecentRow(
                        drawings = drawings.take(10),
                        fileFor = { viewModel.fileFor(it) },
                        onOpen = onOpenDrawing,
                        onSeeAll = onOpenAlbum
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderHeroCard(
    enabled: Boolean,
    nextReminder: Long?,
    everyDay: Boolean,
    dayCount: Int,
    timeLabel: String,
    now: Long,
    onClick: () -> Unit
) {
    PaperCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(22.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (enabled) "Next reminder" else "Reminder is off", Modifier.weight(1f))
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = enabled to (nextReminder ?: 0L),
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 3 })
                    .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(240)) { -it / 3 })
            },
            label = "reminderHero"
        ) { (isEnabled, target) ->
            Column {
                if (isEnabled && target > 0L) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${relativeDayLabel(target, now)} · ${formatCountdown(target, now)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Not set",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Pick a day and a time and the app will nudge you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (enabled) {
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (everyDay) "Every day" else "$dayCount days a week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusCard(drewToday: Boolean, streak: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        PaperCard(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(18.dp),
            fill = if (drewToday) MaterialTheme.colorScheme.primary else null
        ) {
            val tint = if (drewToday) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (drewToday) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                }
                SectionLabel(if (drewToday) "Done today" else "Streak", color = tint)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (streak > 0) "$streak" else "—",
                style = MaterialTheme.typography.displaySmall,
                color = if (drewToday) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (streak == 1) "day in a row" else "days in a row",
                style = MaterialTheme.typography.bodySmall,
                color = tint
            )
        }
        PaperCard(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(18.dp)
        ) {
            SectionLabel("In your album")
            Spacer(Modifier.height(10.dp))
            Text(
                text = "$total",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (total == 1) "drawing" else "drawings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentRow(
    drawings: List<Drawing>,
    fileFor: (Drawing) -> java.io.File,
    onOpen: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(drawings, key = { it.id }) { drawing ->
            Column(
                modifier = Modifier
                    .width(132.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .pressable { onOpen(drawing.id) }
            ) {
                AsyncImage(
                    model = fileFor(drawing),
                    contentDescription = drawing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    text = drawing.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item {
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .aspectRatio(0.82f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .pressable { onSeeAll() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
