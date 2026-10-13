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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.data.Reference
import com.drawit.app.ui.art.FoxyPose
import com.drawit.app.ui.components.ChunkyButton
import com.drawit.app.ui.components.ChunkyIconButton
import com.drawit.app.ui.components.ChunkyTextField
import com.drawit.app.ui.components.Chunks
import com.drawit.app.ui.components.EmptyState
import com.drawit.app.ui.components.FoxyView
import com.drawit.app.ui.components.Segmented
import com.drawit.app.ui.components.SpeechBubble
import com.drawit.app.ui.components.chunky
import com.drawit.app.ui.inspire.Pin
import com.drawit.app.ui.inspire.PinterestFeed
import com.drawit.app.ui.theme.DI

private data class Topic(val label: String, val query: String)

private val topics = listOf(
    Topic("Sketchbook", "sketchbook ideas drawing"),
    Topic("Portraits", "portrait drawing reference"),
    Topic("Hands", "hand drawing reference"),
    Topic("Anatomy", "figure drawing anatomy study"),
    Topic("Poses", "pose reference drawing"),
    Topic("Landscape", "landscape sketch pencil"),
    Topic("Characters", "character design sketch"),
    Topic("Ink", "ink drawing illustration"),
    Topic("Still life", "still life drawing"),
    Topic("Perspective", "perspective drawing practice"),
    Topic("Animals", "animal sketch drawing"),
    Topic("Botanical", "botanical line drawing"),
    Topic("Comics", "comic panel sketch")
)

@Composable
fun InspirationScreen(viewModel: AppViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var viewing by remember { mutableStateOf<Pin?>(null) }
    var browseUrl by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = viewing != null) { viewing = null }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("Inspire", style = DI.type.display, color = DI.colors.text)
                Spacer(Modifier.height(10.dp))
                Segmented(listOf("Feed", "Saved", "Browse"), mode, { mode = it })
                Spacer(Modifier.height(12.dp))
            }
            when (mode) {
                0 -> FeedTab(viewModel, onOpen = { viewing = it }, onBrowse = { browseUrl = it; mode = 2 })
                1 -> SavedTab(viewModel, onOpen = { viewing = it })
                else -> BrowseTab(startUrl = browseUrl)
            }
        }

        AnimatedVisibility(visible = viewing != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
            viewing?.let { pin ->
                PinViewer(
                    pin = pin,
                    saved = viewModel.isReferenceSaved(pin.id),
                    onClose = { viewing = null },
                    onSave = { viewModel.toggleReference(pin.toReference()) },
                    onDraw = { viewModel.drawFromReference(pin.toReference()); viewing = null },
                    onOpenPinterest = { browseUrl = pin.link; viewing = null; mode = 2 }
                )
            }
        }
    }
}

// -- feed -----------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedTab(viewModel: AppViewModel, onOpen: (Pin) -> Unit, onBrowse: (String) -> Unit) {
    val context = LocalContext.current
    val feed = remember { PinterestFeed(context) }
    DisposableEffect(Unit) { onDispose { feed.destroy() } }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) {
        // The feed is new each time the tab comes back, but the chosen topic or
        // typed search is restored, and -1 means a typed search.
        if (feed.query.isEmpty()) {
            val restored = topics.getOrNull(selected)?.query ?: text.takeIf { it.isNotBlank() }
            if (restored == null) selected = 0
            feed.search(restored ?: topics[0].query)
        }
    }

    val grid = rememberLazyStaggeredGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = grid.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) feed.loadMore() }

    Column(Modifier.fillMaxSize()) {
        // The hidden page that does the fetching; it has to be attached to run reliably.
        AndroidView(factory = { feed.webView }, modifier = Modifier.size(1.dp).alpha(0f))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ChunkyTextField(
                text, { text = it },
                placeholder = "Search Pinterest for references",
                imeAction = ImeAction.Search,
                onImeAction = { if (text.isNotBlank()) { selected = -1; feed.search(text) } },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            ChunkyIconButton(painterResource(R.drawable.ic_search), "Search", {
                if (text.isNotBlank()) { selected = -1; feed.search(text) }
            }, chunk = Chunks.ice(), size = 48.dp)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics.indices.toList()) { i ->
                val chunk = if (i == selected) Chunks.soft() else Chunks.plain()
                Text(
                    topics[i].label.uppercase(),
                    style = DI.type.label,
                    color = chunk.content,
                    modifier = Modifier
                        .chunky(chunk, shape = RoundedCornerShape(14.dp), corner = 14.dp, lip = 3.dp, onClick = {
                            if (i != selected) viewModel.feedback.select()
                            selected = i
                            text = ""
                            feed.search(topics[i].query)
                        })
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            feed.failed -> FeedError(onRetry = { feed.retry() }, onBrowse = { onBrowse(PinterestFeed.searchUrl(feed.query)) })
            feed.pins.isEmpty() -> SkeletonGrid()
            else -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = grid,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                items(feed.pins.size, key = { feed.pins[it].id }) { i ->
                    val pin = feed.pins[i]
                    PinCard(pin, saved = viewModel.isReferenceSaved(pin.id), onClick = { onOpen(pin) })
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        when {
                            feed.loading -> LoadingDots()
                            feed.exhausted -> Text("That's everything for \"${feed.query}\".", style = DI.type.small, color = DI.colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinCard(pin: Pin, saved: Boolean, onClick: () -> Unit) {
    // Scraped pins may not know their size until the picture arrives.
    var aspect by remember(pin.id) { mutableFloatStateOf(pin.aspect.coerceIn(0.4f, 1.8f)) }
    var loaded by remember(pin.id) { mutableStateOf(false) }
    val fade by animateFloatAsState(if (loaded) 1f else 0f, tween(260), label = "pinFade")
    val placeholder = pin.color?.let { Color(it) } ?: DI.colors.sunken
    Column(Modifier.chunky(Chunks.plain(), corner = 18.dp, onClick = onClick).padding(5.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(13.dp)).background(placeholder.copy(alpha = 0.5f))) {
            AsyncImage(
                model = pin.thumb,
                contentDescription = pin.title,
                contentScale = ContentScale.Crop,
                onSuccess = { state ->
                    loaded = true
                    val s = state.painter.intrinsicSize
                    if (s.width > 0f && s.height > 0f) aspect = (s.width / s.height).coerceIn(0.4f, 1.8f)
                },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade }
            )
            if (saved) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(RoundedCornerShape(9.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_heart_filled), null, tint = DI.colors.red, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (pin.title.isNotBlank()) {
            Text(
                pin.title, style = DI.type.small, color = DI.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SkeletonGrid() {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        0.35f, 0.75f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "skeletonPulse"
    )
    val heights = listOf(220, 160, 190, 250, 170, 210)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0, 1).forEach { col ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                heights.filterIndexed { i, _ -> i % 2 == col }.forEach { h ->
                    Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(16.dp)).background(DI.colors.sunken.copy(alpha = pulse)))
                }
            }
        }
    }
}

@Composable
private fun LoadingDots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..2).forEach { i ->
            val s by t.animateFloat(
                0.5f, 1f,
                infiniteRepeatable(tween(420, delayMillis = i * 120), RepeatMode.Reverse),
                label = "dot$i"
            )
            Box(Modifier.size(10.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(5.dp)).background(DI.colors.brand))
        }
    }
}

@Composable
private fun FeedError(onRetry: () -> Unit, onBrowse: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
        EmptyState(
            title = "Pinterest didn't answer",
            body = "Maybe you're offline, or Pinterest changed something on their side. You can still browse it directly.",
            illustration = { FoxyView(Modifier.size(150.dp), fit = 0.9f) { FoxyPose(sad = 0.7f) } },
            action = {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ChunkyButton("Try again", onRetry, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    ChunkyButton("Browse Pinterest", onBrowse, chunk = Chunks.plain(), icon = painterResource(R.drawable.ic_globe), modifier = Modifier.fillMaxWidth())
                }
            }
        )
    }
}

