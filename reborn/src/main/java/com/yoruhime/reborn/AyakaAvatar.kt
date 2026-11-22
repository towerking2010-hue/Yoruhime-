package com.yoruhime.reborn

import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.View as FilamentView
import com.google.android.filament.android.ChoreographerHelper
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

@Composable
internal fun AyakaAvatar(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Starting native 3D renderer…") }
    Box(modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context -> FilamentAyakaView(context) { status = it } },
            update = { },
            onRelease = { it.release() }
        )
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp),
            color = Color(0xD914182A),
            contentColor = Color(0xFFE0D4FF)
        ) {
            Text(status, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontSize = 11.sp)
        }
    }
}

/** Native Filament avatar surface; rendering no longer depends on Android WebView/WebGL. */
private class FilamentAyakaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val report: (String) -> Unit = {}
) : FrameLayout(context, attrs) {
    companion object {
        private const val TAG = "AyakaFilament"
    }

    private val surface = SurfaceView(context).apply {
        setBackgroundColor(AndroidColor.rgb(9, 11, 22))
        holder.setFormat(android.graphics.PixelFormat.OPAQUE)
    }
    private var viewer: ModelViewer? = null
    private var released = false
    private var rendererCreationAttempted = false
    private var firstFrameReported = false
    private var noRenderableReported = false
    private val frameLoop = object : ChoreographerHelper() {
        override fun onFrame(frameTimeNanos: Long) {
            val current = viewer ?: return
            try {
                // Use the timestamp overload on every Android version. This keeps the
                // renderer free of API-33 FrameData references on the Android 12 target.
                val rendered = current.render(frameTimeNanos)
                val progress: Float = current.progress
                val renderableCount: Int = current.asset?.renderableEntities?.size ?: 0
                if (progress >= 1f && renderableCount == 0 && !noRenderableReported) {
                    noRenderableReported = true
                    report("Ayaka's GLB loaded, but Filament found no renderable meshes.")
                    Log.e(TAG, "Ayaka GLB completed loading with zero renderable entities.")
                    this.remove()
                } else if (rendered && progress >= 1f && renderableCount > 0 && !firstFrameReported) {
                    firstFrameReported = true
                    report("Ayaka mesh rendered · $renderableCount mesh entities")
                    Log.i(TAG, "Ayaka mesh rendered by Filament ($renderableCount renderable entities).")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Filament frame failed", error)
            }
        }
    }

    init {
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        surface.setOnTouchListener { _, event -> viewer?.onTouchEvent(event); true }
        createViewer()
    }

    private fun createViewer() {
        if (released || viewer != null || rendererCreationAttempted) return
        rendererCreationAttempted = true
        try {
            // Native library loading must stay inside this guarded path. A missing or
            // incompatible JNI library should disable the avatar surface, not crash
            // the entire Compose activity during class initialization.
            Utils.init()
            firstFrameReported = false
            noRenderableReported = false
            val modelViewer = ModelViewer(surface)
            viewer = modelViewer
            modelViewer.view.renderQuality = modelViewer.view.renderQuality.apply {
                hdrColorBuffer = FilamentView.QualityLevel.MEDIUM
            }
            modelViewer.view.dynamicResolutionOptions = modelViewer.view.dynamicResolutionOptions.apply {
                enabled = true
                quality = FilamentView.QualityLevel.MEDIUM
            }
            modelViewer.view.antiAliasing = FilamentView.AntiAliasing.FXAA
            val bytes = context.assets.open("ayaka.glb").use { it.readBytes() }
            check(bytes.isNotEmpty()) { "Bundled Ayaka GLB is empty." }
            modelViewer.loadModelGlb(ByteBuffer.wrap(bytes))
            val asset = checkNotNull(modelViewer.asset) { "Filament could not create Ayaka's glTF asset." }
            val halfExtent = asset.boundingBox.halfExtent
            val maxExtent = halfExtent.maxOrNull() ?: 0f
            check(halfExtent.size >= 3 && halfExtent.all { it.isFinite() } && maxExtent.isFinite() && maxExtent > 0f) {
                "Ayaka GLB has invalid or empty mesh bounds."
            }
            check(asset.renderableEntities.isNotEmpty()) { "Ayaka GLB contains no renderable mesh entities." }
            modelViewer.transformToUnitCube()
            report("Ayaka GLB parsed · loading textures and mesh data…")
            Log.i(TAG, "Ayaka GLB loaded (" + bytes.size + " bytes).")
        } catch (error: Throwable) {
            report("Native renderer failed: " + (error.message ?: error.javaClass.simpleName))
            Log.e(TAG, "Unable to initialize native Ayaka renderer", error)
            // ModelViewer owns a detach listener on the SurfaceView. Let that listener
            // release its Filament engine when this SurfaceView is detached.
            viewer = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!released && viewer == null && !rendererCreationAttempted) createViewer()
        if (viewer != null) frameLoop.post()
    }

    override fun onDetachedFromWindow() {
        frameLoop.remove()
        // ModelViewer registers its own SurfaceView detach listener and destroys its
        // engine there. Do not destroy it here as well: that would double-free Filament.
        viewer = null
        rendererCreationAttempted = false
        super.onDetachedFromWindow()
    }

    fun release() {
        if (released) return
        released = true
        frameLoop.remove()
        // AndroidView removal detaches the SurfaceView; ModelViewer owns that cleanup.
        viewer = null
    }
}
