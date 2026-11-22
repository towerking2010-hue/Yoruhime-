package com.yoruhime.reborn

import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView

internal class AyakaStatusBridge(
    private val webView: WebView,
    private val statusCallback: (String) -> Unit
) {
    @JavascriptInterface
    fun onStatus(message: String) {
        webView.post { statusCallback(message.take(140)) }
    }
}

@Composable
internal fun AyakaAvatar(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Waking Ayaka…") }
    var avatarView by remember { mutableStateOf<WebView?>(null) }
    val failed = status.contains("failed", true) ||
        status.contains("could not", true) ||
        status.contains("stopped", true) ||
        status.contains("error", true) ||
        status.contains("unavailable", true)

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .build()
                WebView(context).apply {
                    avatarView = this
                    setBackgroundColor(AndroidColor.rgb(9, 11, 22))
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.loadsImagesAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    addJavascriptInterface(AyakaStatusBridge(this) { status = it }, "YoruHimeBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            val line = "${message.message()} (${message.sourceId()}:${message.lineNumber()})"
                            if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                Log.e("AyakaAvatar", line)
                            } else {
                                Log.d("AyakaAvatar", line)
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                            status = "Opening Ayaka's 3D stage…"
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            Log.i("AyakaAvatar", "Avatar page finished: $url")
                            status = "Stage opened · checking 3D graphics…"
                            view.evaluateJavascript(
                                "(function(){var c=document.getElementById('avatar');return 'page='+document.readyState+', canvas='+(!!c)+', webgl2='+(!!(c&&c.getContext('webgl2')))+', webgl='+(!!(c&&c.getContext('webgl')));})()"
                            ) { result ->
                                Log.i("AyakaAvatar", "WebView startup diagnostic: $result")
                                if (!status.contains("ready", true) && !status.contains("failed", true)) {
                                    status = "Stage opened · $result"
                                }
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: android.webkit.WebResourceRequest
                        ): android.webkit.WebResourceResponse? =
                            assetLoader.shouldInterceptRequest(request.url)

                        override fun onReceivedError(
                            view: WebView,
                            request: android.webkit.WebResourceRequest,
                            error: android.webkit.WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                status = "Avatar page failed: ${error.description}"
                                Log.e("AyakaAvatar", status)
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView,
                            detail: android.webkit.RenderProcessGoneDetail
                        ): Boolean {
                            status = "Android stopped the 3D renderer. Tap retry to wake her again."
                            Log.e("AyakaAvatar", status)
                            return true
                        }
                    }
                    loadUrl("https://appassets.androidplatform.net/assets/avatar/index.html")
                }
            },
            update = { }
        )

        if (failed) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                color = Color(0xD914182A),
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(status, color = Color(0xFFE0D4FF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    if (failed || status.contains("Stage opened", true) || status.contains("checking", true)) {
                        Button(
                            onClick = {
                                status = "Retrying Ayaka's 3D stage…"
                                avatarView?.reload()
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) { Text("Retry", fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}
