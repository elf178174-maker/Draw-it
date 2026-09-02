package com.drawit.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.StrokeCap
import com.drawit.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brief opening moment: the mark settles in, a pencil line is drawn under it,
 * then the whole thing lifts away.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val markScale = remember { Animatable(0.82f) }
    val markAlpha = remember { Animatable(0f) }
    val strokeProgress = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        launch {
            markAlpha.animateTo(1f, tween(420, easing = LinearOutSlowInEasing))
        }
        markScale.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow))
        launch {
            strokeProgress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        }
        delay(260)
        wordAlpha.animateTo(1f, tween(380))
        delay(560)
        exitAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer { alpha = exitAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo_mark),
                contentDescription = "Draw it",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(148.dp)
                    .graphicsLayer {
                        alpha = markAlpha.value
                        scaleX = markScale.value
                        scaleY = markScale.value
                    }
            )
            Spacer(Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .width(160.dp)
                    .height(16.dp)
            ) {
                val path = Path().apply {
                    moveTo(size.width * 0.06f, size.height * 0.72f)
                    quadraticBezierTo(
                        size.width * 0.5f, size.height * 0.05f,
                        size.width * 0.94f, size.height * 0.58f
                    )
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val visible = Path()
                measure.getSegment(0f, measure.length * strokeProgress.value, visible, true)
                drawPath(
                    path = visible,
                    color = accent,
                    style = Stroke(width = 4.5f * density, cap = StrokeCap.Round)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Draw it",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha.value }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "A page a day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha.value }
            )
        }
    }
}
