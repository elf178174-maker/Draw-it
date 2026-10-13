package com.drawit.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drawit.app.feedback.Feedback
import com.drawit.app.ui.theme.DI
import kotlinx.coroutines.delay

/** Sound and haptics for the UI; null in previews. */
val LocalFeedback = staticCompositionLocalOf<Feedback?> { null }

/** Face colour, lip colour and content colour for a chunky control. */
data class Chunk(val face: Color, val lip: Color, val content: Color, val border: Color = Color.Transparent)

object Chunks {
    @Composable fun brand() = Chunk(DI.colors.brand, DI.colors.brandLip, DI.colors.onBrand)
    @Composable fun ink() = Chunk(DI.colors.ink, DI.colors.inkLip, Color.White)
    @Composable fun gold() = Chunk(DI.colors.gold, DI.colors.goldLip, Color(0xFF5A3A00))
    @Composable fun ice() = Chunk(DI.colors.ice, DI.colors.iceLip, Color.White)
    @Composable fun green() = Chunk(DI.colors.green, DI.colors.greenLip, Color.White)
    @Composable fun red() = Chunk(DI.colors.red, DI.colors.redLip, Color.White)
    @Composable fun purple() = Chunk(DI.colors.purple, DI.colors.purpleLip, Color.White)
    @Composable fun plain() = Chunk(DI.colors.surface, DI.colors.lineStrong, DI.colors.text, DI.colors.line)
    @Composable fun soft() = Chunk(DI.colors.brandSoft, DI.colors.brand.copy(alpha = 0.35f), DI.colors.brand, DI.colors.brand.copy(alpha = 0.35f))
    @Composable fun disabled() = Chunk(DI.colors.sunken, DI.colors.lineStrong, DI.colors.textFaint)
}

/**
 * The house modifier for anything tappable: draws a darker lip under the
 * shape, and on press pushes the face down into it with a springy bounce
 * and a haptic tick. [lip] 0 gives a flat press that just squishes a little.
 */
@Composable
fun Modifier.chunky(
    chunk: Chunk,
    shape: Shape = RoundedCornerShape(16.dp),
    lip: Dp = 4.dp,
    corner: Dp = 16.dp,
    enabled: Boolean = true,
    borderWidth: Dp = 2.dp,
    onClick: (() -> Unit)?
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled && onClick != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "press"
    )
    val feedback = LocalFeedback.current
    var m = this
    if (onClick != null) {
        m = m.clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            feedback?.tap()
            onClick()
        }
    }
    return m
        .drawBehind {
            val lipPx = lip.toPx()
            if (lipPx > 0f) {
                drawRoundRect(
                    color = chunk.lip,
                    topLeft = Offset(0f, lipPx),
                    size = Size(size.width, size.height - lipPx),
                    cornerRadius = CornerRadius(corner.toPx(), corner.toPx())
                )
            }
        }
        .padding(bottom = lip)
        .graphicsLayer {
            translationY = lip.toPx() * press
            if (lip == 0.dp) {
                scaleX = 1f - press * 0.04f
                scaleY = 1f - press * 0.04f
            }
        }
        .clip(shape)
        .background(chunk.face)
        .then(if (chunk.border != Color.Transparent && borderWidth > 0.dp) Modifier.border(borderWidth, chunk.border, shape) else Modifier)
}

@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chunk: Chunk = Chunks.brand(),
    icon: Painter? = null,
    enabled: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    height: Dp = 52.dp
) {
    val c = if (enabled) chunk else Chunks.disabled()
    Row(
        modifier = modifier
            .chunky(c, enabled = enabled, onClick = onClick)
            .heightIn(min = height)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, tint = c.content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text.uppercase(), style = DI.type.button, color = c.content, textAlign = TextAlign.Center)
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/** A round icon button with the same lip. */
@Composable
fun ChunkyIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chunk: Chunk = Chunks.plain(),
    size: Dp = 44.dp
) {
    Box(
        modifier
            .chunky(chunk, shape = CircleShape, corner = size / 2, lip = 3.dp, onClick = onClick)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = chunk.content, modifier = Modifier.size(size * 0.46f))
    }
}

