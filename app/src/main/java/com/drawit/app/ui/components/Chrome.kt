package com.drawit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drawit.app.model.FlameTier
import com.drawit.app.ui.art.AvatarLook
import com.drawit.app.ui.theme.DI

/**
 * The bar across the top of every main screen, Duolingo style: your avatar,
 * the streak flame (grey until today's drawing is in) and your ink.
 */
@Composable
fun StatusBar(
    look: AvatarLook,
    streak: Int,
    tier: FlameTier,
    drewToday: Boolean,
    ink: Int,
    onProfile: () -> Unit,
    onStreak: () -> Unit,
    onInk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, DI.colors.line, CircleShape)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onProfile)
        ) {
            AvatarView(look, Modifier.size(40.dp), animate = false)
        }
        Spacer(Modifier.weight(1f))
        StatPill(onClick = onStreak) {
            FlameView(tier, Modifier.size(26.dp), lit = drewToday, detail = false)
            Spacer(Modifier.width(4.dp))
            RollingNumber(streak, DI.type.number, if (drewToday) DI.colors.flame else DI.colors.textFaint)
        }
        Spacer(Modifier.width(6.dp))
        StatPill(onClick = onInk) {
            InkDrop(Modifier.size(24.dp))
            Spacer(Modifier.width(4.dp))
            RollingNumber(ink, DI.type.number, DI.colors.ink)
        }
    }
}

@Composable
private fun StatPill(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

data class NavItem(val route: String, val label: String, val iconRes: Int, val color: @Composable () -> Color)

/** Full-width bottom bar; the selected tab gets a tinted, bordered tile. */
@Composable
fun DrawItBottomBar(
    items: List<NavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(DI.colors.background)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(DI.colors.line))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { item ->
                NavCell(item, item.route == selectedRoute, Modifier.weight(1f)) { onSelect(item.route) }
            }
        }
    }
}

@Composable
private fun NavCell(item: NavItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = item.color()
    val tint by animateColorAsState(if (selected) accent else DI.colors.textFaint, tween(200), label = "navTint")
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent, tween(200), label = "navBg")
    val border by animateColorAsState(if (selected) accent.copy(alpha = 0.55f) else Color.Transparent, tween(200), label = "navBorder")
    val pop by animateFloatAsState(
        if (selected) 1f else 0.9f,
        spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "navPop"
    )
    val feedback = LocalFeedback.current
    Column(
        modifier
            .clickable(remember { MutableInteractionSource() }, null) {
                if (!selected) feedback?.tick()
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(width = 54.dp, height = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(2.dp, border, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(item.iconRes),
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier.size(26.dp).graphicsLayer { scaleX = pop; scaleY = pop }
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(item.label, style = DI.type.label, color = tint, textAlign = TextAlign.Center)
    }
}

/** A rounded speech bubble with a little tail pointing down-left at Foxy. */
@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DI.colors.surface,
    border: Color = DI.colors.line,
    textColor: Color = DI.colors.text,
    tailLeft: Boolean = true
) {
    val tail: Dp = 12.dp
    Box(
        modifier
            .padding(bottom = tail)
            .drawBehind {
                val t = tail.toPx()
                val x = if (tailLeft) 28.dp.toPx() else size.width - 28.dp.toPx()
                val path = Path().apply {
                    moveTo(x - t * 0.8f, size.height - 2f)
                    lineTo(x - (if (tailLeft) t * 0.9f else -t * 0.9f), size.height + t)
                    lineTo(x + t * 0.8f, size.height - 2f)
                    close()
                }
                drawPath(path, border)
                drawPath(Path().apply {
                    moveTo(x - t * 0.55f, size.height - 4f)
                    lineTo(x - (if (tailLeft) t * 0.7f else -t * 0.7f), size.height + t - 4.dp.toPx())
                    lineTo(x + t * 0.55f, size.height - 4f)
                    close()
                }, color)
            }
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .border(2.dp, border, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text, style = DI.type.bodyStrong, color = textColor)
    }
}

/** A tiny ink amount: drop + number. */
@Composable
fun InkAmount(amount: Int, modifier: Modifier = Modifier, color: Color = DI.colors.ink, size: Dp = 18.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        InkDrop(Modifier.size(size), animate = false)
        Spacer(Modifier.width(3.dp))
        Text("$amount", style = DI.type.number, color = color)
    }
}

/** A thin top-of-screen header for secondary screens: back button and a title. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChunkyIconButton(painterResource(com.drawit.app.R.drawable.ic_arrow_left), "Back", onBack)
        Spacer(Modifier.width(14.dp))
        Text(title, style = DI.type.headline, color = DI.colors.text, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** A little sparkle burst drawn behind something that was just earned. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.burst(progress: Float, color: Color, rays: Int = 10, reach: Float = 1f) {
    if (progress <= 0f || progress >= 1f) return
    val c = Offset(size.width / 2f, size.height / 2f)
    val r0 = size.minDimension * 0.3f
    val r1 = size.minDimension * 0.62f * reach
    for (i in 0 until rays) {
        val a = Math.toRadians((i * 360.0 / rays) - 90.0)
        val inner = r0 + (r1 - r0) * progress * 0.6f
        val outer = r0 + (r1 - r0) * progress
        val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
        drawLine(
            color.copy(alpha = (1f - progress)),
            c + dir * inner, c + dir * outer,
            strokeWidth = 4.dp.toPx() * (1f - progress * 0.5f),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
