package com.taiwanfreetv.app.ui.player

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private class YouTubeJsInterface(
    private val onReadyCallback: () -> Unit,
    private val onStateChangeCallback: (Int) -> Unit,
    private val onErrorCallback: (Int, String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onReady() {
        mainHandler.post {
            onReadyCallback()
        }
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        mainHandler.post {
            onStateChangeCallback(state)
        }
    }

    @JavascriptInterface
    fun onError(code: Int) {
        mainHandler.post {
            val message = when (code) {
                2 -> "無效的影片 ID 參數 (錯誤碼 2)"
                5 -> "HTML5 播放器載入失敗 (錯誤碼 5)"
                100 -> "影片不存在、已移除或設為私有 (錯誤碼 100)"
                101, 150 -> "此頻道需透過官方平台觀看 (錯誤碼 $code)"
                152 -> "YouTube 內嵌來源驗證限制 (錯誤碼 152)"
                153 -> "YouTube 嵌入播放器遭遇受限內容錯誤 (錯誤碼 153)"
                else -> "YouTube 影片載入失敗 (錯誤碼 $code)"
            }
            onErrorCallback(code, message)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeIFramePlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    onLoadingChanged: (Boolean) -> Unit = {},
    onError: (Int, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val packageName = context.packageName

    val htmlContent = remember(videoId, packageName) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body {
                    width: 100%;
                    height: 100%;
                    background-color: #000000;
                    overflow: hidden;
                }
                #player {
                    width: 100%;
                    height: 100%;
                    border: 0;
                }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'playsinline': 1,
                            'enablejsapi': 1,
                            'origin': 'https://$packageName',
                            'widget_referrer': 'https://$packageName',
                            'rel': 0,
                            'modestbranding': 1,
                            'controls': 1,
                            'fs': 1
                        },
                        events: {
                            'onReady': function(event) {
                                if (window.AndroidBridge && window.AndroidBridge.onReady) {
                                    window.AndroidBridge.onReady();
                                }
                                try {
                                    event.target.playVideo();
                                } catch(e){}
                            },
                            'onStateChange': function(event) {
                                if (window.AndroidBridge && window.AndroidBridge.onStateChange) {
                                    window.AndroidBridge.onStateChange(event.data);
                                }
                            },
                            'onError': function(event) {
                                if (window.AndroidBridge && window.AndroidBridge.onError) {
                                    window.AndroidBridge.onError(event.data);
                                }
                            }
                        }
                    });
                }
            </script>
            <script src="https://www.youtube.com/iframe_api"></script>
        </body>
        </html>
        """.trimIndent()
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner, videoId) {
        val observer = LifecycleEventObserver { _, event ->
            val targetWebView = webViewRef
            if (targetWebView != null) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        targetWebView.evaluateJavascript("if(player && player.pauseVideo){player.pauseVideo();}", null)
                        targetWebView.onPause()
                        targetWebView.pauseTimers()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        targetWebView.onResume()
                        targetWebView.resumeTimers()
                        targetWebView.evaluateJavascript("if(player && player.playVideo){player.playVideo();}", null)
                    }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val targetWebView = webViewRef
            if (targetWebView != null) {
                try {
                    targetWebView.evaluateJavascript("if(player && player.stopVideo){player.stopVideo();}", null)
                    targetWebView.stopLoading()
                    targetWebView.onPause()
                    targetWebView.clearHistory()
                    targetWebView.loadUrl("about:blank")
                    targetWebView.removeAllViews()
                    targetWebView.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            webViewRef = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }

                    addJavascriptInterface(
                        YouTubeJsInterface(
                            onReadyCallback = {
                                // Wait for actual PLAYING state (state 1) before stopping loading indicator
                            },
                            onStateChangeCallback = { state ->
                                when (state) {
                                    1 -> onLoadingChanged(false) // PLAYING
                                    3 -> onLoadingChanged(true)  // BUFFERING
                                    0, 2, 5 -> onLoadingChanged(false) // ENDED, PAUSED, CUED
                                }
                            },
                            onErrorCallback = { errorCode, errorMsg ->
                                onLoadingChanged(false)
                                onError(errorCode, errorMsg)
                            }
                        ),
                        "AndroidBridge"
                    )

                    webChromeClient = WebChromeClient()


                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            request?.let {
                                if (it.url.host?.contains("youtube.com") == true) {
                                    // we can't really mutate request headers, but loadDataWithBaseURL provides the origin
                                    // which satisfies YouTube.
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            onLoadingChanged(false)
                            onError(-1, "網路連線或頁面載入異常: $description")
                        }
                    }

                    loadDataWithBaseURL(
                        "https://${ctx.packageName}",
                        htmlContent,
                        "text/html",
                        "utf-8",
                        null
                    )

                    webViewRef = this
                }
            },
            update = { webView ->
                webView.evaluateJavascript("if(player && player.loadVideoById){player.loadVideoById('$videoId');}", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
