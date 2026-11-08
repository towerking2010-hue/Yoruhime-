package com.aistudio.yoruhime.ui.live2d

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.aistudio.yoruhime.ui.theme.MidnightBlack

class Live2DController {
    var webView: WebView? = null
    private var pendingEmotion: String? = null

    fun setEmotion(emotion: String) {
        val wv = webView
        if (wv != null) {
            val script = "window.setEmotion && window.setEmotion('$emotion');"
            wv.post { wv.evaluateJavascript(script, null) }
        } else {
            pendingEmotion = emotion
        }
    }

    fun playMotion(motionName: String = "Idle") {
        val wv = webView ?: return
        val script = "window.playMotion && window.playMotion('$motionName');"
        wv.post { wv.evaluateJavascript(script, null) }
    }

    fun lookAt(normalizedX: Float, normalizedY: Float) {
        val wv = webView ?: return
        val script = "window.lookAt && window.lookAt($normalizedX, $normalizedY);"
        wv.post { wv.evaluateJavascript(script, null) }
    }

    fun showSpeech(text: String, emotion: String? = null) {
        val wv = webView ?: return
        val escapedText = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
        val emoParam = if (emotion != null) "'$emotion'" else "null"
        val script = "window.showSpeech && window.showSpeech('$escapedText', $emoParam);"
        wv.post { wv.evaluateJavascript(script, null) }
    }

    fun onReady() {
        pendingEmotion?.let {
            setEmotion(it)
            pendingEmotion = null
        }
    }
}

class AndroidBridge(
    private val onTapped: (String) -> Unit,
    private val onReady: () -> Unit
) {
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @JavascriptInterface
    fun onModelTapped(zone: String) {
        mainHandler.post { onTapped(zone) }
    }

    @JavascriptInterface
    fun onModelReady() {
        mainHandler.post { onReady() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Live2DStage(
    currentEmotion: String,
    controller: Live2DController = remember { Live2DController() },
    onModelTapped: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(currentEmotion) {
        controller.setEmotion(currentEmotion)
    }

    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .testTag("live2d_stage_container")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("live2d_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.TRANSPARENT)
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    addJavascriptInterface(
                        AndroidBridge(
                            onTapped = onModelTapped,
                            onReady = { controller.onReady() }
                        ),
                        "AndroidBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url ?: return null
                            return assetLoader.shouldInterceptRequest(url)
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/assets/live2d/index.html")
                    controller.webView = this
                }
            },
            update = { wv ->
                controller.webView = wv
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.webView?.destroy()
            controller.webView = null
        }
    }
}
