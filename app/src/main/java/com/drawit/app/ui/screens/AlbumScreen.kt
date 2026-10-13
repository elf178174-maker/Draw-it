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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.Drawing
import com.drawit.app.ui.art.FoxyPose
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyIconButton
import com.drawit.app.ui.components.ChunkyTextField
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.EmptyState
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.FoxyView
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.formatShortDate
import com.drawit.app.ui.theme.DI
import java.io.File

@Composable
fun AlbumScreen(viewModel: AppViewModel, onAdd: () -> Unit, onOpenDrawing: (String) -> Unit) {
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filtered = remember(drawings, query) {
        if (query.isBlank()) drawings
        else drawings.filter { it.title.contains(query, ignoreCase = true) || it.note.contains(query, ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                    Text("Album", style = DI.type.display, color = DI.colors.text)
                    Text(
                        when (drawings.size) { 0 -> "Nothing yet"; 1 -> "1 drawing"; else -> "${drawings.size} drawings" },
                        style = DI.type.bodyStrong, color = DI.colors.textMuted
                    )
                }
            }
            if (drawings.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_search), null, tint = DI.colors.textFaint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        ChunkyTextField(query, { query = it }, placeholder = "Search your drawings", modifier = Modifier.weight(1f))
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Entrance(delayMillis = 80) {
                        Column(Modifier.fillMaxWidth().padding(top = 32.dp)) {
                            if (drawings.isEmpty()) {
                                EmptyState(
                                    title = "Your album is empty",
                                    body = "Finish a drawing, take a photo of it, and it lives here with its date, name and notes.",
                                    illustration = { FoxyView(Modifier.size(170.dp), fit = 0.9f) { FoxyPose(sleepy = 0.6f, lookY = 0.4f, lookWeight = 0.5f) } },
                                    action = {
                                        ChunkyButton("Add your first drawing", onAdd, icon = painterResource(R.drawable.ic_camera))
                                    }
                                )
                            } else {
                                EmptyState(
                                    title = "No matches",
                                    body = "Nothing here is called \"$query\". Try another word.",
                                    illustration = { FoxyView(Modifier.size(140.dp), fit = 0.9f) { FoxyPose(surprise = 0.6f) } }
                                )
                            }
                        }
                    }
                }
            }
            items(filtered, key = { it.id }) { drawing ->
                AlbumTile(drawing, viewModel.fileFor(drawing)) { onOpenDrawing(drawing.id) }
            }
        }

        if (drawings.isNotEmpty()) {
            ChunkyIconButton(
                painterResource(R.drawable.ic_plus), "Add a drawing", onAdd,
                chunk = Chunks.brand(), size = 60.dp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            )
        }
    }
}

@Composable
private fun AlbumTile(drawing: Drawing, file: File, onClick: () -> Unit) {
    Column(Modifier.chunky(Chunks.plain(), corner = 18.dp, onClick = onClick).padding(6.dp)) {
        AsyncImage(
            model = file,
            contentDescription = drawing.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(13.dp)).background(DI.colors.sunken)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            drawing.title, style = DI.type.bodyStrong, color = DI.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(formatShortDate(drawing.createdAt), style = DI.type.small, color = DI.colors.textMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}
