package dev.erolit.reader.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.erolit.reader.ui.components.ScreenHeader

/**
 * A plain WebView host that shows the same page the website would show.
 *
 * It is used for the parts of the service the app cannot render itself, for
 * example audio playback, illustrations, and the sign-in browser check. The
 * user agent deliberately looks like a mobile browser so the site does not
 * treat the request as an embedded app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    initialTitle: String? = null,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember(url) {
        mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: "Website")
    }
    var progress by remember(url) { mutableIntStateOf(0) }
    var loading by remember(url) { mutableStateOf(true) }
    var firstPaint by remember(url) { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (firstPaint) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "webview-fade-in",
    )

    val webView = remember(url) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.userAgentString = BROWSER_USER_AGENT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val target = request.url
                    val scheme = target.scheme?.lowercase()
                    if (scheme == "http" || scheme == "https") return false
                    // Anything else (mailto:, intent:, market:, ...) belongs to
                    // the system, not to this embedded browser.
                    if (request.isForMainFrame) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, target))
                        }
                    }
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    loading = true
                    progress = 0
                    firstPaint = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    loading = false
                    progress = 100
                    // Keep cookies written by the page available to later
                    // visits during this session.
                    CookieManager.getInstance().flush()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress
                    if (newProgress >= 100) loading = false
                }

                override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                    if (!pageTitle.isNullOrBlank()) title = pageTitle
                }
            }

            loadUrl(url)
        }
    }

    val closeOrGoBack = {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    BackHandler(onBack = closeOrGoBack)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = title.ifBlank { "Website" },
            onBack = closeOrGoBack,
        ) {
            IconButton(onClick = { webView.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload page")
            }
        }
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(tween(durationMillis = 150)),
            exit = fadeOut(tween(durationMillis = 150)),
        ) {
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize().alpha(contentAlpha),
            )
        }
    }
}

/**
 * A mobile Chrome user agent without the `wv` token. Some sign-in pages show a
 * browser check to embedded webviews; presenting a regular browser identity
 * lets the user get past it where the site allows it.
 */
private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
