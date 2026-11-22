package com.yoruhime.reborn

import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
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
import com.google.android.filament.Engine
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

/** Native Filament avatar surface using TextureView so Compose can composite its UI above the live 3D scene. */
private class FilamentAyakaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val report: (String) -> Unit = {}
) : FrameLayout(context, attrs) {
    companion object {
        private const val TAG = "AyakaFilament"
    }

    private val surface = TextureView(context).apply {
        // TextureView is composited inside the Compose window; it does not support
        // View backgrounds. Let Filament draw the stage and keep it transparent
        // so the Compose controls can layer above the live avatar.
        isOpaque = false
    }
    private var viewer: ModelViewer? = null
    private var released = false
    private var rendererCreationAttempted = false
    private var firstFrameReported = false
    private var pixelProbeRequested = false
    private var pixelProbeCompleted = false
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
                } else if (rendered && progress >= 1f && renderableCount > 0 && !pixelProbeRequested) {
                    // A successful beginFrame/render call only proves that Filament submitted
                    // a frame. It does NOT prove the avatar produced visible pixels. Read back
                    // one real GPU frame and count pixels that differ from the dark stage.
                    pixelProbeRequested = true
                    current.debugGetNextFrameCallback { bitmap ->
                        try {
                            val pixels = IntArray(bitmap.width * bitmap.height)
                            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            var visiblePixels = 0
                            var minX = bitmap.width
                            var minY = bitmap.height
                            var maxX = -1
                            var maxY = -1
                            for (y in 0 until bitmap.height) {
                                for (x in 0 until bitmap.width) {
                                    val color = pixels[y * bitmap.width + x]
                                    val red = AndroidColor.red(color)
                                    val green = AndroidColor.green(color)
                                    val blue = AndroidColor.blue(color)
                                    // Ignore the near-black stage/background; count actual
                                    // character-colored output from the Filament framebuffer.
                                    if (red + green + blue > 70 &&
                                        (kotlin.math.abs(red - 9) + kotlin.math.abs(green - 11) + kotlin.math.abs(blue - 22)) > 55
                                    ) {
                                        visiblePixels++
                                        if (x < minX) minX = x
                                        if (x > maxX) maxX = x
                                        if (y < minY) minY = y
                                        if (y > maxY) maxY = y
                                    }
                                }
                            }
                            pixelProbeCompleted = true
                            val bounds = if (visiblePixels > 0) "$minX,$minY-$maxX,$maxY" else "none"
                            Log.i(TAG, "Filament framebuffer pixel probe: visiblePixels=$visiblePixels bounds=$bounds size=${bitmap.width}x${bitmap.height}")
                            if (visiblePixels >= 100) {
                                firstFrameReported = true
                                report("Ayaka visible · $visiblePixels rendered pixels")
                                Log.i(TAG, "Ayaka produced visible framebuffer pixels.")
                            } else {
                                report("Meshes loaded, but Ayaka is not visible · $visiblePixels pixels")
                                Log.e(TAG, "Ayaka mesh entities exist, but framebuffer contains no visible avatar pixels.")
                            }
                        } catch (error: Throwable) {
                            pixelProbeCompleted = true
                            report("Could not verify Ayaka pixels: ${error.message ?: error.javaClass.simpleName}")
                            Log.e(TAG, "Framebuffer pixel verification failed", error)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Filament frame failed", error)
            }
        }
    }

    init {
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        surface.setOnTouchListener { _, event -> viewer?.onTouchEvent(event); true }
        // Wait until Android has attached the TextureView before creating Filament.
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
            pixelProbeRequested = false
            pixelProbeCompleted = false
            noRenderableReported = false
            // Ayaka's VRM has a large 657-joint rig across three skins. On GLES 3.0
            // devices (including the Android 12 emulator's SwiftShader), Filament's
            // masked-unlit skinning shader can exceed GL_MAX_VERTEX_UNIFORM_VECTORS
            // and abort the native render thread. Vulkan uses a different shader
            // resource path and avoids that GLES uniform-vector ceiling.
            val engine = Engine.create(Engine.Backend.VULKAN)
            val modelViewer = ModelViewer(surface, engine)
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
            // Ayaka is a skinned VRM with no embedded glTF animation clips.
            // Explicitly upload the bind/rest-pose bone matrices so the avatar
            // is valid before an idle-animation system is introduced.
            modelViewer.animator?.updateBoneMatrices()
            Log.i(
                TAG,
                "Ayaka skeleton initialized: skins=${asset.instance.skinCount}, " +
                    "animations=${modelViewer.animator?.animationCount ?: 0}, " +
                    "joints=${(0 until asset.instance.skinCount).sumOf { asset.instance.getJointCountAt(it) }}"
            )
            val halfExtent = asset.boundingBox.halfExtent
            val maxExtent = halfExtent.maxOrNull() ?: 0f
            check(halfExtent.size >= 3 && halfExtent.all { it.isFinite() } && maxExtent.isFinite() && maxExtent > 0f) {
                "Ayaka GLB has invalid or empty mesh bounds."
            }
            check(asset.renderableEntities.isNotEmpty()) { "Ayaka GLB contains no renderable mesh entities." }
            modelViewer.transformToUnitCube()
            report("Ayaka GLB parsed · Vulkan renderer loading textures and mesh data…")
            Log.i(TAG, "Ayaka GLB loaded (" + bytes.size + " bytes) using Vulkan backend.")
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
        if (!released && viewer == null && !rendererCreationAttempted) {
            // Posting lets the child TextureView finish its own attach dispatch first.
            // Let Compose draw the companion stage before loading the 20 MB model.
            // This prevents native setup and GLB parsing from delaying Activity startup.
            surface.postDelayed({
                if (isAttachedToWindow && !released && viewer == null && !rendererCreationAttempted) {
                    createViewer()
                    if (viewer != null) frameLoop.post()
                }
            }, 750L)
        }
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
