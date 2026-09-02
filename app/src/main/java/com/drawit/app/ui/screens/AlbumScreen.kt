package com.drawit.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import com.drawit.app.ui.components.EmptyState
import com.drawit.app.ui.components.PrimaryButton
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.components.pressable
import com.drawit.app.ui.formatShortDate
import java.io.File

@Composable
fun AlbumScreen(
    viewModel: AppViewModel,
    onAdd: () -> Unit,
    onOpenDrawing: (String) -> Unit
) {
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val filtered = remember(drawings, query) {
        if (query.isBlank()) drawings
        else drawings.filter {
            it.title.contains(query, ignoreCase = true) || it.note.contains(query, ignoreCase = true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Entrance(delayMillis = 40) {
                    Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
                        SectionLabel(
                            when (drawings.size) {
                                0 -> "Nothing yet"
                                1 -> "1 drawing"
                                else -> "${drawings.size} drawings"
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Album",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (drawings.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Entrance(delayMillis = 90) {
                        SearchField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Entrance(delayMillis = 140) {
                        Column(Modifier.fillMaxWidth().padding(top = 48.dp)) {
                            if (drawings.isEmpty()) {
                                EmptyState(
                                    title = "Your album is empty",
                                    body = "Finish a drawing, photograph it, and it will live here with its date, name and notes.",
                                    illustration = { AlbumEmptyMark() },
                                    action = {
                                        PrimaryButton(
                                            text = "Add your first drawing",
                                            icon = painterResource(R.drawable.ic_camera),
                                            onClick = onAdd
                                        )
                                    }
                                )
                            } else {
                                EmptyState(
                                    title = "No matches",
                                    body = "Nothing here is called \"$query\". Try another word."
                                )
                            }
                        }
                    }
                }
            }

            items(filtered, key = { it.id }) { drawing ->
                AlbumTile(
                    drawing = drawing,
                    file = viewModel.fileFor(drawing),
                    onClick = { onOpenDrawing(drawing.id) }
                )
            }
        }

        AnimatedVisibility(
            visible = drawings.isNotEmpty(),
            enter = fadeIn(tween(320)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                Modifier
                    .statusBarsPadding()
                    .padding(top = 22.dp, end = 22.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pressable(scaleDown = 0.9f) { onAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "Add a drawing",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun AlbumTile(drawing: Drawing, file: File, onClick: () -> Unit) {
    Column(Modifier.pressable(scaleDown = 0.96f, onClick = onClick)) {
        AsyncImage(
            model = file,
            contentDescription = drawing.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = drawing.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatShortDate(drawing.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Search your drawings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(visible = value.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .pressable { onValueChange("") }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/** A quiet stack-of-frames mark for the empty album. */
@Composable
private fun AlbumEmptyMark() {
    Box(
        Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_album),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
        )
    }
}