/** The standard card: white face, hairline border, chunky lip. */
@Composable
fun ChunkyCard(
    modifier: Modifier = Modifier,
    chunk: Chunk = Chunks.plain(),
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    corner: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.chunky(chunk, shape = RoundedCornerShape(corner), corner = corner, onClick = onClick).padding(padding),
        content = content
    )
}

/** Duolingo-style progress bar: chunky track, bright fill, a highlight stripe. */
@Composable
fun ChunkyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = DI.colors.flame,
    track: Color = DI.colors.sunken,
    height: Dp = 16.dp,
    animate: Boolean = true
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animate) spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow) else tween(0),
        label = "progress"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track)
    ) {
        if (animated > 0.001f) {
            Box(
                Modifier
                    .fillMaxWidth(animated.coerceAtLeast(0.06f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(height / 2))
                    .background(color)
            ) {
                Box(
                    Modifier
                        .padding(start = height * 0.45f, end = height * 0.45f, top = height * 0.22f)
                        .fillMaxWidth()
                        .height(height * 0.24f)
                        .clip(RoundedCornerShape(height))
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

/** A number whose digits roll up or down when it changes. */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    var previous by remember { mutableStateOf(value) }
    val up = value >= previous
    LaunchedEffect(value) { previous = value }
    val text = value.toString()
    Row(modifier) {
        text.forEachIndexed { index, ch ->
            // Key by position from the right so units stay units.
            val position = text.length - index
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    val dir = if (up) 1 else -1
                    (slideInVertically(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) { it * dir } + fadeIn(tween(120)))
                        .togetherWith(slideOutVertically(tween(180)) { -it * dir } + fadeOut(tween(120)))
                },
                label = "digit$position"
            ) { digit ->
                Text(digit.toString(), style = style, color = color)
            }
        }
    }
}

/** Small uppercase eyebrow text. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = DI.colors.textMuted) {
    Text(text.uppercase(), style = DI.type.label, color = color, modifier = modifier)
}

/** The chunky on/off switch. */
@Composable
fun ChunkySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (checked) DI.colors.green else DI.colors.lineStrong, tween(200), label = "track")
    val knob by animateDpAsState(
        if (checked) 24.dp else 2.dp,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "knob"
    )
    val feedback = LocalFeedback.current
    Box(
        modifier
            .size(54.dp, 32.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(remember { MutableInteractionSource() }, null) {
                feedback?.tick()
                onCheckedChange(!checked)
            }
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, track.copy(alpha = 0.5f), CircleShape)
        )
    }
}

/** A two-or-more option segmented control with a sliding chunky thumb. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val feedback = LocalFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DI.colors.sunken)
            .padding(4.dp)
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .clip(RoundedCornerShape(11.dp))
                .background(DI.colors.surface)
                .border(2.dp, DI.colors.line, RoundedCornerShape(11.dp))
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null) {
                            if (i != selected) feedback?.tick()
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label.uppercase(),
                        style = DI.type.label,
                        color = if (i == selected) DI.colors.text else DI.colors.textMuted
                    )
                }
            }
        }
    }
}

/** A coloured rounded tag, e.g. rarity labels. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier, content: Color = Color.White) {
    Text(
        text.uppercase(),
        style = DI.type.label,
        color = content,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** Fades and lifts content in; [delayMillis] staggers a list of them. */
@Composable
fun Entrance(
    delayMillis: Int = 0,
    travel: Dp = 18.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "entrance"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * travel.toPx()
        }
    ) {
        content()
    }
}

/** A simple empty-state block with an illustration slot. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    illustration: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            illustration()
            Spacer(Modifier.height(16.dp))
        }
        Text(title, style = DI.type.headline, color = DI.colors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = DI.type.body, color = DI.colors.textMuted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** A thin rule. */
@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(2.dp).background(DI.colors.line))
}
