package com.aistudio.yoruhime.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aistudio.yoruhime.ui.theme.MidnightBlack
import com.aistudio.yoruhime.ui.theme.MidnightNavy
import com.aistudio.yoruhime.ui.theme.SlateDark

@Composable
fun ARCameraBackground(
    isCameraActive: Boolean,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
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
        hasPermission = granted
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(isCameraActive) {
        if (isCameraActive && !hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(isCameraActive, hasPermission, isFrontCamera, previewView) {
        if (!isCameraActive || !hasPermission || previewView == null) {
            cameraProviderInstance?.unbindAll()
            cameraError = null
            return@LaunchedEffect
        }

        cameraError = null
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

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                cameraError = e.localizedMessage ?: "Unable to start the camera. Check permission and close other apps using it."
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(isCameraActive) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
            } catch (e: Exception) {
                // Ignore unbind errors on dispose
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ar_camera_background_layer")
    ) {
        // Fallback Midnight Gradient Background (Visible when camera is OFF)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SlateDark,
                            MidnightNavy,
                            MidnightBlack
                        )
                    )
                )
        )

        // Live Camera Feed Layer
        AnimatedVisibility(
            visible = isCameraActive && hasPermission,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                cameraError?.let { error ->
                    androidx.compose.material3.Text(
                        text = "Camera unavailable: $error",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                            .background(MidnightBlack.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}
