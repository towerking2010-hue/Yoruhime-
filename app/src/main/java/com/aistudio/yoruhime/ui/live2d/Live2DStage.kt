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
import org.json.JSONObject
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
    private var renderingEnabled = true

    fun setRenderingEnabled(enabled: Boolean) {
        renderingEnabled = enabled
        val script = if (enabled) "window.resumeLive2D && window.resumeLive2D();" else "window.pauseLive2D && window.pauseLive2D();"
        webView?.post { webView?.evaluateJavascript(script, null) }
    }

    fun setEmotion(emotion: String) {
        val wv = webView
        if (wv != null) {
            val script = "window.setEmotion && window.setEmotion(${JSONObject.quote(emotion)});"
            wv.post { wv.evaluateJavascript(script, null) }
        } else {
            pendingEmotion = emotion
        }
    }

    fun playMotion(motionName: String = "Idle") {
        val wv = webView ?: return
        val script = "window.playMotion && window.playMotion(${JSONObject.quote(motionName)});"
        wv.post { wv.evaluateJavascript(script, null) }
    }

    fun lookAt(normalizedX: Float, normalizedY: Float) {
        val wv = webView ?: return
        val safeX = normalizedX.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
        val safeY = normalizedY.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
        val script = "window.lookAt && window.lookAt($safeX, $safeY);"
        wv.post { wv.evaluateJavascript(script, null) }
    }

    fun showSpeech(text: String, emotion: String? = null) {
        val wv = webView ?: return
        // Quote cloud-generated text as a JS literal instead of interpolating raw code.
        val quotedText = JSONObject.quote(text)
        val emoParam = emotion?.let { JSONObject.quote(it) } ?: "null"
        val script = "window.showSpeech && window.showSpeech($quotedText, $emoParam);"
        wv.post { wv.evaluateJavascript(script, null) }
    }

    fun onReady() {
        pendingEmotion?.let {
            setEmotion(it)
            pendingEmotion = null
        }
        setRenderingEnabled(renderingEnabled)
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
    isVisible: Boolean = true,
    controller: Live2DController = remember { Live2DController() },
    onModelTapped: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(currentEmotion) {
        controller.setEmotion(currentEmotion)
    }

    LaunchedEffect(isVisible) {
        controller.setRenderingEnabled(isVisible)
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

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            // Keep this surface restricted to its bundled local asset origin.
                            return request?.url?.host != "appassets.androidplatform.net"
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
