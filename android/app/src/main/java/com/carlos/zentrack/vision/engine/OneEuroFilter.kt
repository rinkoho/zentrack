package com.carlos.zentrack.vision.engine

import kotlin.math.PI
import kotlin.math.abs

/**
 * One Euro (1€) Filter - Industry Standard Adaptive Low-Pass Filter for VR/AR.
 * Dynamically adjusts cutoff frequency based on velocity:
 * - High jitter suppression when stationary (zero jitter)
 * - Zero phase-lag during high-speed jumps (maximum responsiveness)
 */
class OneEuroFilter(
    private var minCutoff: Float = 1.0f,
    private var beta: Float = 0.007f,
    private var dCutoff: Float = 1.0f
) {
    private var xPrev = 0f
    private var dxPrev = 0f
    private var tPrev = -1L

    fun filter(value: Float, timestampMs: Long): Float {
        if (tPrev < 0) {
            xPrev = value
            dxPrev = 0f
            tPrev = timestampMs
            return value
        }

        val dt = ((timestampMs - tPrev) / 1000.0f).coerceIn(0.001f, 0.1f)
        tPrev = timestampMs

        // Derivative (Velocity) calculation
        val dx = (value - xPrev) / dt
        val edx = lowPassFilter(dx, dxPrev, alpha(dt, dCutoff))
        dxPrev = edx

        // Adaptive cutoff based on velocity
        val cutoff = minCutoff + beta * abs(edx)
        val filtered = lowPassFilter(value, xPrev, alpha(dt, cutoff))
        xPrev = filtered

        return filtered
    }

    fun reset() {
        tPrev = -1L
        xPrev = 0f
        dxPrev = 0f
    }

    private fun alpha(dt: Float, cutoff: Float): Float {
        val tau = 1.0f / (2.0f * PI.toFloat() * cutoff)
        return 1.0f / (1.0f + tau / dt)
    }

    private fun lowPassFilter(x: Float, xPrev: Float, a: Float): Float {
        return a * x + (1.0f - a) * xPrev
    }
}
