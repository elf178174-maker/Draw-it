package com.drawit.app.ui.inspire

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.drawit.app.data.Reference
import org.json.JSONArray
import org.json.JSONObject

/** One result in the inspiration feed. */
data class Pin(
    val id: String,
    val title: String,
    /** Grid-sized image (about 474px wide). */
    val thumb: String,
    /** Big image for the viewer. */
    val image: String,
    val width: Int,
    val height: Int,
    val color: Long?
) {
    val aspect: Float get() = if (width > 0 && height > 0) width.toFloat() / height else 0.75f
    val link: String get() = "https://www.pinterest.com/pin/$id/"

    fun toReference() = Reference(id, image, title, link, width, height, System.currentTimeMillis())
}

/**
 * Fetches Pinterest search results for the native feed.
 *
 * There is no public API that works without a registered app, so this runs
 * inside a hidden WebView on pinterest.com: it first asks the same search
 * endpoint Pinterest's own web client uses, and if that comes back empty it
 * reads the pins off the rendered search page instead. Either way the
 * results arrive here as plain data and the app draws them natively. If
 * Pinterest changes and both stop working, [failed] turns true and the UI
 * offers the full in-app browser instead.
 */
class PinterestFeed(context: Context) {

    enum class Mode { Api, Scrape }

    var pins by mutableStateOf<List<Pin>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var exhausted by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set

