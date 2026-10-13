package com.drawit.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.Eyebrow
import com.drawit.app.ui.components.InkAmount
import com.drawit.app.ui.components.LabelledField
import com.drawit.app.ui.components.ScreenHeader
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.theme.DI
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AddDrawingScreen(viewModel: AppViewModel, onClose: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val reference by viewModel.todayReference.collectAsStateWithLifecycle()

    var photo by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val reward = remember { viewModel.previewReward() }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photo = pendingCameraUri else message = "No photo was taken."
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = uri
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            message = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("New drawing", onClose)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .imePadding()
            ) {
                reference?.let { ref ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DI.colors.brandSoft).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ref.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Drawing from", color = DI.colors.brand)
                            Text(ref.title.ifBlank { "Today's reference" }, style = DI.type.bodyStrong, color = DI.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                AnimatedContent(
                    targetState = photo,
                    transitionSpec = {
                        (fadeIn(tween(260)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.92f))
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "photoStage"
                ) { current ->
                    if (current == null) {
                        Column {
                            Entrance(delayMillis = 40) {
                                SourceCard(
                                    iconRes = R.drawable.ic_camera,
                                    title = "Take a photo",
                                    body = "Lay the drawing flat in good light and shoot straight on.",
                                    highlighted = true
                                ) {
                                    val uri = createCaptureUri(context)
                                    if (uri == null) {
                                        message = "Could not prepare the camera."
                                    } else {
                                        pendingCameraUri = uri
                                        runCatching { cameraLauncher.launch(uri) }.onFailure { message = "No camera app was found." }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Entrance(delayMillis = 100) {
                                SourceCard(
                                    iconRes = R.drawable.ic_gallery,
                                    title = "Choose from gallery",
                                    body = "Already photographed it? Pick it from your photos.",
                                    highlighted = false
                                ) {
                                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            if (reward > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text("This drawing earns ", style = DI.type.bodyStrong, color = DI.colors.textMuted)
                                    InkAmount(reward, size = 16.dp)
                                }
                            }
                        }
                    } else {
                        Column {
                            Box(Modifier.chunky(Chunks.plain(), corner = 22.dp, onClick = null).padding(6.dp)) {
                                AsyncImage(
                                    model = current,
                                    contentDescription = "The drawing you photographed",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.9f).clip(RoundedCornerShape(16.dp)).background(DI.colors.sunken)
                                )
                                Text(
                                    "RETAKE",
                                    style = DI.type.label,
                                    color = DI.colors.text,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .chunky(Chunks.plain(), corner = 12.dp, lip = 3.dp, onClick = { photo = null })
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            LabelledField("Name", title, { title = it }, placeholder = "Give it a name", imeAction = ImeAction.Next)
                            Spacer(Modifier.height(14.dp))
                            LabelledField("Notes (optional)", note, { note = it }, placeholder = "What were you working on? What went well?", minLines = 3)
                            Spacer(Modifier.height(20.dp))
                            ChunkyButton(
                                text = if (saving) "Saving…" else "Save drawing",
                                icon = painterResource(R.drawable.ic_check),
                                enabled = !saving,
                                chunk = Chunks.green(),
                                modifier = Modifier.fillMaxWidth(),
                                trailing = if (reward > 0 && !saving) ({ InkAmount(reward, color = Color.White, size = 16.dp) }) else null,
                                onClick = {
                                    viewModel.save(current, title, note) { ok ->
                                        if (ok) onSaved() else message = "That image could not be saved."
                                    }
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Saved with today's date and time. Everything stays on this phone.",
                                style = DI.type.small, color = DI.colors.textMuted,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(40.dp))
            }
        }

        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it },
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = message.orEmpty(),
                style = DI.type.bodyStrong,
                color = DI.colors.text,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DI.colors.surface)
                    .border(2.dp, DI.colors.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun SourceCard(iconRes: Int, title: String, body: String, highlighted: Boolean, onClick: () -> Unit) {
    val chunk = if (highlighted) Chunks.brand() else Chunks.plain()
    Row(
        Modifier.fillMaxWidth().chunky(chunk, corner = 20.dp, onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                .background(if (highlighted) Color.White.copy(alpha = 0.22f) else DI.colors.sunken),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), null, tint = chunk.content, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = DI.type.title, color = chunk.content)
            Spacer(Modifier.height(2.dp))
            Text(body, style = DI.type.small, color = chunk.content.copy(alpha = 0.8f))
        }
    }
}

/** Creates a FileProvider URI the camera app can write the full-size photo into. */
private fun createCaptureUri(context: Context): Uri? = runCatching {
    val dir = File(context.cacheDir, "capture").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    file.createNewFile()
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}.getOrNull()
