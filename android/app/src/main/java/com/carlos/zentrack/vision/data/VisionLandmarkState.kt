package com.carlos.zentrack.vision.data

data class HandPoint(
    val x: Float, // Normalized 0.0 .. 1.0 (X coordinate)
    val y: Float, // Normalized 0.0 .. 1.0 (Y coordinate)
    val z: Float  // Normalized depth relative to wrist
)

data class TrackedHand(
    val landmarks: List<HandPoint>,
    val isRightHand: Boolean,
    val confidence: Float
) {
    val wrist: HandPoint? get() = landmarks.getOrNull(0)
    val thumbTip: HandPoint? get() = landmarks.getOrNull(4)
    val indexTip: HandPoint? get() = landmarks.getOrNull(8)
    val middleTip: HandPoint? get() = landmarks.getOrNull(12)
    val ringTip: HandPoint? get() = landmarks.getOrNull(16)
    val pinkyTip: HandPoint? get() = landmarks.getOrNull(20)
}

data class VisionFrameResult(
    val hands: List<TrackedHand> = emptyList(),
    val inferenceTimeMs: Long = 0L,
    val fps: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

object HandSkeleton {
    // Standard 21 MediaPipe Hand Landmark Connections
    val CONNECTIONS = listOf(
        // Thumb
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        // Index finger
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        // Middle finger
        5 to 9, 9 to 10, 10 to 11, 11 to 12,
        // Ring finger
        9 to 13, 13 to 14, 14 to 15, 15 to 16,
        // Pinky
        13 to 17, 17 to 18, 18 to 19, 19 to 20,
        // Palm base
        0 to 17
    )

    val FINGERTIP_INDICES = setOf(4, 8, 12, 16, 20)
    val KNUCKLE_INDICES = setOf(5, 9, 13, 17)
}
