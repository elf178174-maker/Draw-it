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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.OutlineButton
import com.drawit.app.ui.components.PaperCard
import com.drawit.app.ui.components.PaperSwitch
import com.drawit.app.ui.components.PrimaryButton
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.components.pressable
import com.drawit.app.ui.formatCountdown
import com.drawit.app.ui.formatTime
import com.drawit.app.ui.relativeDayLabel

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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val nextReminder = remember(settings, resumeTick) { viewModel.nextReminderAt(settings) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.statusBarsPadding().height(24.dp))

        Entrance(delayMillis = 40) {
            Column {
                SectionLabel("Set your rhythm")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Reminder",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Entrance(delayMillis = 100) {
            PaperCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel(if (settings.enabled) "Reminder on" else "Reminder off")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (settings.enabled) "You'll be nudged" else "Nothing scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    PaperSwitch(
                        checked = settings.enabled,
                        onCheckedChange = { viewModel.setSettings(settings.copy(enabled = it)) }
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
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
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .pressable(scaleDown = 0.98f) { showTimePicker = true }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Tap the time to change it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Entrance(delayMillis = 160) {
            PaperCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp)
            ) {
                SectionLabel("How often")
                Spacer(Modifier.height(14.dp))
                FrequencyToggle(
                    everyDay = settings.isEveryDay,
                    onSelect = { everyDay ->
                        val days = if (everyDay) ReminderSettings.ALL_DAYS
                        else setOf(ReminderSettings.WEEK_ORDER.first())
                        viewModel.setSettings(settings.copy(days = days))
                    }
                )

                AnimatedVisibility(
                    visible = !settings.isEveryDay,
                    enter = fadeIn(tween(240)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(240))
                ) {
                    Column {
                        Spacer(Modifier.height(20.dp))
                        DayPicker(
                            selected = settings.days,
                            onToggle = { day ->
                                val next = settings.days.toMutableSet()
                                if (!next.remove(day)) next.add(day)
                                // Keep at least one day so the schedule stays meaningful.
                                if (next.isNotEmpty()) viewModel.setSettings(settings.copy(days = next))
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        AnimatedVisibility(
            visible = settings.enabled && nextReminder != null,
            enter = fadeIn(tween(280)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(220))
        ) {
            Column {
                PaperCard(
                    modifier = Modifier.fillMaxWidth(),
                    fill = MaterialTheme.colorScheme.primaryContainer,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bell),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = nextReminder?.let {
                                "${relativeDayLabel(it)} at ${formatTime(context, it)} · ${formatCountdown(it)}"
                            }.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        if (!notificationsAllowed) {
            Entrance(delayMillis = 200) {
                NoticeCard(
                    title = "Notifications are switched off",
                    body = "Draw it needs permission to post notifications, otherwise the reminder has no way to reach you.",
                    actionLabel = "Allow notifications",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openNotificationSettings(context)
                        }
                    },
                    secondaryLabel = "Open settings",
                    onSecondary = { openNotificationSettings(context) }
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        if (notificationsAllowed && !exactAllowed) {
            Entrance(delayMillis = 220) {
                NoticeCard(
                    title = "Reminder will arrive late",
                    body = "Without permission for exact alarms, Android parks the reminder " +
                        "while the phone is idle and only delivers it when something wakes " +
                        "the app up. Turn this on and it lands on the minute.",
                    actionLabel = "Allow exact alarms",
                    onAction = { openExactAlarmSettings(context) }
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        if (notificationsAllowed && !batteryExempt) {
            Entrance(delayMillis = 230) {
                NoticeCard(
                    title = "Battery saver can silence this",
                    body = "Some phones stop waking apps in the background, which is the " +
                        "usual reason a reminder never shows up. Letting Draw it run " +
                        "unrestricted costs almost nothing — it wakes once a day.",
                    actionLabel = "Let it run",
                    onAction = { requestBatteryExemption(context) },
                    secondaryLabel = "Open settings",
                    onSecondary = { openBatterySettings(context) }
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        Entrance(delayMillis = 260) {
            PaperCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp)
            ) {
                SectionLabel("Check it works")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "The real test is whether a reminder arrives while the app is " +
                        "closed. This arms a genuine alarm 30 seconds from now — start it, " +
                        "leave the app and lock your phone. Your normal schedule is put " +
                        "back straight afterwards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = if (testArmed) "Armed — now close the app" else "Test the real alarm",
                    icon = painterResource(R.drawable.ic_clock),
                    enabled = !testArmed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.scheduleTestAlarm(30)
                    testArmed = true
                }
                Spacer(Modifier.height(12.dp))
                OutlineButton(
                    text = "Just show me the notification",
                    icon = painterResource(R.drawable.ic_bell),
                    modifier = Modifier.fillMaxWidth()
                ) { viewModel.sendTestNotification() }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (exactAllowed) {
                        "Alarms are exact, so the reminder is exempt from battery dozing. " +
                            "That is why an alarm icon sits in your status bar."
                    } else {
                        "Alarms are currently inexact and may be delayed."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(140.dp))
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
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val is24Hour = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(30.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionLabel("Remind me at")
            Spacer(Modifier.height(20.dp))
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineButton(text = "Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
                PrimaryButton(
                    text = "Set",
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(state.hour, state.minute) }
                )
            }
        }
    }
}

@Composable
private fun FrequencyToggle(everyDay: Boolean, onSelect: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val half = maxWidth / 2
        val offset by animateDpAsState(
            targetValue = if (everyDay) 0.dp else half,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "frequencyThumb"
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(half)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
        Row(Modifier.fillMaxSize()) {
            SegmentLabel("Every day", everyDay, Modifier.weight(1f)) { onSelect(true) }
            SegmentLabel("Certain days", !everyDay, Modifier.weight(1f)) { onSelect(false) }
        }
    }
}

@Composable
private fun SegmentLabel(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "segmentColor"
    )
    Box(
        modifier
            .fillMaxHeight()
            .pressable(scaleDown = 0.98f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun DayPicker(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReminderSettings.WEEK_ORDER.forEach { day ->
            DayChip(
                label = ReminderSettings.initial(day),
                selected = selected.contains(day),
                modifier = Modifier.weight(1f),
                onClick = { onToggle(day) }
            )
        }
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(220),
        label = "dayChipBackground"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "dayChipForeground"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "dayChipScale"
    )

    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .pressable(scaleDown = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
            textAlign = TextAlign.Center
        )
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
    PaperCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(text = actionLabel, onClick = onAction)
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .pressable(onClick = onSecondary)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
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
