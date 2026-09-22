package com.carlos.zentrack.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.ui.components.*
import org.json.JSONObject
import java.util.Random
import kotlin.math.abs
import kotlin.math.hypot

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
    physicalButtonsEnabled: Boolean = com.carlos.zentrack.preferences.ZenPreferences.trackpadPhysicalButtonsEnabled,
    buttonsPosition: String = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsPosition,
    scrollPosition: String = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollPosition,
    scrollWidth: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollWidth,
    buttonsSidebarWidth: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsSidebarWidth,
    buttonsBottomHeight: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsBottomHeight,
    isCompactMode: Boolean = false,
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onOpenBluetoothDialog: () -> Unit = {},
    onOpenServerConnectionDialog: () -> Unit = {},
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Multi-touch Pointer Map for Trackpad Surface
    val trackpadPointers = remember { mutableMapOf<Int, TrackpadPointer>() }

    // Surface bounds tracking for strict pointer isolation
    var surfaceWidthPx by remember { mutableFloatStateOf(0f) }
    var surfaceHeightPx by remember { mutableFloatStateOf(0f) }

    // Active physical button hold counter (to isolate physical click-drag from 2-finger trackpad gestures)
    var activePhysicalButtonsCount by remember { mutableIntStateOf(0) }

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

    // Procedural Hardware-Accelerated Matte Paper / Hydrogel Flat Texture Shader
    val paperMattePaint = remember {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(4242)
        for (x in 0 until size) {
            for (y in 0 until size) {
                val noise = rng.nextFloat()
                val alpha = when {
                    noise > 0.94f -> (rng.nextFloat() * 16 + 10).toInt()
                    noise < 0.06f -> (rng.nextFloat() * 18 + 8).toInt()
                    else -> (rng.nextFloat() * 7).toInt()
                }
                val shade = if (noise > 0.5f) 255 else 0
                bitmap.setPixel(x, y, android.graphics.Color.argb(alpha, shade, shade, shade))
            }
        }
        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        Paint().apply {
            this.shader = shader
            isAntiAlias = true
            isFilterBitmap = true
        }
    }

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

    // MAIN TRACKPAD SURFACE (TRANSPARENT INTERACTIVE ZONE OVER CONTINUOUS FLAT MATTE SCREEN)
    @Composable
    fun MainTrackpadSurface(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .onSizeChanged {
                    surfaceWidthPx = it.width.toFloat()
                    surfaceHeightPx = it.height.toFloat()
                }
        ) {
            // Interactive Multi-Touch Surface Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { motionEvent ->
                        val action = motionEvent.actionMasked

                        // Exclude top-left menu pill touch zone
                        if (action == MotionEvent.ACTION_DOWN && motionEvent.x < 380f && motionEvent.y < 160f) {
                            return@pointerInteropFilter false
                        }

                        val actionIdx = motionEvent.actionIndex
                        val actionPointerId = motionEvent.getPointerId(actionIdx)

                        val touchX = motionEvent.getX(actionIdx)
                        val touchY = motionEvent.getY(actionIdx)
                        val isOriginInsideSurface = (touchX >= 0f && touchX <= surfaceWidthPx && touchY >= 0f && touchY <= surfaceHeightPx)

                        if (action == MotionEvent.ACTION_DOWN && !isOriginInsideSurface) {
                            return@pointerInteropFilter false
                        }

                        when (action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                                val x = touchX
                                val y = touchY

                                if (isOriginInsideSurface) {
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
                                            if (isTapCandidate && !hasHadTwoFingers && trackpadPointers.size == 1 && activePhysicalButtonsCount == 0) {
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
                                    } else if (trackpadPointers.size == 2 && activePhysicalButtonsCount == 0) {
                                        // 2-Finger Trackpad Gesture: only triggered if no physical button is being held down!
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
                                            if (isTwoFingerTapCandidate && trackpadPointers.size == 2 && activePhysicalButtonsCount == 0) {
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
                                    } else if (trackpadPointers.size == 3 && activePhysicalButtonsCount == 0) {
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

                                        if (com.carlos.zentrack.bluetooth.ZenInputRouter.activeMode == com.carlos.zentrack.bluetooth.ConnectionMode.BLUETOOTH && com.carlos.zentrack.bluetooth.ZenInputRouter.isBluetoothConnected) {
                                            // Pure continuous sub-pixel floating point feed (Zero staircase quantization)
                                            com.carlos.zentrack.bluetooth.ZenInputRouter.sendMouseMove(calcDx, calcDy, onSendBinary)
                                        } else {
                                            // Network Mode: 500Hz integer binary protocol
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

                                // Single finger movement or physical button drag (uninterrupted glide across button zone!)
                                if ((trackpadPointers.size == 1 && !hasHadTwoFingers) || activePhysicalButtonsCount > 0) {
                                    val pId = trackpadPointers.keys.firstOrNull()
                                    if (pId != null) {
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

                                            // Process current point
                                            val stepDx = currX - state.prevX
                                            val stepDy = currY - state.prevY
                                            state.prevX = currX
                                            state.prevY = currY
                                            processCursorDelta(stepDx, stepDy)
                                        }
                                    }
                                } else if (trackpadPointers.size == 2 && activePhysicalButtonsCount == 0) {
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
                                        processCursorDelta(frameDx, frameDy)
                                    } else {
                                        // 2-Finger Normal Scroll
                                        var dx = (currScrollX - lastScrollX) * sensitivity * scrollSensitivity
                                        var dy = (currScrollY - lastScrollY) * sensitivity * scrollSensitivity

                                        if (naturalScroll) {
                                            dx = -dx
                                            dy = -dy
                                        }

                                        if (com.carlos.zentrack.bluetooth.ZenInputRouter.activeMode == com.carlos.zentrack.bluetooth.ConnectionMode.BLUETOOTH && com.carlos.zentrack.bluetooth.ZenInputRouter.isBluetoothConnected) {
                                            com.carlos.zentrack.bluetooth.ZenInputRouter.sendMouseScroll(dy, dx, onSendBinary)
                                        } else {
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
                                    }

                                    lastScrollX = currScrollX
                                    lastScrollY = currScrollY
                                } else if (trackpadPointers.size == 3 && isThreeFingerSwipeCandidate && activePhysicalButtonsCount == 0) {
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

                                if (hasHadTwoFingers && activePhysicalButtonsCount == 0) {
                                    val duration = System.currentTimeMillis() - twoFingerTapStartTime
                                    if (isTwoFingerTapCandidate && duration < 300 && !isRightDraggingMode) {
                                        cancelClickTimer()
                                        val json = JSONObject().put("type", "click").put("button", 3)
                                        onSendJson(json.toString())
                                        onVibrate(25L)
                                        isTwoFingerTapCandidate = false
                                    }
                                } else if (activePhysicalButtonsCount == 0) {
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
                // Minimalist Instruction Pill
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
                            Text("1 Dedo: Click / Arrastre", color = currentTheme.textPrimary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = currentTheme.secondaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("2 Dedos: Scroll / R-Click / R-Drag", color = currentTheme.textPrimary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragHandle, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("3 Dedos: Escritorios", color = currentTheme.textPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Top-Left Aesthetic Integrated Menu Pill
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

            // Top-Right Aesthetic Integrated Connection Mode Pill
            val activeMode = com.carlos.zentrack.bluetooth.ZenInputRouter.activeMode
            val isBtMode = activeMode == com.carlos.zentrack.bluetooth.ConnectionMode.BLUETOOTH
            val isBtConnected = com.carlos.zentrack.bluetooth.ZenInputRouter.isBluetoothConnected
            val activeConnected = if (isBtMode) isBtConnected else isConnected
            val isUsbMode = com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clickable {
                        onVibrate(15L)
                        if (isBtMode) {
                            onOpenBluetoothDialog()
                        } else {
                            onOpenServerConnectionDialog()
                        }
                    },

                shape = RoundedCornerShape(20.dp),
                color = currentTheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, if (activeConnected) currentTheme.primaryAccent.copy(alpha = 0.45f) else currentTheme.card)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (activeConnected) Color(0xFF10B981) else Color(0xFFF7768E),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = when {
                            isBtMode -> Icons.Default.Bluetooth
                            isUsbMode && isConnected -> Icons.Default.Usb
                            else -> Icons.Default.Wifi
                        },
                        contentDescription = null,
                        tint = if (activeConnected) currentTheme.primaryAccent else currentTheme.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isBtMode -> {
                                if (isBtConnected) {
                                    (com.carlos.zentrack.bluetooth.ZenInputRouter.connectedBluetoothDeviceName ?: "BT HID")
                                } else {
                                    "BT Offline"
                                }
                            }
                            isUsbMode -> {
                                if (isConnected) "USB ADB (500Hz)" else if (statusText == "Conectando...") "USB Conectando..." else "USB Desconectado"
                            }
                            else -> {
                                if (isConnected) "Wi-Fi (500Hz)" else if (statusText == "Conectando...") "Wi-Fi Conectando..." else "Red Offline"
                            }
                        },
                        color = currentTheme.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = currentTheme.card,
                        modifier = Modifier.clickable {
                            onVibrate(15L)
                            com.carlos.zentrack.bluetooth.ZenInputRouter.toggleMode()
                        }
                    ) {
                        Text(
                            text = when {
                                isBtMode -> "BT"
                                isUsbMode -> "USB"
                                else -> "WIFI"
                            },
                            color = currentTheme.primaryAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Scroll Wheel Strip Box Component (Transparent Overlay)
    @Composable
    fun ScrollStripBox(isLeft: Boolean, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(Color.Transparent)
        ) {
            ScrollWheelStrip(
                naturalScroll = naturalScroll,
                cardColor = currentTheme.card,
                iconColor = currentTheme.primaryAccent,
                isLeftPosition = isLeft,
                onSendJson = onSendJson,
                onVibrate = onVibrate
            )
        }
    }

    // Sidebar Click Buttons Box Component (Transparent Floating Keycaps)
    @Composable
    fun SidebarButtonsBox(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(Color.Transparent)
        ) {
            TrackpadVerticalButtons(
                currentTheme = currentTheme,
                modifier = Modifier.fillMaxSize(),
                onButtonPressChanged = { isPressed ->
                    if (isPressed) {
                        activePhysicalButtonsCount++
                    } else {
                        activePhysicalButtonsCount = maxOf(0, activePhysicalButtonsCount - 1)
                    }
                },
                onSendJson = onSendJson,
                onVibrate = onVibrate
            )
        }
    }

    // -------------------------------------------------------------
    // FULL-SCREEN CONTINUOUS FLAT MATTE PAPER SURFACE + DYNAMIC LAYOUT
    // -------------------------------------------------------------
    val effectiveScrollWidth = scrollWidth.dp
    val effectiveSidebarWidth = buttonsSidebarWidth.dp
    val effectiveBottomHeight = buttonsBottomHeight.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
    ) {
        // FULL-SCREEN CONTINUOUS FLAT MATTE PAPER / HYDROGEL TEXTURE CANVAS
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Layer 1: Flat Solid Base Color
            drawRect(color = currentTheme.background)

            // Layer 2: Seamless GPU-Tiled Matte Grain Texture (Zero Vignette / Flat Texture)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(0f, 0f, w, h, paperMattePaint)
            }
        }

        // DYNAMIC SCREEN LAYOUT
        if (isCompactMode || !physicalButtonsEnabled) {
            // PURE SURFACE + RESIZABLE SCROLL STRIP (LEFT OR RIGHT)
            Row(modifier = Modifier.fillMaxSize()) {
                if (scrollPosition == "left") {
                    ScrollStripBox(isLeft = true, modifier = Modifier.width(effectiveScrollWidth))
                    MainTrackpadSurface(modifier = Modifier.weight(1f))
                } else {
                    MainTrackpadSurface(modifier = Modifier.weight(1f))
                    ScrollStripBox(isLeft = false, modifier = Modifier.width(effectiveScrollWidth))
                }
            }
        } else {
            when (buttonsPosition) {
                "bottom" -> {
                    // LAPTOP STYLE (SEAMLESS FLOATING BOTTOM CLICK BAR + LATERAL SCROLL)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (scrollPosition == "left") {
                                ScrollStripBox(isLeft = true, modifier = Modifier.width(effectiveScrollWidth))
                                MainTrackpadSurface(modifier = Modifier.weight(1f))
                            } else {
                                MainTrackpadSurface(modifier = Modifier.weight(1f))
                                ScrollStripBox(isLeft = false, modifier = Modifier.width(effectiveScrollWidth))
                            }
                        }
                        TrackpadBottomBar(
                            currentTheme = currentTheme,
                            height = effectiveBottomHeight,
                            onButtonPressChanged = { isPressed ->
                                if (isPressed) {
                                    activePhysicalButtonsCount++
                                } else {
                                    activePhysicalButtonsCount = maxOf(0, activePhysicalButtonsCount - 1)
                                }
                            },
                            onSendJson = onSendJson,
                            onVibrate = onVibrate
                        )
                    }
                }
                "left" -> {
                    // BUTTONS ON LEFT SIDEBAR (SEAMLESS FLOATING KEYCAPS)
                    Row(modifier = Modifier.fillMaxSize()) {
                        SidebarButtonsBox(modifier = Modifier.width(effectiveSidebarWidth))
                        if (scrollPosition == "left") {
                            ScrollStripBox(isLeft = true, modifier = Modifier.width(effectiveScrollWidth))
                            MainTrackpadSurface(modifier = Modifier.weight(1f))
                        } else {
                            MainTrackpadSurface(modifier = Modifier.weight(1f))
                            ScrollStripBox(isLeft = false, modifier = Modifier.width(effectiveScrollWidth))
                        }
                    }
                }
                else -> { // "right"
                    // BUTTONS ON RIGHT SIDEBAR (SEAMLESS FLOATING KEYCAPS)
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (scrollPosition == "left") {
                            ScrollStripBox(isLeft = true, modifier = Modifier.width(effectiveScrollWidth))
                            MainTrackpadSurface(modifier = Modifier.weight(1f))
                            SidebarButtonsBox(modifier = Modifier.width(effectiveSidebarWidth))
                        } else {
                            MainTrackpadSurface(modifier = Modifier.weight(1f))
                            ScrollStripBox(isLeft = false, modifier = Modifier.width(effectiveScrollWidth))
                            SidebarButtonsBox(modifier = Modifier.width(effectiveSidebarWidth))
                        }
                    }
                }
            }
        }
    }
}
