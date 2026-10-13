package com.drawit.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyIconButton
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.Eyebrow
import com.drawit.app.ui.components.LabelledField
import com.drawit.app.ui.components.ScreenHeader
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.formatDate
import com.drawit.app.ui.formatTime
import com.drawit.app.ui.theme.DI

@Composable
fun DrawingDetailScreen(viewModel: AppViewModel, drawingId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val drawing = drawings.firstOrNull { it.id == drawingId }

    var editing by remember { mutableStateOf(false) }
    var title by remember(drawing?.id) { mutableStateOf(drawing?.title.orEmpty()) }
    var note by remember(drawing?.id) { mutableStateOf(drawing?.note.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf(false) }

    if (drawing == null) {
        // The drawing was deleted while this screen was open.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This drawing is gone.", style = DI.type.bodyStrong, color = DI.colors.textMuted)
        }
        return
    }

    BackHandler(enabled = zoomed) { zoomed = false }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(if (editing) "Edit drawing" else "Drawing", onBack) {
                ChunkyIconButton(
                    painterResource(if (editing) R.drawable.ic_close else R.drawable.ic_pencil),
                    if (editing) "Cancel" else "Edit",
                    onClick = {
                        if (editing) { title = drawing.title; note = drawing.note }
                        editing = !editing
                    }
                )
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                Entrance(delayMillis = 30) {
                    Box(Modifier.chunky(Chunks.plain(), corner = 22.dp, onClick = { zoomed = true }).padding(6.dp)) {
                        AsyncImage(
                            model = viewModel.fileFor(drawing),
                            contentDescription = drawing.title,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DI.colors.sunken)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Entrance(delayMillis = 90) {
                    Column {
                        Eyebrow("${formatDate(drawing.createdAt)} · ${formatTime(context, drawing.createdAt)}")
                        Spacer(Modifier.height(8.dp))
                        if (editing) {
                            LabelledField("Name", title, { title = it }, placeholder = "Give it a name")
                            Spacer(Modifier.height(14.dp))
                            LabelledField("Description", note, { note = it }, placeholder = "What were you working on?", minLines = 4)
                            Spacer(Modifier.height(18.dp))
                            ChunkyButton(
                                "Save changes",
                                onClick = { viewModel.update(drawing.id, title, note); editing = false },
                                icon = painterResource(R.drawable.ic_check),
                                chunk = Chunks.green(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(drawing.title, style = DI.type.display, color = DI.colors.text)
                            if (drawing.note.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(drawing.note, style = DI.type.body, color = DI.colors.textMuted)
                            }
                        }
                    }
                }
                if (!editing) {
                    Spacer(Modifier.height(24.dp))
                    ChunkyButton(
                        "Delete this drawing",
                        onClick = { confirmDelete = true },
                        icon = painterResource(R.drawable.ic_trash),
                        chunk = Chunks.plain(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }

        AnimatedVisibility(visible = zoomed, enter = fadeIn(tween(200)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(DI.colors.scrim)
                    .clickable(remember { MutableInteractionSource() }, null) { zoomed = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = viewModel.fileFor(drawing),
                    contentDescription = drawing.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this drawing?", style = DI.type.headline) },
            text = { Text("The photo and its notes will be removed from your album. This cannot be undone.", style = DI.type.body) },
            confirmButton = {
                ChunkyButton("Delete", onClick = {
                    confirmDelete = false
                    viewModel.delete(drawing.id)
                    onBack()
                }, chunk = Chunks.red(), height = 44.dp)
            },
            dismissButton = { ChunkyButton("Keep it", onClick = { confirmDelete = false }, chunk = Chunks.plain(), height = 44.dp) },
            containerColor = DI.colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
