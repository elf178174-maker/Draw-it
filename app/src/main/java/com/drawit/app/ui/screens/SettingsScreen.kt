package com.drawit.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drawit.app.AppViewModel
import com.drawit.app.BuildConfig
import com.drawit.app.R
import com.drawit.app.feedback.Feedback.Buzz
import com.drawit.app.feedback.Feedback.Sound
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyCard
import com.drawit.app.ui.components.ChunkySwitch
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Eyebrow
import com.drawit.app.ui.components.ScreenHeader
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.theme.DI

private val soundCheck = listOf(
    "Saved" to (Sound.Ding to Buzz.Success),
    "Ignite" to (Sound.Ignite to Buzz.Thud),
    "Streak" to (Sound.Streak to Buzz.Success),
    "Ink" to (Sound.Coin to Buzz.Tick),
    "Goal" to (Sound.Fanfare to Buzz.Big),
    "Evolve" to (Sound.EvolveBoom to Buzz.Big),
    "Buy" to (Sound.Purchase to Buzz.Success),
    "Unlock" to (Sound.Unlock to Buzz.Success),
    "Freeze" to (Sound.Freeze to Buzz.Soft),
    "Oh no" to (Sound.Lost to Buzz.Soft)
)

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val feedback by viewModel.feedbackSettings.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val busy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val message by viewModel.backupMessage.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.exportAlbum(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importAlbum(uri)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            ChunkyCard {
                Eyebrow("Sound and feel")
                Spacer(Modifier.height(14.dp))
                ToggleRow("Sound effects", "Chimes, whooshes and fanfares when you draw.", feedback.soundEnabled) {
                    viewModel.setFeedbackSettings(feedback.copy(soundEnabled = it))
                }
                Spacer(Modifier.height(16.dp))
                ToggleRow("Vibration", "Taps and thumps you can feel.", feedback.hapticsEnabled) {
                    viewModel.setFeedbackSettings(feedback.copy(hapticsEnabled = it))
                    if (it) viewModel.feedback.tick()
                }
                Spacer(Modifier.height(16.dp))
                Text("Sound check", style = DI.type.title, color = DI.colors.text)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    soundCheck.forEach { (label, pair) ->
                        Text(
                            label.uppercase(),
                            style = DI.type.label,
                            color = DI.colors.text,
                            modifier = Modifier
                                .chunky(Chunks.plain(), shape = RoundedCornerShape(12.dp), corner = 12.dp, lip = 3.dp, onClick = {
                                    viewModel.feedback.play(pair.first)
                                    viewModel.feedback.buzz(pair.second)
                                })
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            ChunkyCard {
                Eyebrow("Backup")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Export saves every drawing with its name and date, plus your streak, ink, avatar and saved " +
                        "references, into one .zip you can keep anywhere. Import reads one back — drawings you " +
                        "already have are skipped, so importing twice is safe. Backups made by the older version " +
                        "of Draw it still import, and their drawings earn back their ink.",
                    style = DI.type.body, color = DI.colors.textMuted
                )
                Spacer(Modifier.height(14.dp))
                if (busy) {
                    BusyLine()
                    Spacer(Modifier.height(14.dp))
                }
                ChunkyButton(
                    if (drawings.isEmpty()) "Nothing to export yet" else "Export everything",
                    onClick = { exportLauncher.launch(viewModel.suggestedBackupName()) },
                    icon = painterResource(R.drawable.ic_export),
                    enabled = drawings.isNotEmpty() && !busy,
                    chunk = Chunks.ink(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                ChunkyButton(
                    "Import a backup",
                    onClick = { if (!busy) importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    icon = painterResource(R.drawable.ic_import),
                    chunk = Chunks.plain(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (message != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(message.orEmpty(), style = DI.type.bodyStrong, color = DI.colors.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "DISMISS",
                        style = DI.type.label,
                        color = DI.colors.textMuted,
                        modifier = Modifier.chunky(Chunks.plain(), lip = 0.dp, borderWidth = 0.dp, onClick = { viewModel.clearBackupMessage() }).padding(vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            ChunkyCard {
                Eyebrow("About")
                Spacer(Modifier.height(10.dp))
                Text("Draw it ${BuildConfig.VERSION_NAME}", style = DI.type.title, color = DI.colors.text)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your drawings, streak, ink and avatar live on this phone only. Nothing is uploaded and there " +
                        "is no account. The inspiration feed loads pictures from Pinterest when you open it.",
                    style = DI.type.body, color = DI.colors.textMuted
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Text is set in Nunito, used under the SIL Open Font License. Sounds and art are made for this app.",
                    style = DI.type.small, color = DI.colors.textFaint
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(32.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DI.type.title, color = DI.colors.text)
            Text(body, style = DI.type.small, color = DI.colors.textMuted)
        }
        Spacer(Modifier.width(14.dp))
        ChunkySwitch(checked, onCheckedChange)
    }
}

/** A quiet indeterminate sweep while a zip is read or written. */
@Composable
private fun BusyLine() {
    val phase by rememberInfiniteTransition(label = "busy").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "busyPhase"
    )
    val track = DI.colors.sunken
    val accent = DI.colors.ink
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val y = size.height / 2f
        drawLine(track, Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
        val span = size.width * 0.34f
        val start = (size.width + span) * phase - span
        drawLine(accent, Offset(start.coerceAtLeast(0f), y), Offset((start + span).coerceAtMost(size.width), y), size.height, StrokeCap.Round)
    }
}
