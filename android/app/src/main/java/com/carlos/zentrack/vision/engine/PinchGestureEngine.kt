package com.carlos.zentrack.vision.engine

import com.carlos.zentrack.vision.data.TrackedHand
import kotlin.math.sqrt

class PinchGestureEngine(
    private val onPinchDown: () -> Unit,
    private val onPinchUp: () -> Unit
) {
    private var isPinched = false
    private val pinchThresholdDown = 0.052f
    private val pinchThresholdUp = 0.078f

    fun processHand(hand: TrackedHand?) {
        if (hand == null || hand.thumbTip == null || hand.indexTip == null) {
            if (isPinched) {
                isPinched = false
                onPinchUp()
            }
            return
        }

        val thb = hand.thumbTip!!
        val idx = hand.indexTip!!

        val dx = idx.x - thb.x
        val dy = idx.y - thb.y
        val dz = idx.z - thb.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (!isPinched && distance < pinchThresholdDown) {
            isPinched = true
            onPinchDown()
        } else if (isPinched && distance > pinchThresholdUp) {
            isPinched = false
            onPinchUp()
        }
    }

    fun reset() {
        if (isPinched) {
            isPinched = false
            onPinchUp()
        }
    }
}
