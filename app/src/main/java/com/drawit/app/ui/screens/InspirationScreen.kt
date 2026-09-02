package com.drawit.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drawit.app.R
import com.drawit.app.ui.components.Entrance
import com.drawit.app.ui.components.SectionLabel
import com.drawit.app.ui.components.pressable

private data class Topic(val label: String, val query: String)

private val topics = listOf(
    Topic("Sketchbook", "sketchbook ideas drawing"),
    Topic("Portraits", "portrait drawing reference"),
    Topic("Hands", "hand drawing reference"),
    Topic("Anatomy", "figure drawing anatomy study"),
    Topic("Landscape", "landscape sketch pencil"),
    Topic("Characters", "character design sketch"),
    Topic("Ink", "ink drawing illustration"),
    Topic("Still life", "still life drawing"),
    Topic("Perspective", "perspective drawing practice"),
    Topic("Animals", "animal sketch drawing"),
    Topic("Botanical", "botanical line drawing"),
    Topic("Comics", "comic panel sketch")
)

private fun urlFor(topic: Topic): String =
    "https://www.pinterest.com/search/pins/?q=" + Uri.encode(topic.query)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InspirationScreen() {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(urlFor(topics[0])) }

    // Keeps the browsing session across trips to other tabs.
    val savedWebState = rememberSaveable { Bundle() }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = true
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    canGoBack = view?.canGoBack() == true
                    if (url != null) currentUrl = url
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress / 100f
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (savedWebState.isEmpty) webView.loadUrl(urlFor(topics[selected]))
        else webView.restoreState(savedWebState)
    }

    DisposableEffect(Unit) {
        onDispose { webView.saveState(savedWebState) }
    }

    BackHandler(enabled = canGoBack) {
        webView.goBack()
        canGoBack = webView.canGoBack()
    }

    Column(Modifier.fillMaxSize()) {
        Entrance(delayMillis = 40) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Spacer(Modifier.statusBarsPadding().height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("Browse Pinterest")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Inspiration",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    SmallCircleButton(R.drawable.ic_refresh, "Reload") { webView.reload() }
                    Spacer(Modifier.width(10.dp))
                    SmallCircleButton(R.drawable.ic_external, "Open in browser") {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(topics.size) { index ->
                TopicChip(
                    label = topics[index].label,
                    selected = index == selected,
                    onClick = {
                        selected = index
                        webView.loadUrl(urlFor(topics[index]))
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 84.dp)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            AnimatedVisibility(
                visible = progress < 1f,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(320)),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                LoadingBar(progress = progress)
            }
        }
    }
}

@Composable
private fun LoadingBar(progress: Float) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(240),
        label = "webProgress"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun TopicChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    val background = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainer
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = foreground,
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .pressable(scaleDown = 0.94f, onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 11.dp)
    )
}

@Composable
private fun SmallCircleButton(iconRes: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .pressable(scaleDown = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}