    private val main = Handler(Looper.getMainLooper())
    private var mode = Mode.Api
    private var bookmark: String? = null
    private var request = 0
    private var pageReady = false
    private var pendingRun: (() -> Unit)? = null
    private var timeout: Runnable? = null

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    val webView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(400, 800)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = false
        settings.blockNetworkImage = true
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        addJavascriptInterface(Bridge(), BRIDGE)
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url?.let { Uri.parse(it).host?.endsWith("pinterest.com") } != true) return
                pageReady = true
                pendingRun?.let { run -> pendingRun = null; run() }
            }
        }
    }

    /** Starts a fresh search. */
    fun search(q: String) {
        val clean = q.trim()
        if (clean.isEmpty()) return
        query = clean
        pins = emptyList()
        bookmark = null
        exhausted = false
        failed = false
        mode = Mode.Api
        request++
        fetch()
    }

    fun loadMore() {
        if (loading || exhausted || failed || query.isEmpty()) return
        fetch(more = true)
    }

    fun retry() {
        if (query.isNotEmpty()) search(query)
    }

    fun destroy() {
        timeout?.let { main.removeCallbacks(it) }
        runCatching {
            webView.stopLoading()
            webView.destroy()
        }
    }

    // -- fetching -------------------------------------------------------------------

    private fun fetch(more: Boolean = false) {
        loading = true
        val id = request
        armTimeout(id)
        when (mode) {
            Mode.Api -> runWhenReady(homeUrl()) { evaluate(apiScript(id, query, bookmark)) }
            Mode.Scrape -> if (more) {
                evaluate(scrapeScript(id, scroll = true))
            } else {
                pageReady = false
                pendingRun = { evaluate(scrapeScript(id, scroll = false)) }
                webView.loadUrl(searchUrl(query))
            }
        }
    }

    /** The API needs a pinterest.com page loaded first, for cookies and same-origin fetch. */
    private fun runWhenReady(url: String, run: () -> Unit) {
        if (pageReady) run() else {
            pendingRun = run
            if (webView.url == null) webView.loadUrl(url)
        }
    }

    private fun evaluate(script: String) {
        main.post { runCatching { webView.evaluateJavascript(script, null) } }
    }

    private fun armTimeout(id: Int) {
        timeout?.let { main.removeCallbacks(it) }
        val r = Runnable {
            if (id == request && loading) onFailure(id)
        }
        timeout = r
        main.postDelayed(r, TIMEOUT_MS)
    }

    private fun onFailure(id: Int) {
        if (id != request) return
        if (mode == Mode.Api) {
            // Fall back to reading the rendered page.
            mode = Mode.Scrape
            fetch()
        } else {
            loading = false
            if (pins.isEmpty()) failed = true else exhausted = true
        }
    }

    private fun deliver(id: Int, incoming: List<Pin>, nextBookmark: String?, endOfResults: Boolean) {
        if (id != request) return
        timeout?.let { main.removeCallbacks(it) }
        val known = pins.map { it.id }.toHashSet()
        val fresh = incoming.filter { known.add(it.id) }
        if (fresh.isEmpty() && pins.isEmpty()) {
            onFailure(id)
            return
        }
        pins = pins + fresh
        bookmark = nextBookmark
        loading = false
        // The scraper can't know when it's done; two empty scrolls in a row means it is.
        exhausted = endOfResults || (mode == Mode.Scrape && fresh.isEmpty())
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onResult(id: Int, kind: String, payload: String) {
            val parsed: Triple<List<Pin>, String?, Boolean>? = runCatching {
                when (kind) {
                    "api" -> parseApi(payload)
                    "scrape" -> Triple(parseScrape(payload), null, false)
                    else -> null
                }
            }.getOrNull()
            main.post {
                if (parsed == null) onFailure(id) else deliver(id, parsed.first, parsed.second, parsed.third)
            }
        }
    }

    companion object {
        private const val BRIDGE = "DrawItBridge"
        private const val TIMEOUT_MS = 14_000L

        fun homeUrl() = "https://www.pinterest.com/"
        fun searchUrl(q: String) = "https://www.pinterest.com/search/pins/?q=" + Uri.encode(q) + "&rs=typed"

        /** Parses Pinterest's search resource JSON. Returns pins, the next bookmark and whether that was the end. */
        fun parseApi(text: String): Triple<List<Pin>, String?, Boolean> {
            val root = JSONObject(text)
            val response = root.optJSONObject("resource_response") ?: return Triple(emptyList(), null, true)
            val data = response.opt("data")
            val results: JSONArray = when (data) {
                is JSONArray -> data
                is JSONObject -> data.optJSONArray("results") ?: JSONArray()
                else -> JSONArray()
            }
            val pins = (0 until results.length()).mapNotNull { i -> results.optJSONObject(i)?.let(::pinFromJson) }
            val bookmark = response.optString("bookmark").takeIf { it.isNotEmpty() && it != "-end-" }
                ?: root.optJSONObject("resource")?.optJSONObject("options")?.optJSONArray("bookmarks")?.optString(0)
                    ?.takeIf { it.isNotEmpty() && it != "-end-" }
            return Triple(pins, bookmark, bookmark == null)
        }

        private fun pinFromJson(o: JSONObject): Pin? {
            if (o.optString("type", "pin") != "pin") return null
            if (o.optBoolean("is_promoted", false) || o.has("promoter") && !o.isNull("promoter")) return null
            val id = o.optString("id").takeIf { it.isNotEmpty() } ?: return null
            val images = o.optJSONObject("images") ?: return null
            fun img(key: String) = images.optJSONObject(key)
            val grid = img("474x") ?: img("236x") ?: img("736x") ?: img("orig") ?: return null
            val big = img("736x") ?: img("orig") ?: grid
            val thumb = grid.optString("url").takeIf { it.startsWith("https://") } ?: return null
            val image = big.optString("url").takeIf { it.startsWith("https://") } ?: thumb
            val title = listOf("grid_title", "title", "description").firstNotNullOfOrNull { k ->
                o.optString(k).takeIf { it.isNotBlank() && it != "null" }
            } ?: ""
            val color = o.optString("dominant_color").takeIf { it.startsWith("#") && it.length == 7 }
                ?.let { runCatching { 0xFF000000 or it.substring(1).toLong(16) }.getOrNull() }
            return Pin(id, title.trim().take(140), thumb, image, grid.optInt("width"), grid.optInt("height"), color)
        }

        fun parseScrape(text: String): List<Pin> {
            val array = JSONArray(text)
            return (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val image = o.optString("image").takeIf { it.startsWith("https://") } ?: return@mapNotNull null
                val thumb = o.optString("thumb").takeIf { it.startsWith("https://") } ?: image
                Pin(id, o.optString("title").trim().take(140), thumb, image, o.optInt("w", 236), o.optInt("h", 300), null)
            }
        }

        private fun apiScript(id: Int, q: String, bookmark: String?): String = """
            (function(){
              var REQ = $id, q = ${JSONObject.quote(q)}, bm = ${if (bookmark == null) "null" else JSONObject.quote(bookmark)};
              function send(kind, payload){ try { $BRIDGE.onResult(REQ, kind, payload); } catch(e) {} }
              var data = {options:{query:q, scope:'pins', bookmarks: bm ? [bm] : [], page_size: 30, rs:'typed', redux_normalize_feed:true}, context:{}};
              var src = '/search/pins/?q=' + encodeURIComponent(q) + '&rs=typed';
              var url = '/resource/BaseSearchResource/get/?source_url=' + encodeURIComponent(src) +
                        '&data=' + encodeURIComponent(JSON.stringify(data)) + '&_=' + Date.now();
              fetch(url, {credentials:'include', headers:{
                  'Accept':'application/json, text/javascript, */*; q=0.01',
                  'X-Requested-With':'XMLHttpRequest',
                  'X-Pinterest-AppState':'active',
                  'X-Pinterest-Source-Url': src,
                  'X-Pinterest-PWS-Handler':'www/search/[scope].js'}})
                .then(function(r){ if (!r.ok) throw new Error('http ' + r.status); return r.text(); })
                .then(function(t){ send('api', t); })
                .catch(function(e){ send('error', String(e)); });
            })();
        """.trimIndent()

        private fun scrapeScript(id: Int, scroll: Boolean): String = """
            (function(){
              var REQ = $id;
              function scrape(){
                var out = [], seen = {};
                var imgs = document.querySelectorAll('img[src*="i.pinimg.com"]');
                for (var i = 0; i < imgs.length; i++) {
                  var img = imgs[i];
                  var a = img.closest('a[href*="/pin/"]');
                  if (!a) continue;
                  var m = (a.getAttribute('href') || '').match(/\/pin\/([^\/?#]+)/);
                  if (!m || seen[m[1]]) continue;
                  seen[m[1]] = 1;
                  var src = img.getAttribute('src') || '';
                  var best = src, parts = (img.getAttribute('srcset') || '').split(',');
                  for (var k = 0; k < parts.length; k++) {
                    var u = parts[k].trim().split(' ')[0];
                    if (u.indexOf('/736x/') >= 0) best = u;
                    else if (u.indexOf('/474x/') >= 0 && best.indexOf('/736x/') < 0) best = u;
                  }
                  var r = img.getBoundingClientRect();
                  out.push({id: m[1], title: img.getAttribute('alt') || '', image: best, thumb: src,
                            w: img.naturalWidth || Math.round(r.width) || 236, h: img.naturalHeight || Math.round(r.height) || 300});
                }
                return out;
              }
              ${if (scroll) "window.scrollBy(0, 4000);" else ""}
              setTimeout(function(){ try { $BRIDGE.onResult(REQ, 'scrape', JSON.stringify(scrape())); } catch(e) {} }, ${if (scroll) 1400 else 1800});
            })();
        """.trimIndent()
    }
}
