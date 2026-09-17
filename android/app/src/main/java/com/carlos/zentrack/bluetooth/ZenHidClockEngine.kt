package com.carlos.zentrack.bluetooth

import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

/**
 * High-Precision Monotonic Cadence Engine & Asynchronous Transmit Decoupler
 *
 * Eliminates Bluetooth HID Stutter through:
 * 1. Monotonic Nanosecond Clock: uses SystemClock.elapsedRealtimeNanos() instead of wall-clock.
 * 2. Phase-Drift Compensation: catches up after OS scheduling hiccups without accumulating phase error.
 * 3. Asynchronous Transmit Decoupling: executes blocking Binder IPC (sendReport) on an isolated
 *    worker thread so Binder IPC pauses (10-25ms) never stall the precision clock.
 */
class ZenHidClockEngine(
    private val name: String,
    private val targetIntervalNs: Long, // e.g. 8_000_000L for 8.0ms (125Hz) or 10_000_000L for 10ms (100Hz)
    private val onTick: () -> ByteArray?,
    private val onTransmit: (ByteArray) -> Unit
) {
    companion object {
        private const val TAG = "ZenHidClockEngine"
        private const val MAX_DRIFT_CORRECTION_NS = 40_000_000L // 40ms safety cap
    }

    @Volatile
    private var isRunning = false
    private var clockThread: Thread? = null

    // Single-slot handoff queue: if the radio stalls, it decouples the clock from Binder IPC
    private val transmitQueue = ArrayBlockingQueue<ByteArray>(1)

    // Loss-free delta restoration callback (restores dropped deltas back to ZenHidDeltaDistributor)
    var onReportDropped: ((dx: Int, dy: Int, wheel: Int, pan: Int) -> Unit)? = null

    // Keyboard atomic radio priority pause timestamp
    @Volatile
    private var pauseUntilNs = 0L

    // Dedicated single-thread transmitter isolated from the clock
    private val transmitExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "$name-Transmitter").apply {
            priority = Thread.MAX_PRIORITY
        }
    }

    /**
     * Temporarily pauses mouse generation on the clock thread so high-priority
     * bursts (e.g. keyboard KeyDown / KeyUp) have 100% exclusive radio bandwidth and ACL buffer.
     * Mouse deltas continue accumulating safely inside ZenHidDeltaDistributor without loss.
     */
    fun notifyKeyboardActivity(pauseDurationMs: Long = 40L) {
        val until = SystemClock.elapsedRealtimeNanos() + (pauseDurationMs * 1_000_000L)
        if (until > pauseUntilNs) {
            pauseUntilNs = until
        }
    }

    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true
        pauseUntilNs = 0L

        // 1. Start the Transmit Worker Loop
        transmitExecutor.execute {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            } catch (e: Exception) {
                Log.w(TAG, "[$name] Could not elevate transmitter priority: ${e.message}")
            }

            while (isRunning) {
                try {
                    val report = transmitQueue.poll(200, TimeUnit.MILLISECONDS)
                    if (report != null) {
                        onTransmit(report)
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "[$name] Transmit error", e)
                }
            }
        }

        // 2. Start the Monotonic Precision Clock Loop
        clockThread = Thread({
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            } catch (e: Exception) {
                Log.w(TAG, "[$name] Could not elevate clock priority: ${e.message}")
            }

            var nextTickNs = SystemClock.elapsedRealtimeNanos()

            while (isRunning) {
                val nowNs = SystemClock.elapsedRealtimeNanos()

                // If scheduled tick is in the future, sleep precisely using parkNanos
                val diffNs = nextTickNs - nowNs
                if (diffNs > 0) {
                    LockSupport.parkNanos(diffNs)
                }

                // Check if still running after unparking
                if (!isRunning) break

                val currentElapsed = SystemClock.elapsedRealtimeNanos()

                // 1. Keyboard Atomic Priority: If keyboard has requested priority,
                // pause mouse packet emission to keep Bluetooth radio buffer 100% clear
                if (currentElapsed < pauseUntilNs) {
                    nextTickNs += targetIntervalNs
                    continue
                }

                // 2. Loss-Free Queue Retention: If transmitQueue already has a pending report
                // waiting for the transmitter, do NOT call onTick(). This preserves accumulated motion
                // safely inside ZenHidDeltaDistributor's sub-pixel accumulator until the radio is ready!
                if (transmitQueue.remainingCapacity() == 0) {
                    val nowAfter = SystemClock.elapsedRealtimeNanos()
                    nextTickNs = if (nowAfter >= nextTickNs) nowAfter + targetIntervalNs else nextTickNs + targetIntervalNs
                    continue
                }

                // Generate report on tick
                val report = try {
                    onTick()
                } catch (e: Exception) {
                    Log.e(TAG, "[$name] Error in onTick callback", e)
                    null
                }

                if (report != null) {
                    // Non-blocking offer: if transmitter is busy with previous packet,
                    // restore discarded deltas back to distributor so no motion is ever lost
                    if (!transmitQueue.offer(report)) {
                        val dropped = transmitQueue.poll()
                        if (dropped != null && dropped.size >= 5) {
                            onReportDropped?.invoke(
                                dropped[1].toInt(),
                                dropped[2].toInt(),
                                dropped[3].toInt(),
                                dropped[4].toInt()
                            )
                        }
                        transmitQueue.offer(report)
                    }
                }

                // Strict Cadence Enforcement:
                // Real-time HID pointer input must NEVER burst at 0ms to "catch up" on missed ticks!
                // Every single packet must be separated by at least targetIntervalNs (8.0ms).
                val currentNow = SystemClock.elapsedRealtimeNanos()
                if (currentNow >= nextTickNs) {
                    nextTickNs = currentNow + targetIntervalNs
                } else {
                    nextTickNs += targetIntervalNs
                }
            }
        }, "$name-Clock").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

        Log.i(TAG, "[$name] Clock Engine started (${targetIntervalNs / 1_000_000.0}ms interval)")
    }

    @Synchronized
    fun stop() {
        isRunning = false
        pauseUntilNs = 0L
        clockThread?.interrupt()
        clockThread = null
        transmitQueue.clear()
        Log.i(TAG, "[$name] Clock Engine stopped")
    }
}
