package com.carlos.zentrack.vision.ui.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.carlos.zentrack.vision.engine.HandLandmarkerHelper
import com.google.mediapipe.framework.image.MediaImageBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun VisionCameraPreview(
    lensFacing: Int,
    handLandmarkerHelper: HandLandmarkerHelper,
    renderMode: String = "performance",
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(lensFacing, renderMode, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(
                context = context,
                cameraProvider = cameraProvider,
                lifecycleOwner = lifecycleOwner,
                previewView = pv,
                lensFacing = lensFacing,
                renderMode = renderMode,
                cameraExecutor = cameraExecutor,
                handLandmarkerHelper = handLandmarkerHelper
            )
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.scaleType = scaleType
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                previewView = this
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun bindCameraUseCases(
    context: Context,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int,
    renderMode: String,
    cameraExecutor: ExecutorService,
    handLandmarkerHelper: HandLandmarkerHelper
) {
    try {
        cameraProvider.unbindAll()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // 1. Configure Resolution Strategy based on renderMode
        val targetResolution = when (renderMode) {
            "hd" -> android.util.Size(1920, 1080)
            "performance" -> android.util.Size(640, 360)
            else -> android.util.Size(640, 360) // "skeleton"
        }

        val resolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
            .setAspectRatioStrategy(androidx.camera.core.resolutionselector.AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                androidx.camera.core.resolutionselector.ResolutionStrategy(
                    targetResolution,
                    androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                )
            )
            .build()

        // 2. Preview use case
        val previewBuilder = Preview.Builder()
            .setResolutionSelector(resolutionSelector)

        // 3. ImageAnalysis use case with zero-copy stream
        val analysisBuilder = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)

        // Find and enforce the maximum supported FPS range for this sensor
        var selectedFpsRange = android.util.Range(30, 30)
        try {
            val cameraInfoList = cameraProvider.availableCameraInfos
            for (cInfo in cameraInfoList) {
                val c2Info = androidx.camera.camera2.interop.Camera2CameraInfo.from(cInfo)
                val facing = c2Info.getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                if (facing == lensFacing) {
                    val ranges = c2Info.getCameraCharacteristic(
                        android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
                    )
                    if (ranges != null && ranges.isNotEmpty()) {
                        // Find highest FPS range with highest lower bound (e.g. [60,60] or [30,60])
                        val bestRange = ranges
                            .filter { it.upper >= 30 }
                            .maxByOrNull { it.upper * 1000 + it.lower }
                        if (bestRange != null) {
                            selectedFpsRange = bestRange
                        }
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.w("VisionCameraPreview", "Error reading camera characteristics: ${e.message}")
        }

        Log.d("VisionCameraPreview", "Applying strict FPS range: $selectedFpsRange")

        // Request high FPS range via Camera2Interop
        try {
            val extPreview = androidx.camera.camera2.interop.Camera2Interop.Extender(previewBuilder)
            extPreview.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                selectedFpsRange
            )
            extPreview.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                android.hardware.camera2.CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF
            )

            val extAnalysis = androidx.camera.camera2.interop.Camera2Interop.Extender(analysisBuilder)
            extAnalysis.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                selectedFpsRange
            )
            extAnalysis.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                android.hardware.camera2.CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF
            )
        } catch (e: Exception) {
            Log.w("VisionCameraPreview", "Camera2Interop FPS range set failed: ${e.message}")
        }

        val preview = previewBuilder.build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis = analysisBuilder.build()
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy, handLandmarkerHelper)
        }

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
        Log.d("VisionCameraPreview", "Camera bound successfully in mode: $renderMode, lensFacing: $lensFacing")
    } catch (exc: Exception) {
        Log.e("VisionCameraPreview", "Use case binding failed", exc)
    }
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    handLandmarkerHelper: HandLandmarkerHelper
) {
    val frameTime = android.os.SystemClock.uptimeMillis()
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        try {
            val mpImage = MediaImageBuilder(mediaImage).build()
            handLandmarkerHelper.detectLiveStream(mpImage, imageProxy.imageInfo.rotationDegrees, frameTime)
        } catch (e: Exception) {
            Log.e("VisionCameraPreview", "Error converting frame to MPImage: ${e.message}")
        }
    }
    imageProxy.close()
}
