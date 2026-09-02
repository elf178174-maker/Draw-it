package com.drawit.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drawit.app.AppViewModel
import com.drawit.app.BuildConfig
import com.drawit.app.R
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.OutlineButton
import com.drawit.app.ui.components.PaperCard
import com.drawit.app.ui.components.PaperSwitch
import com.drawit.app.ui.components.PrimaryButton
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.components.pressable

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val feedback by viewModel.feedbackSettings.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val busy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val message by viewModel.backupMessage.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) viewModel.exportAlbum(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importAlbum(uri) }

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
                SectionLabel("Draw it")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Entrance(delayMillis = 90) {
            PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                SectionLabel("Celebration")
                Spacer(Modifier.height(16.dp))
                ToggleRow(
                    title = "Sound",
                    body = "A short chime when your streak grows.",
                    checked = feedback.soundEnabled
                ) { viewModel.setFeedbackSettings(feedback.copy(soundEnabled = it)) }
                Spacer(Modifier.height(18.dp))
                ToggleRow(
                    title = "Vibration",
                    body = "A buzz that builds as the number lands.",
                    checked = feedback.hapticsEnabled
                ) {
                    viewModel.setFeedbackSettings(feedback.copy(hapticsEnabled = it))
                    if (it) viewModel.tick()
                }
                Spacer(Modifier.height(18.dp))
                OutlineButton(
                    text = "Try it",
                    icon = painterResource(R.drawable.ic_flame),
                    modifier = Modifier.fillMaxWidth()
                ) { viewModel.previewCelebration() }
            }
        }

        Spacer(Modifier.height(16.dp))

        Entrance(delayMillis = 140) {
            PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                SectionLabel("Your album")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Export writes every drawing, its name, its date and your streak " +
                        "into a single .zip you can keep anywhere. Import reads one back — " +
                        "drawings you already have are skipped, so importing twice is safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))

                if (busy) {
                    BusyLine()
                    Spacer(Modifier.height(18.dp))
                }

                PrimaryButton(
                    text = if (drawings.isEmpty()) "Nothing to export yet" else "Export album",
                    icon = painterResource(R.drawable.ic_export),
                    enabled = drawings.isNotEmpty() && !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { exportLauncher.launch(viewModel.suggestedBackupName()) }

                Spacer(Modifier.height(12.dp))

                OutlineButton(
                    text = "Import from a backup",
                    icon = painterResource(R.drawable.ic_import),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!busy) {
                        importLauncher.launch(
                            arrayOf("application/zip", "application/octet-stream", "*/*")
                        )
                    }
                }

                if (message != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .pressable { viewModel.clearBackupMessage() }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Entrance(delayMillis = 190) {
            PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                SectionLabel("About")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Draw it ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your drawings, names, dates and streak live on this phone only. " +
                        "Nothing is uploaded, and there is no account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(140.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        PaperSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A quiet indeterminate sweep while a zip is read or written. */
@Composable
private fun BusyLine() {
    val transition = rememberInfiniteTransition(label = "busy")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "busyPhase"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(
            Modifier
                .weight(1f)
                .height(3.dp)
        ) {
            drawLine(
                color = track,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
            val span = size.width * 0.34f
            val start = (size.width + span) * phase - span
            drawLine(
                color = accent,
                start = androidx.compose.ui.geometry.Offset(start.coerceAtLeast(0f), size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(
                    (start + span).coerceAtMost(size.width), size.height / 2f
                ),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "Working…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
