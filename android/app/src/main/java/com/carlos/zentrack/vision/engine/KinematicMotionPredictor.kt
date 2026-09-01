package com.carlos.zentrack.vision.engine

import android.os.SystemClock

/**
 * Kinematic Motion Predictor (Apple Vision Pro & Meta Quest style).
 * Combines 1€ Adaptive Filtering with Forward Quadratic Extrapolation (Dead Reckoning).
 * Compensates for optical pipeline latency and synthesizes 500 Hz micro-deltas.
 */
class KinematicMotionPredictor {
    private val filterX = OneEuroFilter(minCutoff = 1.2f, beta = 0.015f)
    private val filterY = OneEuroFilter(minCutoff = 1.2f, beta = 0.015f)

    @Volatile private var hasAnchor = false
    @Volatile private var lastAnchorTime = 0L

    // Filtered anchor state
    private var anchorX = 0f
    private var anchorY = 0f

    // Velocity (normalized units per millisecond)
    private var velX = 0f
    private var velY = 0f

    // Acceleration (normalized units per ms^2)
    private var accX = 0f
    private var accY = 0f

    // Last emitted projected point to calculate incremental deltas
    private var lastEmittedX = 0f
    private var lastEmittedY = 0f
    private var isFirstEmission = true

    // Sub-pixel accumulator to prevent integer truncation drift
    private var subpixelAccumX = 0f
    private var subpixelAccumY = 0f

    fun updateAnchor(rawX: Float, rawY: Float, timestampMs: Long = SystemClock.uptimeMillis()) {
        val filtX = filterX.filter(rawX, timestampMs)
        val filtY = filterY.filter(rawY, timestampMs)

        if (!hasAnchor) {
            anchorX = filtX
            anchorY = filtY
            velX = 0f
            velY = 0f
            accX = 0f
            accY = 0f
            lastAnchorTime = timestampMs
            lastEmittedX = filtX
            lastEmittedY = filtY
            isFirstEmission = true
            hasAnchor = true
            return
        }

        val dt = (timestampMs - lastAnchorTime).coerceAtLeast(1L).toFloat() // ms
        val newVelX = (filtX - anchorX) / dt
        val newVelY = (filtY - anchorY) / dt

        // Low-pass filter velocity & acceleration
        val newAccX = (newVelX - velX) / dt
        val newAccY = (newVelY - velY) / dt

        velX = 0.7f * velX + 0.3f * newVelX
        velY = 0.7f * velY + 0.3f * newVelY

        accX = 0.8f * accX + 0.2f * newAccX
        accY = 0.8f * accY + 0.2f * newAccY

        anchorX = filtX
        anchorY = filtY
        lastAnchorTime = timestampMs
    }

    /**
     * Called at 500 Hz by the emitter thread.
     * Projects the hand position forward by (tau + leadTimeMs) and returns the incremental (dx, dy).
     */
    fun evaluateIncrementalDelta(
        currentTimeMs: Long = SystemClock.uptimeMillis(),
        leadTimeMs: Float = 28.0f,
        sensitivity: Float = 1.0f,
        mirrorX: Boolean = true
    ): Pair<Int, Int>? {
        if (!hasAnchor) return null

        val tau = (currentTimeMs - lastAnchorTime).toFloat()

        // If no new camera frame for more than 200ms, hand was lost -> reset
        if (tau > 200f) {
            reset()
            return null
        }

        // Forward Projective Kinematic Equation: P(t) = P0 + V * t + 0.5 * A * t^2
        val tPred = (tau + leadTimeMs).coerceAtLeast(0f)
        val damping = (1.0f - (tau / 200f).coerceIn(0f, 1f)) // Damp velocity if frame delayed

        val projX = anchorX + (velX * tPred + 0.5f * accX * tPred * tPred) * damping
        val projY = anchorY + (velY * tPred + 0.5f * accY * tPred * tPred) * damping

        if (isFirstEmission) {
            lastEmittedX = projX
            lastEmittedY = projY
            isFirstEmission = false
            return null
        }

        var deltaNormX = projX - lastEmittedX
        var deltaNormY = projY - lastEmittedY

        lastEmittedX = projX
        lastEmittedY = projY

        if (mirrorX) {
            deltaNormX = -deltaNormX
        }

        // Map normalized delta to mouse pixels with sensitivity scaling
        val baseScale = 2200.0f * sensitivity
        val rawPx = deltaNormX * baseScale + subpixelAccumX
        val rawPy = deltaNormY * baseScale + subpixelAccumY

        val intDx = rawPx.toInt()
        val intDy = rawPy.toInt()

        subpixelAccumX = rawPx - intDx
        subpixelAccumY = rawPy - intDy

        if (intDx == 0 && intDy == 0) return null

        return Pair(intDx, intDy)
    }

    fun reset() {
        hasAnchor = false
        lastAnchorTime = 0L
        velX = 0f
        velY = 0f
        accX = 0f
        accY = 0f
        subpixelAccumX = 0f
        subpixelAccumY = 0f
        isFirstEmission = true
        filterX.reset()
        filterY.reset()
    }
}
