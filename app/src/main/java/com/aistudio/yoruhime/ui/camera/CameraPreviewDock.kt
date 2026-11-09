package com.aistudio.yoruhime.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aistudio.yoruhime.ui.theme.GlassBorder
import com.aistudio.yoruhime.ui.theme.GlassDark
import com.aistudio.yoruhime.ui.theme.LavenderPrimary
import com.aistudio.yoruhime.ui.theme.MidnightBlack
import com.aistudio.yoruhime.ui.theme.SlateBorder
import com.aistudio.yoruhime.ui.theme.TextMuted
import com.aistudio.yoruhime.ui.theme.TextSilver
import com.aistudio.yoruhime.ui.theme.TextWhite
import com.aistudio.yoruhime.ui.theme.VioletAccent

@Composable
fun CameraPreviewDock(
    isVisible: Boolean,
    isFrontCamera: Boolean,
    onToggleCameraFacing: () -> Unit,
    onClose: () -> Unit,
    onCaptureForInspection: (Bitmap, Boolean) -> Unit, // bitmap, isFrontCamera
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(isVisible) {
        if (isVisible && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Bind Camera lifecycle when isVisible, permission, or lens facing toggles
    LaunchedEffect(isVisible, hasCameraPermission, isFrontCamera, previewView) {
        if (!isVisible || !hasCameraPermission || previewView == null) {
            cameraProviderInstance?.unbindAll()
            return@LaunchedEffect
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProviderInstance = cameraProvider

                val lensFacing = if (isFrontCamera) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView?.surfaceProvider
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView?.display?.rotation ?: Surface.ROTATION_0)
                    .setJpegQuality(80)
                    .build()
                imageCapture = capture

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(isVisible) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
            } catch (e: Exception) {
                // Ignore unbind on dispose
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("camera_dock_surface"),
            shape = RoundedCornerShape(20.dp),
            color = GlassDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFrontCamera) Icons.Default.PhotoCamera else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = LavenderPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFrontCamera) "Front Camera Snapshot" else "Rear Camera Snapshot",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleCameraFacing,
                            modifier = Modifier.size(36.dp).testTag("camera_switch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = VioletAccent
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp).testTag("camera_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Camera",
                                tint = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (hasCameraPermission) {
                    var isFlashing by remember { mutableStateOf(false) }
                    val infiniteScan = androidx.compose.animation.core.rememberInfiniteTransition(label = "scanner")
                    val scanLineY by infiniteScan.animateFloat(
                        initialValue = 0f,
                        targetValue = 166f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(2200, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "scan_line"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MidnightBlack)
                            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    previewView = this
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(170.dp)
                        )

                        // Animated High-Tech Laser Scan Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .padding(horizontal = 8.dp)
                                .offset(y = scanLineY.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            LavenderPrimary,
                                            androidx.compose.ui.graphics.Color.White,
                                            LavenderPrimary,
                                            androidx.compose.ui.graphics.Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Mode indicator badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = GlassDark.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = if (isFrontCamera) "FRONT CAMERA • SNAPSHOT" else "REAR CAMERA • SNAPSHOT",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = LavenderPrimary,
                                fontSize = 10.sp
                            )
                        }

                        // Shutter Flash Animation
                        if (isFlashing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "With Cloud mode and an API key enabled, one still image is sent to your selected AI provider. Offline mode cannot analyze images.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Inspection Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isFlashing = true
                                    delay(120)
                                    isFlashing = false
                                }
                                captureSnapshot(context, imageCapture, coroutineScope) { bitmap ->
                                    onCaptureForInspection(bitmap, isFrontCamera)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("camera_inspect_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ask Ayaka to Inspect",
                                color = TextWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Camera permission needed for vision analysis",
                            color = TextSilver,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                        ) {
                            Text("Grant Permission", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun captureSnapshot(
    context: Context,
    imageCapture: ImageCapture?,
    coroutineScope: CoroutineScope,
    onCaptured: (Bitmap) -> Unit
) {
    val capture = imageCapture ?: return
    val callbackExecutor = ContextCompat.getMainExecutor(context)

    capture.takePicture(
        callbackExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotationDegrees = image.imageInfo.rotationDegrees
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                // Decode and rotate off the UI thread, sampling large camera frames down
                // before allocating a full-resolution bitmap on mid-range phones.
                coroutineScope.launch(Dispatchers.Default) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val maxDimension = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                    var sampleSize = 1
                    while (maxDimension / sampleSize > 1280) sampleSize *= 2

                    val decoded = BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    ) ?: return@launch

                    val upright = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(
                            decoded, 0, 0, decoded.width, decoded.height, matrix, true
                        ).also { rotated ->
                            if (rotated !== decoded) decoded.recycle()
                        }
                    } else {
                        decoded
                    }

                    withContext(Dispatchers.Main) {
                        onCaptured(upright)
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}
