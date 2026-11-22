package com.yoruhime.reborn

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.View as FilamentView
import com.google.android.filament.android.ChoreographerHelper
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

@Composable
internal fun AyakaAvatar(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> FilamentAyakaView(context) },
        update = { },
        onRelease = { it.release() }
    )
}

/** Native Filament avatar surface; rendering no longer depends on Android WebView/WebGL. */
private class FilamentAyakaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    companion object {
        private const val TAG = "AyakaFilament"
        init { Utils.init() }
    }

    private val surface = SurfaceView(context).apply {
        setBackgroundColor(Color.rgb(9, 11, 22))
        holder.setFormat(android.graphics.PixelFormat.OPAQUE)
    }
    private var viewer: ModelViewer? = null
    private var released = false
    private val frameLoop = object : ChoreographerHelper() {
        override fun onFrame(frameTimeNanos: Long, frameData: Any?) {
            val current = viewer ?: return
            try {
                if (android.os.Build.VERSION.SDK_INT >= 33 && frameData is Choreographer.FrameData) {
                    current.render(frameData)
                } else current.render(frameTimeNanos)
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
        if (released || viewer != null) return
        try {
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
            check(modelViewer.asset != null) { "Filament could not create Ayaka's glTF asset." }
            modelViewer.transformToUnitCube()
            Log.i(TAG, "Ayaka GLB loaded (" + bytes.size + " bytes).")
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to initialize native Ayaka renderer", error)
            viewer?.let { runCatching { it.destroy() } }
            viewer = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!released && viewer == null) createViewer()
        frameLoop.post()
    }

    override fun onDetachedFromWindow() {
        frameLoop.remove()
        // ModelViewer documents that it must be recreated after its SurfaceView detaches.
        viewer?.let { current -> runCatching { current.destroy() } }
        viewer = null
        super.onDetachedFromWindow()
    }

    fun release() {
        if (released) return
        released = true
        frameLoop.remove()
        viewer?.let { runCatching { it.destroy() } }
        viewer = null
    }
}
