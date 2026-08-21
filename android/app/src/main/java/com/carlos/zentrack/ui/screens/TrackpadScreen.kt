package com.carlos.zentrack.ui.screens

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.ui.components.*
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

data class TrackpadPointer(
    var currentX: Float,
    var currentY: Float,
    var prevX: Float,
    var prevY: Float,
    var startX: Float,
    var startY: Float,
    var startTime: Long
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrackpadScreen(
    currentTheme: ZenThemeConfig,
    isConnected: Boolean,
    statusText: String,
    sensitivity: Float,
    scrollSensitivity: Float,
    mouseAccelEnabled: Boolean,
    mouseAccelProfile: String = com.carlos.zentrack.preferences.ZenPreferences.mouseAccelProfile,
    naturalScroll: Boolean,
    invertThreeFingerSwipe: Boolean = com.carlos.zentrack.preferences.ZenPreferences.invertThreeFingerSwipe,
    isCompactMode: Boolean = false,
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Multi-touch Pointer Map for Trackpad Surface
    val trackpadPointers = remember { mutableMapOf<Int, TrackpadPointer>() }

    var tapStartX by remember { mutableFloatStateOf(0f) }
    var tapStartY by remember { mutableFloatStateOf(0f) }
    var touchDownTime by remember { mutableLongStateOf(0L) }
    var isTapCandidate by remember { mutableStateOf(true) }
    var isDraggingMode by remember { mutableStateOf(false) }

    var lastScrollX by remember { mutableFloatStateOf(0f) }
    var lastScrollY by remember { mutableFloatStateOf(0f) }
    var twoFingerStartX by remember { mutableFloatStateOf(0f) }
    var twoFingerStartY by remember { mutableFloatStateOf(0f) }
    var twoFingerTapStartTime by remember { mutableLongStateOf(0L) }
    var isTwoFingerTapCandidate by remember { mutableStateOf(false) }
    var hasHadTwoFingers by remember { mutableStateOf(false) }

    var threeFingerStartX by remember { mutableFloatStateOf(0f) }
    var isThreeFingerSwipeCandidate by remember { mutableStateOf(false) }

    // Sub-pixel Floating Point Accumulators for High-Hz Touch Screens
    var subPixelRemainderX by remember { mutableFloatStateOf(0f) }
    var subPixelRemainderY by remember { mutableFloatStateOf(0f) }
    var scrollRemainderX by remember { mutableFloatStateOf(0f) }
    var scrollRemainderY by remember { mutableFloatStateOf(0f) }

    // Timers
    var dragRunnable by remember { mutableStateOf<Runnable?>(null) }
    var twoFingerDragRunnable by remember { mutableStateOf<Runnable?>(null) }
    var clickRunnable by remember { mutableStateOf<Runnable?>(null) }

    var isRightDraggingMode by remember { mutableStateOf(false) }

    fun cancelDragTimer() {
        dragRunnable?.let { mainHandler.removeCallbacks(it) }
        dragRunnable = null
    }

    fun cancelTwoFingerDragTimer() {
        twoFingerDragRunnable?.let { mainHandler.removeCallbacks(it) }
        twoFingerDragRunnable = null
    }

    fun cancelClickTimer() {
        clickRunnable?.let { mainHandler.removeCallbacks(it) }
        clickRunnable = null
    }

    Row(modifier = Modifier.fillMaxSize()) {

        // -------------------------------------------------------------
        // CENTER AREA: Giant Main Trackpad Surface
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(currentTheme.background)
                .border(1.dp, currentTheme.card)
        ) {
            // Background Dot Matrix Grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 30.dp.toPx()
                val dotRadius = 1.2.dp.toPx()
                val cols = (size.width / gridSpacing).toInt()
                val rows = (size.height / gridSpacing).toInt()

                for (i in 0..cols) {
                    for (j in 0..rows) {
                        drawCircle(
                            color = currentTheme.primaryAccent.copy(alpha = 0.12f),
                            radius = dotRadius,
                            center = Offset(i * gridSpacing, j * gridSpacing)
                        )
                    }
                }
            }

            // Interactive Trackpad Surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { motionEvent ->
                        val action = motionEvent.actionMasked

                        // Exclude top-left menu pill touch zone so button clicks are passed directly to Compose clickable
                        if (action == MotionEvent.ACTION_DOWN && motionEvent.x < 380f && motionEvent.y < 160f) {
                            return@pointerInteropFilter false
                        }

                        val actionIdx = motionEvent.actionIndex
                        val actionPointerId = motionEvent.getPointerId(actionIdx)


                        when (action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                                val x = motionEvent.getX(actionIdx)
                                val y = motionEvent.getY(actionIdx)
                                trackpadPointers[actionPointerId] = TrackpadPointer(x, y, x, y, x, y, System.currentTimeMillis())

                                if (trackpadPointers.size == 1) {
                                    tapStartX = x
                                    tapStartY = y
                                    touchDownTime = System.currentTimeMillis()
                                    isTapCandidate = true
                                    hasHadTwoFingers = false

                                    subPixelRemainderX = 0f
                                    subPixelRemainderY = 0f
                                    scrollRemainderX = 0f
                                    scrollRemainderY = 0f

                                    cancelDragTimer()
                                    val r = Runnable {
                                        if (isTapCandidate && !hasHadTwoFingers && trackpadPointers.size == 1) {
                                            isDraggingMode = true
                                            isTapCandidate = false
                                            cancelClickTimer()
                                            val json = JSONObject().put("type", "mousedown").put("button", 1)
                                            onSendJson(json.toString())
                                            onVibrate(40L)
                                        }
                                    }
                                    dragRunnable = r
                                    mainHandler.postDelayed(r, 250L)
                                } else if (trackpadPointers.size == 2) {
                                    cancelDragTimer()
                                    cancelTwoFingerDragTimer()
                                    isTapCandidate = false
                                    hasHadTwoFingers = true
                                    isTwoFingerTapCandidate = true
                                    twoFingerTapStartTime = System.currentTimeMillis()

                                    val coords = trackpadPointers.values.toList()
                                    val midX = (coords[0].currentX + coords[1].currentX) / 2f
                                    val midY = (coords[0].currentY + coords[1].currentY) / 2f
                                    twoFingerStartX = midX
                                    twoFingerStartY = midY
                                    lastScrollX = midX
                                    lastScrollY = midY

                                    val r2 = Runnable {
                                        if (isTwoFingerTapCandidate && trackpadPointers.size == 2) {
                                            isRightDraggingMode = true
                                            isTwoFingerTapCandidate = false
                                            cancelClickTimer()
                                            val json = JSONObject().put("type", "mousedown").put("button", 3)
                                            onSendJson(json.toString())
                                            onVibrate(40L)
                                        }
                                    }
                                    twoFingerDragRunnable = r2
                                    mainHandler.postDelayed(r2, 250L)
                                } else if (trackpadPointers.size == 3) {
                                    cancelDragTimer()
                                    cancelTwoFingerDragTimer()
                                    isTwoFingerTapCandidate = false
                                    isThreeFingerSwipeCandidate = true
                                    val coords = trackpadPointers.values.toList()
                                    if (coords.size >= 3) {
                                        threeFingerStartX = (coords[0].currentX + coords[1].currentX + coords[2].currentX) / 3f
                                    }
                                }
                            }
                            MotionEvent.ACTION_MOVE -> {
                                fun processCursorDelta(dx: Float, dy: Float) {
                                    if (dx != 0f || dy != 0f) {
                                        val rawDx = dx * sensitivity
                                        val rawDy = dy * sensitivity

                                        val velocity = hypot(rawDx, rawDy)
                                        val accelFactor = when (mouseAccelProfile) {
                                            "none" -> 1.0f
                                            "exponential" -> {
                                                (1.0f + 0.22f * Math.pow(velocity.toDouble(), 1.25)).coerceAtMost(3.0).toFloat()
                                            }
                                            "linear_offset_cap" -> {
                                                val threshold = 1.5f
                                                val maxVelocity = 12.0f
                                                val maxCap = 2.2f
                                                when {
                                                    velocity <= threshold -> 1.0f
                                                    velocity >= maxVelocity -> maxCap
                                                    else -> 1.0f + (maxCap - 1.0f) * ((velocity - threshold) / (maxVelocity - threshold))
                                                }
                                            }
                                            else -> if (mouseAccelEnabled) (1.0f + 0.22f * Math.pow(velocity.toDouble(), 1.25)).coerceAtMost(3.0).toFloat() else 1.0f
                                        }

                                        val calcDx = rawDx * accelFactor
                                        val calcDy = rawDy * accelFactor

                                        subPixelRemainderX += calcDx
                                        subPixelRemainderY += calcDy

                                        val sendMx = subPixelRemainderX.toInt()
                                        val sendMy = subPixelRemainderY.toInt()

                                        subPixelRemainderX -= sendMx.toFloat()
                                        subPixelRemainderY -= sendMy.toFloat()

                                        if (sendMx != 0 || sendMy != 0) {
                                            onSendBinary(1, sendMx, sendMy)
                                        }
                                    }
                                }

                                for (i in 0 until motionEvent.pointerCount) {
                                    val pId = motionEvent.getPointerId(i)
                                    val pt = trackpadPointers[pId]
                                    if (pt != null) {
                                        pt.prevX = pt.currentX
                                        pt.prevY = pt.currentY
                                        pt.currentX = motionEvent.getX(i)
                                        pt.currentY = motionEvent.getY(i)
                                    }
                                }

                                if (trackpadPointers.size == 1 && !hasHadTwoFingers) {
                                    val pId = trackpadPointers.keys.first()
                                    val pIdx = motionEvent.findPointerIndex(pId)
                                    val state = trackpadPointers[pId]

                                    if (pIdx >= 0 && state != null) {
                                        val currX = motionEvent.getX(pIdx)
                                        val currY = motionEvent.getY(pIdx)
                                        val totalDist = hypot(currX - tapStartX, currY - tapStartY)

                                        if (totalDist > 12f) {
                                            isTapCandidate = false
                                            cancelDragTimer()
                                        }

                                        // Process all historical micro-step samples captured by hardware digitizer (1000Hz+ boost!)
                                        val historySize = motionEvent.historySize
                                        for (h in 0 until historySize) {
                                            val hX = motionEvent.getHistoricalX(pIdx, h)
                                            val hY = motionEvent.getHistoricalY(pIdx, h)
                                            val stepDx = hX - state.prevX
                                            val stepDy = hY - state.prevY
                                            state.prevX = hX
                                            state.prevY = hY
                                            processCursorDelta(stepDx, stepDy)
                                        }

                                        // Process current (latest) point
                                        val stepDx = currX - state.prevX
                                        val stepDy = currY - state.prevY
                                        state.prevX = currX
                                        state.prevY = currY
                                        processCursorDelta(stepDx, stepDy)
                                    }
                                } else if (trackpadPointers.size == 2) {
                                    val coords = trackpadPointers.values.toList()
                                    val currScrollX = (coords[0].currentX + coords[1].currentX) / 2f
                                    val currScrollY = (coords[0].currentY + coords[1].currentY) / 2f

                                    val moveDist = hypot(currScrollX - twoFingerStartX, currScrollY - twoFingerStartY)
                                    if (moveDist > 16f && !isRightDraggingMode) {
                                        isTwoFingerTapCandidate = false
                                        cancelTwoFingerDragTimer()
                                    }

                                    if (isRightDraggingMode) {
                                        // 2-Finger Right Click Drag: Cursor movement with Right Mouse Button held down
                                        val frameDx = currScrollX - lastScrollX
                                        val frameDy = currScrollY - lastScrollY

                                        if (frameDx != 0f || frameDy != 0f) {
                                            val rawDx = frameDx * sensitivity
                                            val rawDy = frameDy * sensitivity

                                            val velocity = hypot(rawDx, rawDy)
                                            val accelFactor = when (mouseAccelProfile) {
                                                "none" -> 1.0f
                                                "exponential" -> {
                                                    (1.0f + 0.22f * Math.pow(velocity.toDouble(), 1.25)).coerceAtMost(3.0).toFloat()
                                                }
                                                "linear_offset_cap" -> {
                                                    val threshold = 1.5f
                                                    val maxVelocity = 12.0f
                                                    val maxCap = 2.2f
                                                    when {
                                                        velocity <= threshold -> 1.0f
                                                        velocity >= maxVelocity -> maxCap
                                                        else -> 1.0f + (maxCap - 1.0f) * ((velocity - threshold) / (maxVelocity - threshold))
                                                    }
                                                }
                                                else -> if (mouseAccelEnabled) (1.0f + 0.22f * Math.pow(velocity.toDouble(), 1.25)).coerceAtMost(3.0).toFloat() else 1.0f
                                            }

                                            val calcDx = rawDx * accelFactor
                                            val calcDy = rawDy * accelFactor

                                            subPixelRemainderX += calcDx
                                            subPixelRemainderY += calcDy

                                            val sendMx = subPixelRemainderX.toInt()
                                            val sendMy = subPixelRemainderY.toInt()

                                            subPixelRemainderX -= sendMx.toFloat()
                                            subPixelRemainderY -= sendMy.toFloat()

                                            if (sendMx != 0 || sendMy != 0) {
                                                onSendBinary(1, sendMx, sendMy)
                                            }
                                        }
                                    } else {
                                        // 2-Finger Normal Scroll
                                        var dx = (currScrollX - lastScrollX) * sensitivity * scrollSensitivity
                                        var dy = (currScrollY - lastScrollY) * sensitivity * scrollSensitivity

                                        if (naturalScroll) {
                                            dx = -dx
                                            dy = -dy
                                        }

                                        scrollRemainderX += dx
                                        scrollRemainderY += dy

                                        val sendDx = scrollRemainderX.toInt()
                                        val sendDy = scrollRemainderY.toInt()

                                        scrollRemainderX -= sendDx.toFloat()
                                        scrollRemainderY -= sendDy.toFloat()

                                        if (sendDx != 0 || sendDy != 0) {
                                            onSendBinary(2, sendDx * 10, sendDy * 10)
                                        }
                                    }

                                    lastScrollX = currScrollX
                                    lastScrollY = currScrollY
                                } else if (trackpadPointers.size == 3 && isThreeFingerSwipeCandidate) {
                                    val coords = trackpadPointers.values.toList()
                                    val curr3X = (coords[0].currentX + coords[1].currentX + coords[2].currentX) / 3f
                                    val dx = curr3X - threeFingerStartX

                                    if (abs(dx) > 40f) {
                                        isThreeFingerSwipeCandidate = false
                                        val effectiveNatural = if (invertThreeFingerSwipe) !naturalScroll else naturalScroll
                                        val actionStr = if (effectiveNatural) {
                                            if (dx > 0) "workspace_right" else "workspace_left"
                                        } else {
                                            if (dx > 0) "workspace_left" else "workspace_right"
                                        }
                                        val json = JSONObject().put("type", "shortcut").put("action", actionStr)
                                        onSendJson(json.toString())
                                        onVibrate(30L)
                                    }
                                }
                            }

                            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                                cancelDragTimer()
                                cancelTwoFingerDragTimer()
                                trackpadPointers.remove(actionPointerId)

                                if (isDraggingMode) {
                                    isDraggingMode = false
                                    val json = JSONObject().put("type", "mouseup").put("button", 1)
                                    onSendJson(json.toString())
                                    onVibrate(15L)
                                    return@pointerInteropFilter true
                                }

                                if (isRightDraggingMode) {
                                    if (trackpadPointers.size < 2) {
                                        isRightDraggingMode = false
                                        val json = JSONObject().put("type", "mouseup").put("button", 3)
                                        onSendJson(json.toString())
                                        onVibrate(15L)
                                        return@pointerInteropFilter true
                                    }
                                }

                                if (hasHadTwoFingers) {
                                    val duration = System.currentTimeMillis() - twoFingerTapStartTime
                                    if (isTwoFingerTapCandidate && duration < 300 && !isRightDraggingMode) {
                                        cancelClickTimer()
                                        val json = JSONObject().put("type", "click").put("button", 3)
                                        onSendJson(json.toString())
                                        onVibrate(25L)
                                        isTwoFingerTapCandidate = false
                                    }
                                } else {
                                    val duration = System.currentTimeMillis() - touchDownTime
                                    if (isTapCandidate && duration < 200) {
                                        cancelClickTimer()
                                        val json = JSONObject().put("type", "click").put("button", 1)
                                        onSendJson(json.toString())
                                        onVibrate(15L)
                                        isTapCandidate = false
                                    }
                                }
                            }
                        }
                        true
                    }
            ) {
                // Minimal Icon Instructions Overlay (Hidden in compact mode for 100% clean surface)
                if (!isCompactMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(currentTheme.card.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Click", color = currentTheme.textPrimary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = currentTheme.secondaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scroll", color = currentTheme.textPrimary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragHandle, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Arrastrar", color = currentTheme.textPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Top-Left Aesthetic Integrated Menu Pill (Top Sibling - Receives Touch First!)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clickable {
                        onOpenDrawer()
                        onVibrate(15L)
                    },
                shape = RoundedCornerShape(20.dp),
                color = currentTheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, currentTheme.card)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = currentTheme.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ZEN-TRACK",
                        color = currentTheme.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BatteryBadge(currentTheme = currentTheme)
                }
            }
        }

        // -------------------------------------------------------------
        // RIGHT SIDEBAR / COMPACT SCROLL OVERLAY
        // -------------------------------------------------------------
        if (isCompactMode) {
            // Ultra-slim 18dp right edge scroll strip for compact hybrid mode
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight()
                    .background(currentTheme.card.copy(alpha = 0.5f))
            ) {
                ScrollWheelStrip(
                    naturalScroll = naturalScroll,
                    cardColor = currentTheme.card.copy(alpha = 0.8f),
                    iconColor = currentTheme.primaryAccent,
                    onSendJson = onSendJson,
                    onVibrate = onVibrate
                )
            }
        } else {
            // Full 120dp Sidebar for Solo Trackpad Mode
            Row(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .background(currentTheme.surface)
            ) {
                // Scroll Wheel Strip
                ScrollWheelStrip(
                    naturalScroll = naturalScroll,
                    cardColor = currentTheme.card,
                    iconColor = currentTheme.textMuted,
                    onSendJson = onSendJson,
                    onVibrate = onVibrate
                )

                // Stacked Physical Mouse Buttons (100% Isolated Compose pointerInput!)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Left Click Button
                    TokyoIconButton(
                        icon = Icons.Default.Mouse,
                        label = "L-CLICK",
                        buttonCode = 1,
                        activeColor = currentTheme.primaryAccent,
                        cardColor = currentTheme.card,
                        textColor = currentTheme.textPrimary,
                        modifier = Modifier.weight(1.5f),
                        onSendJson = onSendJson,
                        onVibrate = onVibrate
                    )

                    // Middle Click Button
                    TokyoIconButton(
                        icon = Icons.Default.Adjust,
                        label = "MID",
                        buttonCode = 2,
                        activeColor = currentTheme.secondaryAccent,
                        cardColor = currentTheme.card,
                        textColor = currentTheme.textPrimary,
                        modifier = Modifier.weight(1f),
                        onSendJson = onSendJson,
                        onVibrate = onVibrate
                    )

                    // Right Click Button
                    TokyoIconButton(
                        icon = Icons.Default.AdsClick,
                        label = "R-CLICK",
                        buttonCode = 3,
                        activeColor = currentTheme.primaryAccent,
                        cardColor = currentTheme.card,
                        textColor = currentTheme.textPrimary,
                        modifier = Modifier.weight(1.5f),
                        onSendJson = onSendJson,
                        onVibrate = onVibrate
                    )
                }
            }
        }
    }
}

