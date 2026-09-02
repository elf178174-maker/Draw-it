package com.drawit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Click handling with a small, spring-loaded press response. */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/** Fades and lifts content into place; [delayMillis] staggers a list of them. */
@Composable
fun Entrance(
    delayMillis: Int = 0,
    travel: Dp = 22.dp,
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
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "entrance"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * travel.toPx()
        }
    ) {
        content()
    }
}

/** Small uppercase eyebrow used above sections. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** The app's standard raised surface: warm fill, hairline outline, no heavy shadow. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    fill: Color? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val base = modifier
        .clip(shape)
        .background(fill ?: MaterialTheme.colorScheme.surfaceContainer)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
    val withClick = if (onClick != null) base.pressable(onClick = onClick) else base
    Column(modifier = withClick.padding(contentPadding), content = content)
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val background = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (enabled) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .pressable(enabled = enabled, scaleDown = 0.96f, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, tint = foreground, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = foreground)
    }
}

@Composable
fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .pressable(scaleDown = 0.96f, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    illustration: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            illustration()
            Spacer(Modifier.height(24.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

/** The app's switch: a sliding thumb rather than Material's default. */
@Composable
fun PaperSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val track by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(240),
        label = "switchTrack"
    )
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 28.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = Spring.StiffnessMedium),
        label = "switchThumb"
    )

    Box(
        modifier
            .width(56.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(track)
            .pressable(scaleDown = 0.94f) { onCheckedChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        )
    }
}

/** A one-pixel rule that matches the paper aesthetic. */
@Composable
fun HairLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}
