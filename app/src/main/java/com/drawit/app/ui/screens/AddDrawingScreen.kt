package com.drawit.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.LabelledField
import com.drawit.app.ui.components.PrimaryButton
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.components.StreakCelebration
import com.drawit.app.ui.components.pressable
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AddDrawingScreen(
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()

    var photo by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photo = pendingCameraUri
        else message = "No photo was taken."
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photo = uri
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            message = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .imePadding()
        ) {
            Spacer(Modifier.statusBarsPadding().height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = "Close",
                    onClick = onClose
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    SectionLabel("New entry")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Today's drawing",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 8 })
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "photoStage"
            ) { current ->
                if (current == null) {
                    SourcePicker(
                        onCamera = {
                            val uri = createCaptureUri(context)
                            if (uri == null) {
                                message = "Could not prepare the camera."
                            } else {
                                pendingCameraUri = uri
                                runCatching { cameraLauncher.launch(uri) }
                                    .onFailure { message = "No camera app was found." }
                            }
                        },
                        onGallery = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                } else {
                    Column {
                        Box {
                            AsyncImage(
                                model = current,
                                contentDescription = "The drawing you photographed",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.86f)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
                            )
                            Text(
                                text = "Retake",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .pressable { photo = null }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        LabelledField(
                            label = "Name",
                            value = title,
                            placeholder = "Give it a name",
                            imeAction = ImeAction.Next,
                            onValueChange = { title = it }
                        )
                        Spacer(Modifier.height(18.dp))
                        LabelledField(
                            label = "Description (optional)",
                            value = note,
                            placeholder = "What were you working on? What went well?",
                            minLines = 4,
                            onValueChange = { note = it }
                        )
                        Spacer(Modifier.height(26.dp))
                        PrimaryButton(
                            text = if (saving) "Saving…" else "Save to album",
                            icon = painterResource(R.drawable.ic_check),
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.save(current, title, note) { ok ->
                                if (ok) saved = true else message = "That image could not be saved."
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Saved with today's date and time. Everything stays on this phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding().height(48.dp))
        }

        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it },
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(22.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }

        AnimatedVisibility(visible = saved, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
            val streak = celebration
            if (streak != null) {
                // The streak grew, so the save is celebrated rather than just confirmed.
                StreakCelebration(
                    streak = streak.state.current,
                    milestone = streak.milestone,
                    isRecord = streak.state.current == streak.state.best,
                    onPlayFeedback = { viewModel.playCelebration(streak) },
                    onDone = {
                        viewModel.clearCelebration()
                        onSaved()
                    }
                )
            } else {
                SavedOverlay(onDone = onSaved)
            }
        }
    }
}

@Composable
private fun SourcePicker(onCamera: () -> Unit, onGallery: () -> Unit) {
    Column {
        Entrance(delayMillis = 40) {
            SourceCard(
                iconRes = R.drawable.ic_camera,
                title = "Take a photo",
                body = "Lay the drawing flat, good light, straight on.",
                highlighted = true,
                onClick = onCamera
            )
        }
        Spacer(Modifier.height(14.dp))
        Entrance(delayMillis = 110) {
            SourceCard(
                iconRes = R.drawable.ic_gallery,
                title = "Choose from gallery",
                body = "Already photographed it? Pick it from your photos.",
                highlighted = false,
                onClick = onGallery
            )
        }
    }
}

@Composable
private fun SourceCard(
    iconRes: Int,
    title: String,
    body: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val fill = if (highlighted) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
    val onFill = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .pressable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else onFill,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = onFill)
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = onFill.copy(alpha = 0.72f)
            )
        }
    }
}

/** Confirmation moment: a checkmark drawn stroke-by-stroke, then out. */
@Composable
private fun SavedOverlay(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val ring = remember { Animatable(0.7f) }
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        ring.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        delay(520)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = ring.value
                        scaleY = ring.value
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(48.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.12f, size.height * 0.53f)
                        lineTo(size.width * 0.40f, size.height * 0.80f)
                        lineTo(size.width * 0.88f, size.height * 0.22f)
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val visible = Path()
                    measure.getSegment(0f, measure.length * progress.value, visible, true)
                    drawPath(
                        path = visible,
                        color = accent,
                        style = Stroke(width = 3.4f * density, cap = StrokeCap.Round)
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Saved to your album",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
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
