package com.drawit.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.ReminderSettings
import com.drawit.app.ui.art.FoxyPose
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyCard
import com.drawit.app.ui.components.ChunkySwitch
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.Eyebrow
import com.drawit.app.ui.components.FoxyView
import com.drawit.app.ui.components.Segmented
import com.drawit.app.ui.components.SpeechBubble
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.formatCountdown
import com.drawit.app.ui.formatTime
import com.drawit.app.ui.relativeDayLabel
import com.drawit.app.ui.theme.DI

@Composable
fun ReminderScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    // Permission state can change while the user is away in system settings.
    val resumeTick = rememberResumeTick()
    val notificationsAllowed = remember(resumeTick) { viewModel.hasNotificationPermission() }
    val exactAllowed = remember(resumeTick) { viewModel.canScheduleExact() }
    val batteryExempt = remember(resumeTick) { viewModel.isBatteryExempt() }
    var testArmed by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val nextReminder = remember(settings, resumeTick) { viewModel.nextReminderAt(settings) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Reminder", style = DI.type.display, color = DI.colors.text)
        Text("Foxy taps you on the shoulder at your drawing time.", style = DI.type.bodyStrong, color = DI.colors.textMuted)
        Spacer(Modifier.height(16.dp))

        Entrance(delayMillis = 40) {
            ChunkyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(if (settings.enabled) "Reminder on" else "Reminder off")
                        Spacer(Modifier.height(2.dp))
                        Text(if (settings.enabled) "You'll be nudged" else "Nothing scheduled", style = DI.type.title, color = DI.colors.text)
                    }
                    ChunkySwitch(checked = settings.enabled, onCheckedChange = { viewModel.setSettings(settings.copy(enabled = it)) })
                }
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    targetState = settings.hour to settings.minute,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 2 })
                            .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(240)) { -it / 2 })
                    },
                    label = "timeDisplay"
                ) { (hour, minute) ->
                    Text(
                        text = formatTime(context, hour, minute),
                        style = DI.type.hero,
                        color = if (settings.enabled) DI.colors.text else DI.colors.textFaint,
                        modifier = Modifier.chunky(Chunks.plain(), lip = 0.dp, borderWidth = 0.dp, corner = 12.dp, onClick = { showTimePicker = true })
                    )
                }
                Text("Tap the time to change it", style = DI.type.small, color = DI.colors.textMuted)
            }
        }

        Spacer(Modifier.height(14.dp))
        Entrance(delayMillis = 90) {
            ChunkyCard {
                Eyebrow("How often")
                Spacer(Modifier.height(12.dp))
                Segmented(
                    options = listOf("Every day", "Certain days"),
                    selected = if (settings.isEveryDay) 0 else 1,
                    onSelect = { i ->
                        val everyDay = i == 0
                        if (everyDay != settings.isEveryDay) {
                            val days = if (everyDay) ReminderSettings.ALL_DAYS else setOf(ReminderSettings.WEEK_ORDER.first())
                            viewModel.setSettings(settings.copy(days = days))
                        }
                    }
                )
                AnimatedVisibility(
                    visible = !settings.isEveryDay,
                    enter = fadeIn(tween(240)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(240))
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReminderSettings.WEEK_ORDER.forEach { day ->
                                DayChip(
                                    label = ReminderSettings.initial(day),
                                    selected = settings.days.contains(day),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val next = settings.days.toMutableSet()
                                    if (!next.remove(day)) next.add(day)
                                    // Keep at least one day so the schedule stays meaningful.
                                    if (next.isNotEmpty()) viewModel.setSettings(settings.copy(days = next))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        AnimatedVisibility(
            visible = settings.enabled && nextReminder != null,
            enter = fadeIn(tween(280)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(220))
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 14.dp)) {
                FoxyView(Modifier.size(96.dp), fit = 0.9f) { FoxyPose(wave = 1f) }
                SpeechBubble(
                    nextReminder?.let { "See you ${relativeDayLabel(it).lowercase()} at ${formatTime(context, it)}! That's ${formatCountdown(it)}." }.orEmpty(),
                    modifier = Modifier.weight(1f).padding(bottom = 26.dp)
                )
            }
        }

        if (!notificationsAllowed) {
            NoticeCard(
                title = "Notifications are switched off",
                body = "Draw it needs permission to post notifications, otherwise the reminder has no way to reach you.",
                actionLabel = "Allow notifications",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else openNotificationSettings(context)
                },
                secondaryLabel = "Open settings",
                onSecondary = { openNotificationSettings(context) }
            )
            Spacer(Modifier.height(14.dp))
        }

        if (notificationsAllowed && !exactAllowed) {
            NoticeCard(
                title = "Reminder will arrive late",
                body = "Without permission for exact alarms, Android parks the reminder while the phone is idle " +
                    "and only delivers it when something wakes the app up. Turn this on and it lands on the minute.",
                actionLabel = "Allow exact alarms",
                onAction = { openExactAlarmSettings(context) }
            )
            Spacer(Modifier.height(14.dp))
        }

        if (notificationsAllowed && !batteryExempt) {
            NoticeCard(
                title = "Battery saver can silence this",
                body = "Some phones stop waking apps in the background, which is the usual reason a reminder never " +
                    "shows up. Letting Draw it run unrestricted costs almost nothing — it wakes once a day.",
                actionLabel = "Let it run",
                onAction = { requestBatteryExemption(context) },
                secondaryLabel = "Open settings",
                onSecondary = { openBatterySettings(context) }
            )
            Spacer(Modifier.height(14.dp))
        }

        ChunkyCard {
            Eyebrow("Check it works")
            Spacer(Modifier.height(10.dp))
            Text(
                "The real test is whether a reminder arrives while the app is closed. This arms a genuine alarm " +
                    "30 seconds from now — start it, leave the app and lock your phone. Your normal schedule is " +
                    "put back straight afterwards.",
                style = DI.type.body, color = DI.colors.textMuted
            )
            Spacer(Modifier.height(14.dp))
            ChunkyButton(
                text = if (testArmed) "Armed — now close the app" else "Test the real alarm",
                icon = painterResource(R.drawable.ic_clock),
                enabled = !testArmed,
                chunk = Chunks.ice(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.scheduleTestAlarm(30)
                    testArmed = true
                }
            )
            Spacer(Modifier.height(10.dp))
            ChunkyButton(
                text = "Just show me the notification",
                icon = painterResource(R.drawable.ic_bell),
                chunk = Chunks.plain(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.sendTestNotification() }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (exactAllowed) "Alarms are exact, so the reminder is exempt from battery dozing. That is why an alarm icon sits in your status bar."
                else "Alarms are currently inexact and may be delayed.",
                style = DI.type.small, color = DI.colors.textMuted
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.hour,
            initialMinute = settings.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                showTimePicker = false
                viewModel.setSettings(settings.copy(hour = hour, minute = minute))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val is24Hour = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = is24Hour)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(DI.colors.surface).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Eyebrow("Remind me at")
            Spacer(Modifier.height(18.dp))
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = DI.colors.sunken,
                    selectorColor = DI.colors.brand,
                    containerColor = DI.colors.surface,
                    periodSelectorSelectedContainerColor = DI.colors.brandSoft,
                    periodSelectorSelectedContentColor = DI.colors.brand,
                    timeSelectorSelectedContainerColor = DI.colors.brandSoft,
                    timeSelectorSelectedContentColor = DI.colors.brand
                )
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton("Cancel", onDismiss, chunk = Chunks.plain(), modifier = Modifier.weight(1f))
                ChunkyButton("Set", { onConfirm(state.hour, state.minute) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val chunk = if (selected) Chunks.brand() else Chunks.plain()
    Box(
        modifier.aspectRatio(1f).chunky(chunk, shape = CircleShape, corner = 100.dp, lip = 3.dp, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = DI.type.label, color = chunk.content, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NoticeCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    ChunkyCard(chunk = Chunks.soft()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_bell), null, tint = DI.colors.brand, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, style = DI.type.title, color = DI.colors.text)
        }
        Spacer(Modifier.height(8.dp))
        Text(body, style = DI.type.body, color = DI.colors.textMuted)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton(actionLabel, onAction, height = 46.dp)
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    secondaryLabel.uppercase(),
                    style = DI.type.label,
                    color = DI.colors.brand,
                    modifier = Modifier.chunky(Chunks.plain(), lip = 0.dp, borderWidth = 0.dp, onClick = onSecondary).padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/** Increments whenever this screen comes back to the foreground. */
@Composable
private fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return tick
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        .setData(Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

@SuppressLint("BatteryLife")
private fun requestBatteryExemption(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openBatterySettings(context) }
}

private fun openBatterySettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
