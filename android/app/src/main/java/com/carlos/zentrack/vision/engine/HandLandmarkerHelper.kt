package com.carlos.zentrack.vision.engine

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.carlos.zentrack.vision.data.HandPoint
import com.carlos.zentrack.vision.data.TrackedHand
import com.carlos.zentrack.vision.data.VisionFrameResult
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    private val context: Context,
    var minHandDetectionConfidence: Float = 0.5f,
    var minHandTrackingConfidence: Float = 0.5f,
    var minHandPresenceConfidence: Float = 0.5f,
    var maxNumHands: Int = 1,
    var currentDelegate: Int = DELEGATE_GPU,
    var landmarkerListener: LandmarkerListener? = null
) {
    private var handLandmarker: HandLandmarker? = null
    private var frameCounter = 0
    private var lastFpsCalculationTime = SystemClock.uptimeMillis()
    private var currentFps = 0

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = 0)
        fun onResults(result: VisionFrameResult)
    }

    init {
        setupHandLandmarker()
    }

    fun clearHandLandmarker() {
        try {
            handLandmarker?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing HandLandmarker: ${e.message}")
        }
        handLandmarker = null
    }

    fun isClose(): Boolean = handLandmarker == null

    fun setupHandLandmarker() {
        clearHandLandmarker()

        val baseOptionBuilder = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)

        when (currentDelegate) {
            DELEGATE_CPU -> baseOptionBuilder.setDelegate(Delegate.CPU)
            DELEGATE_GPU -> baseOptionBuilder.setDelegate(Delegate.GPU)
        }

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(minHandDetectionConfidence)
                .setMinTrackingConfidence(minHandTrackingConfidence)
                .setMinHandPresenceConfidence(minHandPresenceConfidence)
                .setNumHands(maxNumHands)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: HandLandmarkerResult, inputImage: MPImage ->
                    val finishTimeMs = SystemClock.uptimeMillis()
                    val inferenceTime = finishTimeMs - result.timestampMs()

                    // Calculate live FPS
                    frameCounter++
                    val now = SystemClock.uptimeMillis()
                    if (now - lastFpsCalculationTime >= 1000) {
                        currentFps = frameCounter
                        frameCounter = 0
                        lastFpsCalculationTime = now
                    }

                    val trackedHands = mutableListOf<TrackedHand>()
                    val landmarksList = result.landmarks()
                    val handednessList = result.handedness()

                    for (i in 0 until landmarksList.size) {
                        val normalizedLandmarks = landmarksList[i]
                        val isRight = if (i < handednessList.size && handednessList[i].isNotEmpty()) {
                            handednessList[i][0].displayName().equals("Right", ignoreCase = true)
                        } else true
                        val confidence = if (i < handednessList.size && handednessList[i].isNotEmpty()) {
                            handednessList[i][0].score()
                        } else 0.9f

                        val points = normalizedLandmarks.map { lm ->
                            HandPoint(x = lm.x(), y = lm.y(), z = lm.z())
                        }
                        trackedHands.add(TrackedHand(landmarks = points, isRightHand = isRight, confidence = confidence))
                    }

                    val frameResult = VisionFrameResult(
                        hands = trackedHands,
                        inferenceTimeMs = inferenceTime,
                        fps = currentFps,
                        timestamp = finishTimeMs
                    )

                    landmarkerListener?.onResults(frameResult)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "MediaPipe LiveStream error: ${error.message}")
                    landmarkerListener?.onError(error.message ?: "Unknown MediaPipe error")
                }

            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
            Log.d(TAG, "HandLandmarker successfully initialized on delegate: $currentDelegate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HandLandmarker: ${e.message}")
            if (currentDelegate == DELEGATE_GPU) {
                Log.w(TAG, "Falling back to CPU Delegate...")
                currentDelegate = DELEGATE_CPU
                setupHandLandmarker()
            } else {
                landmarkerListener?.onError("HandLandmarker error: ${e.message}")
            }
        }
    }

    fun detectLiveStream(image: MPImage, rotationDegrees: Int, frameTimeMs: Long) {
        val imageProcessingOptions = com.google.mediapipe.tasks.vision.core.ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()
        handLandmarker?.detectAsync(image, imageProcessingOptions, frameTimeMs)
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        private const val MODEL_ASSET_PATH = "hand_landmarker.task"
        private const val TAG = "HandLandmarkerHelper"
    }
}
