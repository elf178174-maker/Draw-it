package com.drawit.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.LabelledField
import com.drawit.app.ui.components.OutlineButton
import com.drawit.app.ui.components.PrimaryButton
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.components.pressable
import com.drawit.app.ui.formatDate
import com.drawit.app.ui.formatTime
import androidx.compose.ui.platform.LocalContext

@Composable
fun DrawingDetailScreen(
    viewModel: AppViewModel,
    drawingId: String,
    onBack: () -> Unit
) {
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
            Text(
                text = "This drawing is gone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    BackHandler(enabled = zoomed) { zoomed = false }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 40.dp)
        ) {
            Spacer(Modifier.statusBarsPadding().height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    iconRes = R.drawable.ic_arrow_left,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (editing) "Cancel" else "Edit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .pressable {
                            if (editing) {
                                title = drawing.title
                                note = drawing.note
                            }
                            editing = !editing
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Entrance(delayMillis = 40) {
                AsyncImage(
                    model = viewModel.fileFor(drawing),
                    contentDescription = drawing.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .pressable(scaleDown = 0.985f) { zoomed = true }
                )
            }

            Spacer(Modifier.height(22.dp))

            Entrance(delayMillis = 110) {
                Column {
                    SectionLabel("${formatDate(drawing.createdAt)} · ${formatTime(context, drawing.createdAt)}")
                    Spacer(Modifier.height(10.dp))

                    if (editing) {
                        LabelledField(
                            label = "Name",
                            value = title,
                            placeholder = "Give it a name",
                            onValueChange = { title = it }
                        )
                        Spacer(Modifier.height(14.dp))
                        LabelledField(
                            label = "Description",
                            value = note,
                            placeholder = "What were you working on?",
                            minLines = 4,
                            onValueChange = { note = it }
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            text = "Save changes",
                            icon = painterResource(R.drawable.ic_check),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.update(drawing.id, title, note)
                            editing = false
                        }
                    } else {
                        Text(
                            text = drawing.title,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (drawing.note.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = drawing.note,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!editing) {
                Spacer(Modifier.height(28.dp))
                Entrance(delayMillis = 180) {
                    OutlineButton(
                        text = "Delete this drawing",
                        icon = painterResource(R.drawable.ic_trash),
                        modifier = Modifier.fillMaxWidth()
                    ) { confirmDelete = true }
                }
            }

            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }

        AnimatedVisibility(
            visible = zoomed,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200))
        ) {
            ZoomOverlay(
                model = viewModel.fileFor(drawing),
                description = drawing.title,
                onDismiss = { zoomed = false }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this drawing?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "The photo and its notes will be removed from your album. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(drawing.id)
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep it") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(26.dp)
        )
    }
}

@Composable
private fun ZoomOverlay(model: Any, description: String, onDismiss: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "zoom"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .pressable(scaleDown = 1f) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Composable
fun CircleIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    rotate: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .pressable(scaleDown = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer { rotationZ = rotate }
        )
    }
}
