package com.drawit.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.InkTransaction
import com.drawit.app.feedback.Feedback
import com.drawit.app.model.FlameTier
import com.drawit.app.ui.art.AvatarPose
import com.drawit.app.ui.components.AvatarView
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyCard
import com.drawit.app.ui.components.ChunkyIconButton
import com.drawit.app.ui.components.ChunkyTextField
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.Eyebrow
import com.drawit.app.ui.components.FlameView
import com.drawit.app.ui.components.InkDrop
import com.drawit.app.ui.components.TierBadge
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.formatShortDate
import com.drawit.app.ui.theme.DI
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: AppViewModel, onOpenShop: () -> Unit, onOpenSettings: () -> Unit, onOpenStreak: () -> Unit) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hop = remember { Animatable(0f) }
    val joy = remember { Animatable(0f) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", style = DI.type.display, color = DI.colors.text, modifier = Modifier.weight(1f))
            ChunkyIconButton(painterResource(R.drawable.ic_sliders), "Settings", onOpenSettings)
        }

        Entrance(delayMillis = 30) {
            ChunkyCard(padding = PaddingValues(0.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clickable(remember { MutableInteractionSource() }, null) {
                            viewModel.feedback.play(Feedback.Sound.Pop)
                            viewModel.feedback.buzz(Feedback.Buzz.Pop)
                            scope.launch {
                                launch { joy.snapTo(1f); kotlinx.coroutines.delay(700); joy.animateTo(0f, tween(200)) }
                                hop.animateTo(16f, tween(160))
                                hop.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                            }
                        }
                ) {
                    AvatarView(look, Modifier.fillMaxSize()) { AvatarPose(hop = hop.value, joy = joy.value) }
                }
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name.ifBlank { "Artist" },
                            style = DI.type.headline,
                            color = DI.colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        ChunkyIconButton(painterResource(R.drawable.ic_pencil), "Rename", { editing = true }, size = 38.dp)
                    }
                    if (profile.createdAt > 0) {
                        Text("Drawing since ${formatShortDate(profile.createdAt)}", style = DI.type.small, color = DI.colors.textMuted)
                    }
                    Spacer(Modifier.height(14.dp))
                    ChunkyButton(
                        "Customize avatar",
                        onClick = onOpenShop,
                        icon = painterResource(R.drawable.ic_shop),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Statistics", style = DI.type.headline, color = DI.colors.text)
        Spacer(Modifier.height(10.dp))
        Entrance(delayMillis = 80) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f), "${streak.current}", "Day streak", onClick = onOpenStreak) {
                        FlameView(FlameTier.forStreak(streak.current), Modifier.size(28.dp), detail = false, lit = viewModel.drewToday(streak))
                    }
                    StatBox(Modifier.weight(1f), "${streak.best}", "Best streak") {
                        Icon(painterResource(R.drawable.ic_trophy), null, tint = DI.colors.gold, modifier = Modifier.size(26.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f), "${drawings.size}", "Drawings") {
                        Icon(painterResource(R.drawable.ic_album), null, tint = DI.colors.purple, modifier = Modifier.size(26.dp))
                    }
                    StatBox(Modifier.weight(1f), "${wallet.earned}", "Ink earned") {
                        InkDrop(Modifier.size(26.dp), animate = false)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Flames collected", style = DI.type.headline, color = DI.colors.text)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FlameTier.entries.drop(1).forEach { TierBadge(it, reached = streak.best >= it.minStreak) }
        }

        if (wallet.history.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Ink history", style = DI.type.headline, color = DI.colors.text)
            Spacer(Modifier.height(10.dp))
            ChunkyCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                wallet.history.take(12).forEach { HistoryRow(it) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (editing) {
        var name by remember { mutableStateOf(profile.name) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Your name", style = DI.type.headline) },
            text = {
                ChunkyTextField(name, { name = it.take(24) }, placeholder = "What should Foxy call you?", imeAction = ImeAction.Done, onImeAction = {
                    viewModel.rename(name); editing = false
                })
            },
            confirmButton = {
                ChunkyButton("Save", onClick = { viewModel.rename(name); editing = false }, height = 44.dp)
            },
            dismissButton = {
                ChunkyButton("Cancel", onClick = { editing = false }, chunk = Chunks.plain(), height = 44.dp)
            },
            containerColor = DI.colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun StatBox(modifier: Modifier, value: String, label: String, onClick: (() -> Unit)? = null, icon: @Composable () -> Unit) {
    Row(
        modifier.chunky(Chunks.plain(), corner = 18.dp, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, style = DI.type.headline, color = DI.colors.text)
            Text(label, style = DI.type.small, color = DI.colors.textMuted)
        }
    }
}

@Composable
private fun HistoryRow(t: InkTransaction) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(t.label, style = DI.type.bodyStrong, color = DI.colors.text)
            Text(formatShortDate(t.at), style = DI.type.small, color = DI.colors.textFaint)
        }
        val positive = t.amount > 0
        Text(
            (if (positive) "+" else "") + t.amount,
            style = DI.type.number,
            color = if (positive) DI.colors.ink else DI.colors.textMuted
        )
        Spacer(Modifier.width(4.dp))
        InkDrop(Modifier.size(16.dp), animate = false)
    }
}
