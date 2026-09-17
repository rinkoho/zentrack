package com.carlos.zentrack.bluetooth

import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Velocity-Adaptive One Euro Filter for 2D Pointer Motion
 */
private class HidOneEuroFilter(
    private val minCutoff: Float = 1.2f, // Suppresses micro-hand tremor and AMOLED digitizer noise
    private val beta: Float = 0.015f,     // Rapidly opens up on high velocity for 0ms lag
    private val dCutoff: Float = 1.0f
) {
    private var xPrev = 0f
    private var dxPrev = 0f
    private var tPrevNs = -1L

    fun filter(value: Float, timestampNs: Long): Float {
        if (tPrevNs < 0) {
            xPrev = value
            dxPrev = 0f
            tPrevNs = timestampNs
            return value
        }

        val dt = ((timestampNs - tPrevNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)
        tPrevNs = timestampNs

        val dx = (value - xPrev) / dt
        val edx = lowPass(dx, dxPrev, alpha(dt, dCutoff))
        dxPrev = edx

        val cutoff = minCutoff + beta * abs(edx)
        val filtered = lowPass(value, xPrev, alpha(dt, cutoff))
        xPrev = filtered
        return filtered
    }

    fun reset() {
        tPrevNs = -1L
        xPrev = 0f
        dxPrev = 0f
    }

    private fun alpha(dt: Float, cutoff: Float): Float {
        val tau = 1.0f / (2.0f * PI.toFloat() * cutoff)
        return 1.0f / (1.0f + tau / dt)
    }

    private fun lowPass(x: Float, xPrev: Float, a: Float): Float {
        return a * x + (1.0f - a) * xPrev
    }
}

/**
 * Token Bucket Sub-Pixel Fractional Motion Distributor
 *
 * Replaces naive integer rounding and erratic burst flushing with:
 * 1. Continuous Fractional Accumulation (Zero pixel-dropping / staircase quantization).
 * 2. Velocity-Adaptive Jitter Suppression (One Euro filter).
 * 3. Token-Bucket Slew Rate Limiter (Prevents sudden 3x-4x acceleration spikes after RF pauses).
 */
class ZenHidDeltaDistributor {

    companion object {
        // Anti-Sniff Keep-Alive: 250ms interval (4 pulses/sec) ensures host Link Manager
        // inactivity timer never expires, while keeping RF spectrum completely clear
        // for instant, uncontested keyboard key events.
        private const val HEARTBEAT_INTERVAL_NS = 250_000_000L
    }

    private val lock = Any()

    // Non-blocking UI Thread accumulators
    private var pendingDx = 0f
    private var pendingDy = 0f
    private var pendingScrollY = 0f
    private var pendingScrollX = 0f
    private var currentButtonsMask: Byte = 0
    private var hasPendingMotion = false

    // Sub-pixel continuous fractional remainders
    private var subPixelDx = 0f
    private var subPixelDy = 0f
    private var subPixelScrollY = 0f
    private var subPixelScrollX = 0f
    private var lastEmittedButtons: Byte = 0

    // Anti-Sniff Heartbeat state
    var heartbeatEnabled = true
    private var lastReportSentNs = 0L

    // Velocity-adaptive filters
    private val filterX = HidOneEuroFilter()
    private val filterY = HidOneEuroFilter()

    // Non-blocking UI Producer (<0.001ms)
    fun pushMotion(dx: Float, dy: Float) {
        synchronized(lock) {
            pendingDx += dx
            pendingDy += dy
            hasPendingMotion = true
        }
    }

    fun pushScroll(scrollY: Float, scrollX: Float = 0f) {
        synchronized(lock) {
            pendingScrollY += scrollY
            pendingScrollX += scrollX
            hasPendingMotion = true
        }
    }

    fun setButtons(mask: Byte) {
        synchronized(lock) {
            currentButtonsMask = mask
            hasPendingMotion = true
        }
    }

    /**
     * Loss-Free Delta Restoration:
     * Restores untransmitted deltas back into the sub-pixel accumulator if an RF or queue
     * stall ever drops an outgoing packet, guaranteeing zero lost cursor distance.
     */
    fun restoreMotion(dx: Int, dy: Int, wheel: Int = 0, pan: Int = 0) {
        synchronized(lock) {
            subPixelDx += dx.toFloat()
            subPixelDy += dy.toFloat()
            subPixelScrollY += wheel.toFloat()
            subPixelScrollX += pan.toFloat()
            hasPendingMotion = true
        }
    }

    fun reset() {
        synchronized(lock) {
            pendingDx = 0f
            pendingDy = 0f
            pendingScrollY = 0f
            pendingScrollX = 0f
            hasPendingMotion = false
            subPixelDx = 0f
            subPixelDy = 0f
            subPixelScrollY = 0f
            subPixelScrollX = 0f
            lastEmittedButtons = 0
            lastReportSentNs = SystemClock.elapsedRealtimeNanos()
            filterX.reset()
            filterY.reset()
        }
    }

    /**
     * Consumes accumulated motion on clock tick and packs into a 5-byte mouse report buffer.
     * Returns true if there was any delta, button change, or keep-alive heartbeat to transmit.
     */
    fun produceMouseReport(outputBuffer: ByteArray): Boolean {
        var rawDx: Float
        var rawDy: Float
        var rawSY: Float
        var rawSX: Float
        var buttons: Byte

        synchronized(lock) {
            rawDx = pendingDx
            rawDy = pendingDy
            rawSY = pendingScrollY
            rawSX = pendingScrollX
            buttons = currentButtonsMask

            pendingDx = 0f
            pendingDy = 0f
            pendingScrollY = 0f
            pendingScrollX = 0f
            hasPendingMotion = false
        }

        val nowNs = SystemClock.elapsedRealtimeNanos()

        // Apply One-Euro filter if there is active motion, otherwise decay smoothly
        val filteredDx = if (rawDx != 0f) filterX.filter(rawDx, nowNs) else 0f
        val filteredDy = if (rawDy != 0f) filterY.filter(rawDy, nowNs) else 0f

        subPixelDx += filteredDx
        subPixelDy += filteredDy
        subPixelScrollY += rawSY
        subPixelScrollX += rawSX

        // Human-Ergonomic Slew-Rate Token Bucket:
        // Tuned for 11.25ms frames (~88.88Hz). Caps maximum step per tick to 18px (~1600 px/s).
        // If an RF pause accumulated distance, it glides out continuously across
        // consecutive frames at natural speeds, completely eliminating violent speed spasms.
        val currentSpeed = hypot(filteredDx, filteredDy)
        val maxStep = if (currentSpeed > 0.1f) (currentSpeed * 1.35f).coerceIn(10f, 18f) else 16f

        val stepDx = subPixelDx.coerceIn(-maxStep, maxStep)
        val stepDy = subPixelDy.coerceIn(-maxStep, maxStep)

        val intDx = stepDx.roundToInt()
        val intDy = stepDy.roundToInt()
        val intWheel = subPixelScrollY.roundToInt()
        val intPan = subPixelScrollX.roundToInt()
        val buttonChanged = buttons != lastEmittedButtons

        if (intDx != 0 || intDy != 0 || intWheel != 0 || intPan != 0 || buttonChanged) {
            subPixelDx -= intDx.toFloat()
            subPixelDy -= intDy.toFloat()
            subPixelScrollY -= intWheel.toFloat()
            subPixelScrollX -= intPan.toFloat()
            lastEmittedButtons = buttons
            lastReportSentNs = nowNs

            outputBuffer[0] = buttons
            outputBuffer[1] = intDx.coerceIn(-127, 127).toByte()
            outputBuffer[2] = intDy.coerceIn(-127, 127).toByte()
            outputBuffer[3] = intWheel.coerceIn(-127, 127).toByte()
            outputBuffer[4] = intPan.coerceIn(-127, 127).toByte()
            return true
        }

        // Anti-Sniff Keep-Alive Pulse:
        // When the mouse is stationary for >= 80ms, transmit a null report [buttons, 0, 0, 0, 0]
        // to reset the host's Link Manager inactivity timer, permanently keeping
        // the Bluetooth radio in ACTIVE MODE (mode=0) and preventing 448ms Sniff Mode.
        if (heartbeatEnabled && lastReportSentNs > 0L && (nowNs - lastReportSentNs >= HEARTBEAT_INTERVAL_NS)) {
            lastReportSentNs = nowNs
            outputBuffer[0] = lastEmittedButtons
            outputBuffer[1] = 0
            outputBuffer[2] = 0
            outputBuffer[3] = 0
            outputBuffer[4] = 0
            return true
        }

        return false
    }
}