// -- saved ------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedTab(viewModel: AppViewModel, onOpen: (Pin) -> Unit) {
    val saved by viewModel.references.collectAsStateWithLifecycle()
    if (saved.isEmpty()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
            EmptyState(
                title = "No saved references yet",
                body = "Tap the heart on anything in the feed to keep it here for later.",
                illustration = { FoxyView(Modifier.size(150.dp), fit = 0.9f) { FoxyPose(lookY = 0.3f, lookWeight = 0.5f) } }
            )
        }
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        items(saved.size, key = { saved[it].id }) { i ->
            val ref = saved[i]
            PinCard(ref.toPin(), saved = true, onClick = { onOpen(ref.toPin()) })
        }
    }
}

private fun Reference.toPin() = Pin(id, title, imageUrl, imageUrl, width, height, null)

// -- viewer -------------------------------------------------------------------------------

@Composable
private fun PinViewer(
    pin: Pin,
    saved: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDraw: () -> Unit,
    onOpenPinterest: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DI.colors.background)
            .clickable(remember { MutableInteractionSource() }, null) {}
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ChunkyIconButton(painterResource(R.drawable.ic_close), "Close", onClose)
                Spacer(Modifier.weight(1f))
                ChunkyIconButton(
                    painterResource(if (saved) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                    if (saved) "Remove from saved" else "Save", onSave,
                    chunk = if (saved) Chunks.red() else Chunks.plain()
                )
                Spacer(Modifier.width(10.dp))
                ChunkyIconButton(painterResource(R.drawable.ic_external), "Open on Pinterest", onOpenPinterest)
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = pin.image,
                    contentDescription = pin.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
                )
            }
            if (pin.title.isNotBlank()) {
                Text(pin.title, style = DI.type.title, color = DI.colors.text, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
            }
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                FoxyView(Modifier.size(80.dp), fit = 0.9f) { FoxyPose(cheer = 0.3f, lookX = 0.6f, lookY = -0.5f, lookWeight = 0.8f) }
                SpeechBubble("Ooh, draw this one!", modifier = Modifier.padding(bottom = 22.dp))
            }
            ChunkyButton(
                "Draw this today",
                onDraw,
                icon = painterResource(R.drawable.ic_pencil),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

// -- browse (the full site, kept as a fallback) ----------------------------------------------

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowseTab(startUrl: String?) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(startUrl ?: PinterestFeed.searchUrl(topics[0].query)) }
    val savedWebState = rememberSaveable { Bundle() }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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

    LaunchedEffect(startUrl) {
        when {
            startUrl != null -> webView.loadUrl(startUrl)
            savedWebState.isEmpty -> webView.loadUrl(currentUrl)
            else -> webView.restoreState(savedWebState)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            webView.saveState(savedWebState)
            webView.destroy()
        }
    }
    BackHandler(enabled = canGoBack) {
        webView.goBack()
        canGoBack = webView.canGoBack()
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("pinterest.com", style = DI.type.small, color = DI.colors.textMuted, modifier = Modifier.weight(1f))
            ChunkyIconButton(painterResource(R.drawable.ic_refresh), "Reload", { webView.reload() }, size = 40.dp)
            Spacer(Modifier.width(8.dp))
            ChunkyIconButton(painterResource(R.drawable.ic_external), "Open in browser", {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }, size = 40.dp)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().background(DI.colors.surface))
            if (progress < 1f) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(DI.colors.sunken)) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0.05f, 1f)).height(4.dp).background(DI.colors.brand))
                }
            }
        }
    }
}
