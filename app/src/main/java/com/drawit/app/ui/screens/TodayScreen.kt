package com.drawit.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.Drawing
import com.drawit.app.data.Reference
import com.drawit.app.ui.art.FoxyPose
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyCard
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.Eyebrow
import com.drawit.app.ui.components.FlameView
import com.drawit.app.ui.components.FoxyView
import com.drawit.app.ui.components.GoalProgress
import com.drawit.app.ui.components.SpeechBubble
import com.drawit.app.ui.components.StatusBar
import com.drawit.app.ui.components.WeekStrip
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.formatCountdown
import com.drawit.app.ui.formatTime
import com.drawit.app.ui.greeting
import com.drawit.app.ui.relativeDayLabel
import com.drawit.app.ui.theme.DI
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
    "Sketch someone's back while they aren't looking.",
    "Draw Foxy. He'd love that.",
    "Draw your favourite place from memory.",
    "Draw a creature that's half one animal, half another."
)

@Composable
fun TodayScreen(
    viewModel: AppViewModel,
    onAdd: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenInspiration: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenStreak: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenDrawing: (String) -> Unit
) {
    val context = LocalContext.current
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val reference by viewModel.todayReference.collectAsStateWithLifecycle()

    // Drives the countdown so it stays honest without being a ticking clock.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    // Coming back to the app is when a missed day gets paid for.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.settleStreak()
                now = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val nextReminder = remember(settings, now) { viewModel.nextReminderAt(settings) }
    val drawnDays = remember(drawings) { viewModel.drawnDays(drawings) }
    val drewToday = remember(streak, now) { viewModel.drewToday(streak) }
    val tier = viewModel.tier(streak)
    val dayIndex = now / 86_400_000L
    val prompt = remember(dayIndex) { prompts[(dayIndex % prompts.size).toInt()] }

    Column(Modifier.fillMaxSize()) {
        StatusBar(
            look = look,
            streak = streak.current,
            tier = tier,
            drewToday = drewToday,
            ink = wallet.balance,
            onProfile = onOpenProfile,
            onStreak = onOpenStreak,
            onInk = onOpenShop
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Entrance(delayMillis = 30) {
                    Text(greeting(now), style = DI.type.display, color = DI.colors.text, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }

            item {
                Entrance(delayMillis = 80) {
                    ChunkyCard(onClick = onOpenStreak, padding = PaddingValues(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FlameView(tier, Modifier.size(74.dp), lit = drewToday)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${streak.current}",
                                    style = DI.type.hero,
                                    color = if (drewToday) DI.colors.flame else DI.colors.textFaint
                                )
                                Text(
                                    if (streak.current == 1) "day streak" else "day streak",
                                    style = DI.type.title,
                                    color = if (drewToday) DI.colors.flame else DI.colors.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Eyebrow(tier.title)
                                Spacer(Modifier.height(4.dp))
                                FreezeCount(streak.freezes)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        WeekStrip(drawn = drawnDays, frozen = streak.frozenDays)
                        Spacer(Modifier.height(16.dp))
                        GoalProgress(streak.current)
                    }
                }
            }

            item {
                Entrance(delayMillis = 140) {
                    FoxyPrompt(drewToday = drewToday, streak = streak.current, onAdd = onAdd)
                }
            }

            reference?.let { ref ->
                item {
                    Entrance(delayMillis = 170) {
                        TodayReference(ref, onOpenInspiration, onClear = { viewModel.drawFromReference(null) })
                    }
                }
            }

            item {
                Entrance(delayMillis = 200) {
                    ChunkyCard(onClick = onOpenInspiration, chunk = Chunks.plain()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(DI.colors.goldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.ic_bulb), null, tint = DI.colors.goldLip, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Eyebrow("Today's prompt", Modifier.weight(1f))
                            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = DI.colors.textFaint, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(prompt, style = DI.type.headline, color = DI.colors.text)
                        Spacer(Modifier.height(8.dp))
                        Text("Need a reference? Tap to browse Pinterest.", style = DI.type.small, color = DI.colors.textMuted)
                    }
                }
            }

            item {
                Entrance(delayMillis = 240) {
                    ChunkyCard(onClick = onOpenReminder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (settings.enabled) DI.colors.iceSoft else DI.colors.sunken),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.ic_bell), null, tint = if (settings.enabled) DI.colors.ice else DI.colors.textFaint, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                if (settings.enabled && nextReminder != null) {
                                    Text("Reminder ${formatTime(context, settings.hour, settings.minute)}", style = DI.type.title, color = DI.colors.text)
                                    Text(
                                        "${relativeDayLabel(nextReminder, now)} · ${formatCountdown(nextReminder, now)}",
                                        style = DI.type.small, color = DI.colors.textMuted
                                    )
                                } else {
                                    Text("Reminder is off", style = DI.type.title, color = DI.colors.text)
                                    Text("Pick a time and Foxy will nudge you.", style = DI.type.small, color = DI.colors.textMuted)
                                }
                            }
                            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = DI.colors.textFaint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (drawings.isNotEmpty()) {
                item {
                    Entrance(delayMillis = 280) {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (drawings.size == 1) "1 drawing" else "${drawings.size} drawings",
                                style = DI.type.title, color = DI.colors.text, modifier = Modifier.weight(1f)
                            )
                            Text(
                                "SEE ALL",
                                style = DI.type.label,
                                color = DI.colors.ice,
                                modifier = Modifier.chunky(Chunks.plain(), lip = 0.dp, borderWidth = 0.dp, onClick = onOpenAlbum).padding(8.dp)
                            )
                        }
                    }
                }
                item {
                    Entrance(delayMillis = 320) {
                        RecentRow(drawings.take(10), { viewModel.fileFor(it) }, onOpenDrawing)
                    }
                }
            }
        }
    }
}

@Composable
private fun FreezeCount(count: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(DI.colors.iceSoft).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_snowflake), null, tint = DI.colors.ice, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("$count", style = DI.type.label, color = DI.colors.iceLip)
    }
}

@Composable
private fun FoxyPrompt(drewToday: Boolean, streak: Int, onAdd: () -> Unit) {
    ChunkyCard(chunk = if (drewToday) Chunks.plain() else Chunks.soft(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            FoxyView(Modifier.size(110.dp), fit = 0.9f) {
                if (drewToday) FoxyPose(cheer = 0.35f) else FoxyPose(wave = 1f)
            }
            Spacer(Modifier.width(4.dp))
            SpeechBubble(
                when {
                    drewToday -> "Streak safe! I'll see you tomorrow."
                    streak >= 2 -> "Your $streak-day flame is waiting. Draw something!"
                    streak == 1 -> "Draw today and your streak hits 2!"
                    else -> "Let's light a new flame. One drawing does it!"
                },
                modifier = Modifier.weight(1f).padding(bottom = 30.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        ChunkyButton(
            text = if (drewToday) "Add another drawing" else "Draw today's page",
            icon = painterResource(R.drawable.ic_camera),
            chunk = if (drewToday) Chunks.plain() else Chunks.brand(),
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TodayReference(ref: Reference, onOpen: () -> Unit, onClear: () -> Unit) {
    ChunkyCard(onClick = onOpen, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ref.imageUrl,
                contentDescription = ref.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)).background(DI.colors.sunken)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Today's reference", color = DI.colors.brand)
                Spacer(Modifier.height(4.dp))
                Text(ref.title.ifBlank { "From Pinterest" }, style = DI.type.title, color = DI.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                painterResource(R.drawable.ic_close), "Clear reference", tint = DI.colors.textFaint,
                modifier = Modifier.size(32.dp).chunky(Chunks.plain(), lip = 0.dp, borderWidth = 0.dp, onClick = onClear).padding(6.dp)
            )
        }
    }
}

@Composable
private fun RecentRow(drawings: List<Drawing>, fileFor: (Drawing) -> java.io.File, onOpen: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(drawings, key = { it.id }) { drawing ->
            Column(Modifier.width(128.dp).chunky(Chunks.plain(), corner = 18.dp, onClick = { onOpen(drawing.id) }).padding(6.dp)) {
                AsyncImage(
                    model = fileFor(drawing),
                    contentDescription = drawing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(12.dp)).background(DI.colors.sunken)
                )
                Text(
                    drawing.title, style = DI.type.small, color = DI.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp)
                )
            }
        }
    }
}
