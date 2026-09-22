package com.carlos.zentrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.preferences.ZenPreferences
import com.carlos.zentrack.theme.ZenGamepadThemeConfig
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.theme.rememberAnimatedGamepadTheme
import com.carlos.zentrack.theme.toGamepadTheme
import com.carlos.zentrack.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

// LAYOUT PROFILE DATA MODEL
data class GamepadProfile(
    val id: String,
    val name: String,
    val abxyMode: String = "grouped", // "grouped" or "freeform"
    val selectX: Float = -60.362427f, val selectY: Float = 21.22223f, val selectScale: Float = 2.0f,
    val startX: Float = 59.637535f, val startY: Float = 21.22223f, val startScale: Float = 2.0f,
    val ltX: Float = 21.642065f, val ltY: Float = 54.963966f, val ltScale: Float = 2.0f,
    val lbX: Float = 140.76747f, val lbY: Float = 55.286266f, val lbScale: Float = 2.0f,
    val rbX: Float = 127.01098f, val rbY: Float = 62.627647f, val rbScale: Float = 2.0f,
    val rtX: Float = 8.1541815f, val rtY: Float = 62.592983f, val rtScale: Float = 2.0f,
    val abxyX: Float = 33.99899f, val abxyY: Float = 45.070637f, val abxyScale: Float = 1.3919744f, val abxyInnerScale: Float = 1.2156146f,
    val dpadX: Float = 230.04437f, val dpadY: Float = 40.47721f, val dpadScale: Float = 1.2847857f,
    val aX: Float = 32f, val aY: Float = 32f, val aScale: Float = 1.0f,
    val bX: Float = 78f, val bY: Float = 78f, val bScale: Float = 1.0f,
    val xX: Float = 14f, val xY: Float = 78f, val xScale: Float = 1.0f,
    val yX: Float = 32f, val yY: Float = 124f, val yScale: Float = 1.0f
)

@Composable
fun GamepadScreen(
    currentTheme: ZenThemeConfig,
    isConnected: Boolean,
    statusText: String,
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    // Isolated Gamepad Theme Engine & Animated Color State
    val rawGamepadTheme = remember(currentTheme) { currentTheme.toGamepadTheme() }
    val gamepadTheme = rememberAnimatedGamepadTheme(targetTheme = rawGamepadTheme)

    var gamepadMode by remember { mutableStateOf(ZenPreferences.gamepadMode) }
    var pcSensitivity by remember { mutableFloatStateOf(ZenPreferences.gamepadPcSensitivity) }
    var pcAccelEnabled by remember { mutableStateOf(ZenPreferences.gamepadPcAccelEnabled) }
    var xboxSensitivity by remember { mutableFloatStateOf(ZenPreferences.gamepadXboxSensitivity) }
    var xboxRightStickMode by remember { mutableStateOf(ZenPreferences.gamepadXboxRightStickMode) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isEditLayoutMode by remember { mutableStateOf(false) }

    // Synchronize mode changes with server
    LaunchedEffect(gamepadMode) {
        if (gamepadMode == "xbox") {
            onSendJson("""{"type":"gamepad_mode","enabled":true}""")
        } else {
            onSendJson("""{"type":"gamepad_mode","enabled":false}""")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onSendJson("""{"type":"gamepad_mode","enabled":false}""")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gamepadTheme.chasisBg)
    ) {
        // MAIN 50/50 SPLIT CONTAINER
        Row(modifier = Modifier.fillMaxSize()) {
            // =========================================================
            // LEFT 50%: MINIMALIST DYNAMIC JOYSTICK WITH PRESS-HOLD L3
            // =========================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                DynamicJoystickArea(
                    gamepadTheme = gamepadTheme,
                    gamepadMode = gamepadMode,
                    onSendJson = onSendJson,
                    onVibrate = onVibrate
                )
            }

            // Divider Line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(gamepadTheme.chasisBorder.copy(alpha = 0.25f))
            )

            // =========================================================
            // RIGHT 50%: AIM CAMERA TRACKPAD AREA WITH PRESS-HOLD R3
            // =========================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AimCameraTrackpadArea(
                    gamepadTheme = gamepadTheme,
                    gamepadMode = gamepadMode,
                    pcSensitivity = pcSensitivity,
                    pcAccelEnabled = pcAccelEnabled,
                    xboxSensitivity = xboxSensitivity,
                    xboxRightStickMode = xboxRightStickMode,
                    onSendBinary = onSendBinary,
                    onSendJson = onSendJson,
                    onVibrate = onVibrate
                )
            }
        }

        // =========================================================
        // XBOX GAMEPAD OVERLAY BUTTONS (PROFILES & FREEFORM ENGINE)
        // =========================================================
        if (gamepadMode == "xbox") {
            XboxGamepadOverlay(
                gamepadTheme = gamepadTheme,
                isEditMode = isEditLayoutMode,
                onSendJson = onSendJson,
                onVibrate = onVibrate
            )
        }

        // =========================================================
        // BALANCED TOP HEADER BAR (LEFT & RIGHT SYMMETRICAL DIVISION)
        // =========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE GROUP: Drawer Menu Button + Connection Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        onOpenDrawer()
                        onVibrate(15L)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menú",
                        tint = gamepadTheme.accentGlow
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    color = gamepadTheme.surfaceBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, gamepadTheme.chasisBorder.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onReconnect() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    color = if (isConnected) gamepadTheme.accentGlow else Color(0xFFF7768E),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            color = currentTheme.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // RIGHT SIDE GROUP: PC/XBOX Quick Switcher + Edit Layout Button + Gamepad Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = gamepadTheme.surfaceBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, gamepadTheme.accentGlow.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        val newMode = if (gamepadMode == "pc") "xbox" else "pc"
                        gamepadMode = newMode
                        ZenPreferences.gamepadMode = newMode
                        onVibrate(20L)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (gamepadMode == "pc") Icons.Default.Computer else Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = gamepadTheme.accentGlow,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (gamepadMode == "pc") "PC" else "XBOX",
                            color = gamepadTheme.accentGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (gamepadMode == "xbox") {
                    IconButton(
                        onClick = {
                            isEditLayoutMode = !isEditLayoutMode
                            onVibrate(15L)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            if (isEditLayoutMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Editar Layout Botones",
                            tint = if (isEditLayoutMode) Color(0xFF9ECE6A) else gamepadTheme.accentGlow
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = {
                        showSettingsDialog = true
                        onVibrate(15L)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Ajustes Gamepad",
                        tint = gamepadTheme.accentGlow
                    )
                }
            }
        }

        // Dedicated Gamepad Settings Dialog Modal
        GamepadSettingsDialog(
            show = showSettingsDialog,
            currentTheme = currentTheme,
            gamepadMode = gamepadMode,
            pcSensitivity = pcSensitivity,
            pcAccelEnabled = pcAccelEnabled,
            xboxSensitivity = xboxSensitivity,
            xboxRightStickMode = xboxRightStickMode,
            onGamepadModeChanged = { newMode ->
                gamepadMode = newMode
                ZenPreferences.gamepadMode = newMode
            },
            onPcSensitivityChanged = { newSens ->
                pcSensitivity = newSens
                ZenPreferences.gamepadPcSensitivity = newSens
            },
            onPcAccelEnabledChanged = { newAccel ->
                pcAccelEnabled = newAccel
                ZenPreferences.gamepadPcAccelEnabled = newAccel
            },
            onXboxSensitivityChanged = { newSens ->
                xboxSensitivity = newSens
                ZenPreferences.gamepadXboxSensitivity = newSens
            },
            onXboxRightStickModeChanged = { newStickMode ->
                xboxRightStickMode = newStickMode
                ZenPreferences.gamepadXboxRightStickMode = newStickMode
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// ====================================================================
// LEFT 50%: DYNAMIC MOVEMENT JOYSTICK WITH PRESS-HOLD L3
// ====================================================================
@Composable
fun DynamicJoystickArea(
    gamepadTheme: ZenGamepadThemeConfig,
    gamepadMode: String,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { 75.dp.toPx() }
    val strictMoveThresholdPx = with(density) { 8.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    var isTouchActive by remember { mutableStateOf(false) }
    var isL3Pressed by remember { mutableStateOf(false) }

    var touchAnchorX by remember { mutableFloatStateOf(0f) }
    var touchAnchorY by remember { mutableFloatStateOf(0f) }
    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }

    val activeWasdKeys = remember { mutableSetOf<String>() }

    fun updateWasd(newKeys: Set<String>) {
        val toRelease = activeWasdKeys - newKeys
        val toPress = newKeys - activeWasdKeys

        for (k in toRelease) {
            onSendJson("""{"type":"keyup","key":"$k"}""")
        }
        for (k in toPress) {
            onSendJson("""{"type":"keydown","key":"$k"}""")
        }

        activeWasdKeys.clear()
        activeWasdKeys.addAll(newKeys)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(gamepadMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y

                    isTouchActive = true
                    isL3Pressed = false
                    touchAnchorX = startX
                    touchAnchorY = startY
                    knobOffsetX = 0f
                    knobOffsetY = 0f

                    var hasMovedFar = false

                    val l3TimerJob = coroutineScope.launch {
                        delay(220L)
                        if (!hasMovedFar && isTouchActive) {
                            isL3Pressed = true
                            onVibrate(40L)
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = "L3")
                            onSendJson("""{"type":"gp_btn","name":"thumbl","value":1}""")
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id }
                        if (pointer != null) {
                            val dx = pointer.position.x - startX
                            val dy = pointer.position.y - startY
                            val distFromStart = sqrt(dx * dx + dy * dy)

                            if (distFromStart >= strictMoveThresholdPx && !isL3Pressed) {
                                hasMovedFar = true
                            }

                            if (pointer.pressed) {
                                val clampedDist = min(distFromStart, maxRadiusPx)
                                val angle = atan2(dy, dx)

                                knobOffsetX = clampedDist * cos(angle)
                                knobOffsetY = clampedDist * sin(angle)

                                if (gamepadMode == "pc") {
                                    if (clampedDist > 18f) {
                                        val deg = Math.toDegrees(angle.toDouble())
                                        val desiredWasd = mutableSetOf<String>()

                                        if (deg in -157.5..-112.5) {
                                            desiredWasd.add("w"); desiredWasd.add("a")
                                        } else if (deg in -112.5..-67.5) {
                                            desiredWasd.add("w")
                                        } else if (deg in -67.5..-22.5) {
                                            desiredWasd.add("w"); desiredWasd.add("d")
                                        } else if (deg in -22.5..22.5) {
                                            desiredWasd.add("d")
                                        } else if (deg in 22.5..67.5) {
                                            desiredWasd.add("s"); desiredWasd.add("d")
                                        } else if (deg in 67.5..112.5) {
                                            desiredWasd.add("s")
                                        } else if (deg in 112.5..157.5) {
                                            desiredWasd.add("s"); desiredWasd.add("a")
                                        } else {
                                            desiredWasd.add("a")
                                        }

                                        updateWasd(desiredWasd)
                                    } else {
                                        updateWasd(emptySet())
                                    }
                                } else {
                                    val ratioX = (knobOffsetX / maxRadiusPx).coerceIn(-1.0f, 1.0f)
                                    val ratioY = (knobOffsetY / maxRadiusPx).coerceIn(-1.0f, 1.0f)

                                    val signX = if (ratioX < 0) -1.0f else 1.0f
                                    val signY = if (ratioY < 0) -1.0f else 1.0f

                                    val boostedX = signX * (abs(ratioX).pow(0.65f)).coerceIn(0.0f, 1.0f)
                                    val boostedY = signY * (abs(ratioY).pow(0.65f)).coerceIn(0.0f, 1.0f)

                                    val normX = (boostedX * 127 + 128).toInt().coerceIn(0, 255)
                                    val normY = (boostedY * 127 + 128).toInt().coerceIn(0, 255)

                                    onSendJson("""{"type":"gp_axis","name":"X","value":$normX}""")
                                    onSendJson("""{"type":"gp_axis","name":"Y","value":$normY}""")
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    l3TimerJob.cancel()

                    if (isL3Pressed) {
                        onSendJson("""{"type":"gp_btn","name":"thumbl","value":0}""")
                        com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = "L3")
                        isL3Pressed = false
                    }

                    isTouchActive = false
                    knobOffsetX = 0f
                    knobOffsetY = 0f

                    if (gamepadMode == "pc") {
                        updateWasd(emptySet())
                    } else {
                        onSendJson("""{"type":"gp_axis","name":"X","value":128}""")
                        onSendJson("""{"type":"gp_axis","name":"Y","value":128}""")
                    }
                }
            }
    ) {
        if (isTouchActive) {
            val knobBg = if (isL3Pressed) gamepadTheme.accentGlow else gamepadTheme.bumperKeyBg
            val knobShadow = gamepadTheme.bumperKeyShadow
            val accentColor = gamepadTheme.accentGlow

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(touchAnchorX, touchAnchorY)
                val knobCenter = Offset(touchAnchorX + knobOffsetX, touchAnchorY + knobOffsetY)

                drawCircle(
                    color = gamepadTheme.surfaceBg.copy(alpha = 0.45f),
                    radius = maxRadiusPx,
                    center = center
                )
                drawCircle(
                    color = if (isL3Pressed) accentColor else accentColor.copy(alpha = 0.35f),
                    radius = maxRadiusPx,
                    center = center,
                    style = Stroke(width = if (isL3Pressed) 2.5.dp.toPx() else 1.5.dp.toPx())
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.65f),
                    start = center,
                    end = knobCenter,
                    strokeWidth = 1.8.dp.toPx()
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = 24.dp.toPx(),
                    center = knobCenter + Offset(0f, 2.5.dp.toPx())
                )
                drawCircle(
                    color = knobShadow,
                    radius = 24.dp.toPx(),
                    center = knobCenter
                )
                drawCircle(
                    color = knobBg,
                    radius = 22.dp.toPx(),
                    center = knobCenter - Offset(0f, 1.2.dp.toPx())
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.8f),
                    radius = 22.dp.toPx(),
                    center = knobCenter - Offset(0f, 1.2.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = if (isL3Pressed) Color.White else accentColor,
                    radius = 5.dp.toPx(),
                    center = knobCenter - Offset(0f, 1.2.dp.toPx())
                )
            }
        }
    }
}

// ====================================================================
// RIGHT 50%: AIM CAMERA TRACKPAD AREA WITH PRESS-HOLD R3
// ====================================================================
@Composable
fun AimCameraTrackpadArea(
    gamepadTheme: ZenGamepadThemeConfig,
    gamepadMode: String,
    pcSensitivity: Float,
    pcAccelEnabled: Boolean,
    xboxSensitivity: Float,
    xboxRightStickMode: String,
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { 75.dp.toPx() }
    val strictMoveThresholdPx = with(density) { 8.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    var subPixelRemainderX by remember { mutableFloatStateOf(0f) }
    var subPixelRemainderY by remember { mutableFloatStateOf(0f) }

    var isRightStickTouchActive by remember { mutableStateOf(false) }
    var isR3Pressed by remember { mutableStateOf(false) }
    var touchAnchorX by remember { mutableFloatStateOf(0f) }
    var touchAnchorY by remember { mutableFloatStateOf(0f) }
    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(gamepadMode, pcSensitivity, pcAccelEnabled, xboxSensitivity, xboxRightStickMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    var lastTouchX = startX
                    var lastTouchY = startY
                    var lastTouchTime = down.uptimeMillis

                    var hasMovedFar = false
                    isR3Pressed = false

                    if (gamepadMode == "xbox" && xboxRightStickMode == "native_stick") {
                        isRightStickTouchActive = true
                        touchAnchorX = startX
                        touchAnchorY = startY
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                    }

                    val r3TimerJob = coroutineScope.launch {
                        delay(220L)
                        if (!hasMovedFar) {
                            isR3Pressed = true
                            onVibrate(40L)
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = "R3")
                            onSendJson("""{"type":"gp_btn","name":"thumbr","value":1}""")
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id }
                        if (pointer != null) {
                            val currentX = pointer.position.x
                            val currentY = pointer.position.y
                            val distFromStart = sqrt((currentX - startX).pow(2) + (currentY - startY).pow(2))

                            if (distFromStart >= strictMoveThresholdPx && !isR3Pressed) {
                                hasMovedFar = true
                            }

                            if (pointer.pressed) {
                                val currentTime = pointer.uptimeMillis
                                val dx = currentX - lastTouchX
                                val dy = currentY - lastTouchY
                                val dt = max(1L, currentTime - lastTouchTime).toFloat()

                                lastTouchX = currentX
                                lastTouchY = currentY
                                lastTouchTime = currentTime

                                if (gamepadMode == "pc") {
                                    var gain = pcSensitivity
                                    if (pcAccelEnabled) {
                                        val distStep = sqrt(dx * dx + dy * dy)
                                        val velocity = distStep / dt
                                        if (velocity > 0.12f) {
                                            val accelFactor = (1.0f + 1.2f * (velocity - 0.12f).pow(1.35f)).coerceAtMost(3.0f)
                                            gain *= accelFactor
                                        }
                                    }

                                    val calcDx = dx * gain
                                    val calcDy = dy * gain

                                    subPixelRemainderX += calcDx
                                    subPixelRemainderY += calcDy

                                    val sendMx = subPixelRemainderX.toInt()
                                    val sendMy = subPixelRemainderY.toInt()

                                    if (sendMx != 0 || sendMy != 0) {
                                        subPixelRemainderX -= sendMx.toFloat()
                                        subPixelRemainderY -= sendMy.toFloat()
                                        onSendBinary(1, sendMx, sendMy)
                                    }
                                } else {
                                    if (xboxRightStickMode == "native_stick") {
                                        val totalDx = currentX - touchAnchorX
                                        val totalDy = currentY - touchAnchorY
                                        val dist = sqrt(totalDx * totalDx + totalDy * totalDy)
                                        val clampedDist = min(dist, maxRadiusPx)
                                        val angle = atan2(totalDy, totalDx)

                                        knobOffsetX = clampedDist * cos(angle)
                                        knobOffsetY = clampedDist * sin(angle)

                                        val normRX = ((knobOffsetX / maxRadiusPx) * 127 * xboxSensitivity + 128).toInt().coerceIn(0, 255)
                                        val normRY = ((knobOffsetY / maxRadiusPx) * 127 * xboxSensitivity + 128).toInt().coerceIn(0, 255)

                                        onSendJson("""{"type":"gp_axis","name":"RX","value":$normRX}""")
                                        onSendJson("""{"type":"gp_axis","name":"RY","value":$normRY}""")
                                    } else {
                                        val stickRx = (128 + (dx * 28.0f * xboxSensitivity).coerceIn(-127f, 127f)).toInt()
                                        val stickRy = (128 + (dy * 28.0f * xboxSensitivity).coerceIn(-127f, 127f)).toInt()
                                        onSendJson("""{"type":"gp_axis","name":"RX","value":$stickRx}""")
                                        onSendJson("""{"type":"gp_axis","name":"RY","value":$stickRy}""")
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    r3TimerJob.cancel()

                    if (isR3Pressed) {
                        onSendJson("""{"type":"gp_btn","name":"thumbr","value":0}""")
                        com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = "R3")
                        isR3Pressed = false
                    }

                    if (gamepadMode == "xbox") {
                        isRightStickTouchActive = false
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                        onSendJson("""{"type":"gp_axis","name":"RX","value":128}""")
                        onSendJson("""{"type":"gp_axis","name":"RY","value":128}""")
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 24.dp.toPx()
            val dotRadius = 1.2.dp.toPx()
            val dotColor = gamepadTheme.accentGlow.copy(alpha = 0.08f)

            var x = gridSpacing
            while (x < size.width) {
                var y = gridSpacing
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                    y += gridSpacing
                }
                x += gridSpacing
            }

            if (isRightStickTouchActive && xboxRightStickMode == "native_stick") {
                val knobBg = if (isR3Pressed) gamepadTheme.accentGlow else gamepadTheme.bumperKeyBg
                val knobShadow = gamepadTheme.bumperKeyShadow
                val accentColor = gamepadTheme.accentGlow
                val center = Offset(touchAnchorX, touchAnchorY)
                val knobCenter = Offset(touchAnchorX + knobOffsetX, touchAnchorY + knobOffsetY)

                drawCircle(
                    color = gamepadTheme.surfaceBg.copy(alpha = 0.45f),
                    radius = maxRadiusPx,
                    center = center
                )
                drawCircle(
                    color = if (isR3Pressed) accentColor else accentColor.copy(alpha = 0.35f),
                    radius = maxRadiusPx,
                    center = center,
                    style = Stroke(width = if (isR3Pressed) 2.5.dp.toPx() else 1.5.dp.toPx())
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.65f),
                    start = center,
                    end = knobCenter,
                    strokeWidth = 1.8.dp.toPx()
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = 24.dp.toPx(),
                    center = knobCenter + Offset(0f, 2.5.dp.toPx())
                )
                drawCircle(
                    color = knobShadow,
                    radius = 24.dp.toPx(),
                    center = knobCenter
                )
                drawCircle(
                    color = knobBg,
                    radius = 22.dp.toPx(),
                    center = knobCenter - Offset(0f, 1.2.dp.toPx())
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.8f),
                    radius = 22.dp.toPx(),
                    center = knobCenter - Offset(0f, 1.2.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = if (isR3Pressed) Color.White else accentColor,
                    radius = 5.dp.toPx(),
                    center = knobCenter - Offset(0f, 1.2.dp.toPx())
                )
            }
        }
    }
}

// ====================================================================
// FULL FREEFORM BUTTON, DUAL ABXY MODE & PROFILES ENGINE
// ====================================================================
@Composable
fun XboxGamepadOverlay(
    gamepadTheme: ZenGamepadThemeConfig,
    isEditMode: Boolean,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    // Multi-selection & grouping state
    val selectedIds = remember { mutableStateListOf<String>() }

    // ABXY Mode: "grouped" (chasis diamond) or "freeform" (independent A, B, X, Y buttons)
    var abxyMode by remember { mutableStateOf(ZenPreferences.xboxAbxyMode) }

    // Dialog state for Profile Creation
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var newProfileNameInput by remember { mutableStateOf("") }
    var showProfileDropdown by remember { mutableStateOf(false) }

    // Profiles List State
    val profilesList = remember { mutableStateListOf<GamepadProfile>() }
    var activeProfileId by remember { mutableStateOf(ZenPreferences.xboxActiveProfileId) }

    // Load initial profiles from ZenPreferences
    fun loadProfilesFromPrefs() {
        profilesList.clear()
        val jsonStr = ZenPreferences.xboxProfilesJson
        if (jsonStr.isNotBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    profilesList.add(
                        GamepadProfile(
                            id = obj.optString("id", "profile_$i"),
                            name = obj.optString("name", "Perfil ${i + 1}"),
                            abxyMode = obj.optString("abxyMode", "grouped"),
                            selectX = obj.optDouble("selectX", -60.362427).toFloat(),
                            selectY = obj.optDouble("selectY", 21.22223).toFloat(),
                            selectScale = obj.optDouble("selectScale", 2.0).toFloat(),
                            startX = obj.optDouble("startX", 59.637535).toFloat(),
                            startY = obj.optDouble("startY", 21.22223).toFloat(),
                            startScale = obj.optDouble("startScale", 2.0).toFloat(),
                            ltX = obj.optDouble("ltX", 21.642065).toFloat(),
                            ltY = obj.optDouble("ltY", 54.963966).toFloat(),
                            ltScale = obj.optDouble("ltScale", 2.0).toFloat(),
                            lbX = obj.optDouble("lbX", 140.76747).toFloat(),
                            lbY = obj.optDouble("lbY", 55.286266).toFloat(),
                            lbScale = obj.optDouble("lbScale", 2.0).toFloat(),
                            rbX = obj.optDouble("rbX", 127.01098).toFloat(),
                            rbY = obj.optDouble("rbY", 62.627647).toFloat(),
                            rbScale = obj.optDouble("rbScale", 2.0).toFloat(),
                            rtX = obj.optDouble("rtX", 8.1541815).toFloat(),
                            rtY = obj.optDouble("rtY", 62.592983).toFloat(),
                            rtScale = obj.optDouble("rtScale", 2.0).toFloat(),
                            abxyX = obj.optDouble("abxyX", 33.99899).toFloat(),
                            abxyY = obj.optDouble("abxyY", 45.070637).toFloat(),
                            abxyScale = obj.optDouble("abxyScale", 1.3919744).toFloat(),
                            abxyInnerScale = obj.optDouble("abxyInnerScale", 1.2156146).toFloat(),
                            dpadX = obj.optDouble("dpadX", 230.04437).toFloat(),
                            dpadY = obj.optDouble("dpadY", 40.47721).toFloat(),
                            dpadScale = obj.optDouble("dpadScale", 1.2847857).toFloat(),
                            aX = obj.optDouble("aX", 32.0).toFloat(),
                            aY = obj.optDouble("aY", 32.0).toFloat(),
                            aScale = obj.optDouble("aScale", 1.0).toFloat(),
                            bX = obj.optDouble("bX", 78.0).toFloat(),
                            bY = obj.optDouble("bY", 78.0).toFloat(),
                            bScale = obj.optDouble("bScale", 1.0).toFloat(),
                            xX = obj.optDouble("xX", 14.0).toFloat(),
                            xY = obj.optDouble("xY", 78.0).toFloat(),
                            xScale = obj.optDouble("xScale", 1.0).toFloat(),
                            yX = obj.optDouble("yX", 32.0).toFloat(),
                            yY = obj.optDouble("yY", 124.0).toFloat(),
                            yScale = obj.optDouble("yScale", 1.0).toFloat()
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        // Add built-in defaults if empty
        if (profilesList.isEmpty()) {
            profilesList.add(GamepadProfile("default", "Predeterminado", "grouped"))
            profilesList.add(GamepadProfile("minecraft", "Minecraft (Ergonómico)", "grouped", aY = 40f, bY = 90f))
            profilesList.add(GamepadProfile("roblox", "Roblox (Libre)", "freeform", aY = 20f, bX = 90f, xX = 20f))
        }
    }

    LaunchedEffect(Unit) {
        loadProfilesFromPrefs()
    }

    // Individual Button Offsets & Scales State
    var selectX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnSelectOffsetX) }
    var selectY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnSelectOffsetY) }
    var selectScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnSelectScale) }

    var startX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnStartOffsetX) }
    var startY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnStartOffsetY) }
    var startScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnStartScale) }

    var ltX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnLTOffsetX) }
    var ltY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnLTOffsetY) }
    var ltScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnLTScale) }

    var lbX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnLBOffsetX) }
    var lbY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnLBOffsetY) }
    var lbScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnLBScale) }

    var rbX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnRBOffsetX) }
    var rbY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnRBOffsetY) }
    var rbScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnRBScale) }

    var rtX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnRTOffsetX) }
    var rtY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnRTOffsetY) }
    var rtScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnRTScale) }

    var abxyX by remember { mutableFloatStateOf(ZenPreferences.xboxAbxyOffsetX) }
    var abxyY by remember { mutableFloatStateOf(ZenPreferences.xboxAbxyOffsetY) }
    var abxyScale by remember { mutableFloatStateOf(ZenPreferences.xboxAbxyScale) }
    var abxyInnerScale by remember { mutableFloatStateOf(ZenPreferences.xboxAbxyInnerScale) }

    var dpadX by remember { mutableFloatStateOf(ZenPreferences.xboxDpadOffsetX) }
    var dpadY by remember { mutableFloatStateOf(ZenPreferences.xboxDpadOffsetY) }
    var dpadScale by remember { mutableFloatStateOf(ZenPreferences.xboxDpadScale) }

    // Freeform Individual A, B, X, Y State
    var aX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnAOffsetX) }
    var aY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnAOffsetY) }
    var aScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnAScale) }

    var bX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnBOffsetX) }
    var bY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnBOffsetY) }
    var bScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnBScale) }

    var xX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnXOffsetX) }
    var xY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnXOffsetY) }
    var xScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnXScale) }

    var yX by remember { mutableFloatStateOf(ZenPreferences.xboxBtnYOffsetX) }
    var yY by remember { mutableFloatStateOf(ZenPreferences.xboxBtnYOffsetY) }
    var yScale by remember { mutableFloatStateOf(ZenPreferences.xboxBtnYScale) }

    fun saveProfilesToPrefs() {
        val array = JSONArray()
        for (p in profilesList) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("abxyMode", p.abxyMode)
            obj.put("selectX", p.selectX); obj.put("selectY", p.selectY); obj.put("selectScale", p.selectScale)
            obj.put("startX", p.startX); obj.put("startY", p.startY); obj.put("startScale", p.startScale)
            obj.put("ltX", p.ltX); obj.put("ltY", p.ltY); obj.put("ltScale", p.ltScale)
            obj.put("lbX", p.lbX); obj.put("lbY", p.lbY); obj.put("lbScale", p.lbScale)
            obj.put("rbX", p.rbX); obj.put("rbY", p.rbY); obj.put("rbScale", p.rbScale)
            obj.put("rtX", p.rtX); obj.put("rtY", p.rtY); obj.put("rtScale", p.rtScale)
            obj.put("abxyX", p.abxyX); obj.put("abxyY", p.abxyY); obj.put("abxyScale", p.abxyScale); obj.put("abxyInnerScale", p.abxyInnerScale)
            obj.put("dpadX", p.dpadX); obj.put("dpadY", p.dpadY); obj.put("dpadScale", p.dpadScale)
            obj.put("aX", p.aX); obj.put("aY", p.aY); obj.put("aScale", p.aScale)
            obj.put("bX", p.bX); obj.put("bY", p.bY); obj.put("bScale", p.bScale)
            obj.put("xX", p.xX); obj.put("xY", p.xY); obj.put("xScale", p.xScale)
            obj.put("yX", p.yX); obj.put("yY", p.yY); obj.put("yScale", p.yScale)
            array.put(obj)
        }
        ZenPreferences.xboxProfilesJson = array.toString()
        ZenPreferences.xboxActiveProfileId = activeProfileId
    }

    fun applyProfile(p: GamepadProfile) {
        activeProfileId = p.id
        abxyMode = p.abxyMode
        ZenPreferences.xboxAbxyMode = p.abxyMode

        selectX = p.selectX; selectY = p.selectY; selectScale = p.selectScale
        startX = p.startX; startY = p.startY; startScale = p.startScale
        ltX = p.ltX; ltY = p.ltY; ltScale = p.ltScale
        lbX = p.lbX; lbY = p.lbY; lbScale = p.lbScale
        rbX = p.rbX; rbY = p.rbY; rbScale = p.rbScale
        rtX = p.rtX; rtY = p.rtY; rtScale = p.rtScale
        abxyX = p.abxyX; abxyY = p.abxyY; abxyScale = p.abxyScale; abxyInnerScale = p.abxyInnerScale
        dpadX = p.dpadX; dpadY = p.dpadY; dpadScale = p.dpadScale
        aX = p.aX; aY = p.aY; aScale = p.aScale
        bX = p.bX; bY = p.bY; bScale = p.bScale
        xX = p.xX; xY = p.xY; xScale = p.xScale
        yX = p.yX; yY = p.yY; yScale = p.yScale

        ZenPreferences.xboxBtnSelectOffsetX = selectX; ZenPreferences.xboxBtnSelectOffsetY = selectY; ZenPreferences.xboxBtnSelectScale = selectScale
        ZenPreferences.xboxBtnStartOffsetX = startX; ZenPreferences.xboxBtnStartOffsetY = startY; ZenPreferences.xboxBtnStartScale = startScale
        ZenPreferences.xboxBtnLTOffsetX = ltX; ZenPreferences.xboxBtnLTOffsetY = ltY; ZenPreferences.xboxBtnLTScale = ltScale
        ZenPreferences.xboxBtnLBOffsetX = lbX; ZenPreferences.xboxBtnLBOffsetY = lbY; ZenPreferences.xboxBtnLBScale = lbScale
        ZenPreferences.xboxBtnRBOffsetX = rbX; ZenPreferences.xboxBtnRBOffsetY = rbY; ZenPreferences.xboxBtnRBScale = rbScale
        ZenPreferences.xboxBtnRTOffsetX = rtX; ZenPreferences.xboxBtnRTOffsetY = rtY; ZenPreferences.xboxBtnRTScale = rtScale
        ZenPreferences.xboxAbxyOffsetX = abxyX; ZenPreferences.xboxAbxyOffsetY = abxyY; ZenPreferences.xboxAbxyScale = abxyScale; ZenPreferences.xboxAbxyInnerScale = abxyInnerScale
        ZenPreferences.xboxDpadOffsetX = dpadX; ZenPreferences.xboxDpadOffsetY = dpadY; ZenPreferences.xboxDpadScale = dpadScale
        ZenPreferences.xboxBtnAOffsetX = aX; ZenPreferences.xboxBtnAOffsetY = aY; ZenPreferences.xboxBtnAScale = aScale
        ZenPreferences.xboxBtnBOffsetX = bX; ZenPreferences.xboxBtnBOffsetY = bY; ZenPreferences.xboxBtnBScale = bScale
        ZenPreferences.xboxBtnXOffsetX = xX; ZenPreferences.xboxBtnXOffsetY = xY; ZenPreferences.xboxBtnXScale = xScale
        ZenPreferences.xboxBtnYOffsetX = yX; ZenPreferences.xboxBtnYOffsetY = yY; ZenPreferences.xboxBtnYScale = yScale

        ZenPreferences.xboxActiveProfileId = activeProfileId
    }

    fun saveCurrentToActiveProfile() {
        val currentProfile = GamepadProfile(
            id = activeProfileId,
            name = profilesList.firstOrNull { it.id == activeProfileId }?.name ?: "Perfil Personalizado",
            abxyMode = abxyMode,
            selectX = selectX, selectY = selectY, selectScale = selectScale,
            startX = startX, startY = startY, startScale = startScale,
            ltX = ltX, ltY = ltY, ltScale = ltScale,
            lbX = lbX, lbY = lbY, lbScale = lbScale,
            rbX = rbX, rbY = rbY, rbScale = rbScale,
            rtX = rtX, rtY = rtY, rtScale = rtScale,
            abxyX = abxyX, abxyY = abxyY, abxyScale = abxyScale, abxyInnerScale = abxyInnerScale,
            dpadX = dpadX, dpadY = dpadY, dpadScale = dpadScale,
            aX = aX, aY = aY, aScale = aScale,
            bX = bX, bY = bY, bScale = bScale,
            xX = xX, xY = xY, xScale = xScale,
            yX = yX, yY = yY, yScale = yScale
        )
        val idx = profilesList.indexOfFirst { it.id == activeProfileId }
        if (idx >= 0) {
            profilesList[idx] = currentProfile
        } else {
            profilesList.add(currentProfile)
        }
        saveProfilesToPrefs()
    }

    // Helper: Move all items linked to targetId
    fun moveItemAndLinked(targetId: String, dx: Float, dy: Float) {
        val linkedSet = mutableSetOf(targetId)
        if (selectedIds.contains(targetId)) {
            linkedSet.addAll(selectedIds)
        }

        for (id in linkedSet) {
            when (id) {
                "SELECT" -> { selectX += dx; selectY += dy; ZenPreferences.xboxBtnSelectOffsetX = selectX; ZenPreferences.xboxBtnSelectOffsetY = selectY }
                "START" -> { startX += dx; startY += dy; ZenPreferences.xboxBtnStartOffsetX = startX; ZenPreferences.xboxBtnStartOffsetY = startY }
                "LT" -> { ltX += dx; ltY += dy; ZenPreferences.xboxBtnLTOffsetX = ltX; ZenPreferences.xboxBtnLTOffsetY = ltY }
                "LB" -> { lbX += dx; lbY += dy; ZenPreferences.xboxBtnLBOffsetX = lbX; ZenPreferences.xboxBtnLBOffsetY = lbY }
                "RB" -> { rbX -= dx; rbY += dy; ZenPreferences.xboxBtnRBOffsetX = rbX; ZenPreferences.xboxBtnRBOffsetY = rbY }
                "RT" -> { rtX -= dx; rtY += dy; ZenPreferences.xboxBtnRTOffsetX = rtX; ZenPreferences.xboxBtnRTOffsetY = rtY }
                "ABXY" -> { abxyX -= dx; abxyY -= dy; ZenPreferences.xboxAbxyOffsetX = abxyX; ZenPreferences.xboxAbxyOffsetY = abxyY }
                "DPAD" -> { dpadX += dx; dpadY -= dy; ZenPreferences.xboxDpadOffsetX = dpadX; ZenPreferences.xboxDpadOffsetY = dpadY }
                "A" -> { aX -= dx; aY -= dy; ZenPreferences.xboxBtnAOffsetX = aX; ZenPreferences.xboxBtnAOffsetY = aY }
                "B" -> { bX -= dx; bY -= dy; ZenPreferences.xboxBtnBOffsetX = bX; ZenPreferences.xboxBtnBOffsetY = bY }
                "X" -> { xX -= dx; xY -= dy; ZenPreferences.xboxBtnXOffsetX = xX; ZenPreferences.xboxBtnXOffsetY = xY }
                "Y" -> { yX -= dx; yY -= dy; ZenPreferences.xboxBtnYOffsetX = yX; ZenPreferences.xboxBtnYOffsetY = yY }
            }
        }
    }

    // Helper: Resize all items linked to targetId
    fun resizeItemAndLinked(targetId: String, delta: Float) {
        val linkedSet = mutableSetOf(targetId)
        if (selectedIds.contains(targetId)) {
            linkedSet.addAll(selectedIds)
        }

        for (id in linkedSet) {
            when (id) {
                "SELECT" -> { selectScale = (selectScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnSelectScale = selectScale }
                "START" -> { startScale = (startScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnStartScale = startScale }
                "LT" -> { ltScale = (ltScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnLTScale = ltScale }
                "LB" -> { lbScale = (lbScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnLBScale = lbScale }
                "RB" -> { rbScale = (rbScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnRBScale = rbScale }
                "RT" -> { rtScale = (rtScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnRTScale = rtScale }
                "ABXY" -> { abxyScale = (abxyScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxAbxyScale = abxyScale }
                "DPAD" -> { dpadScale = (dpadScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxDpadScale = dpadScale }
                "A" -> { aScale = (aScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnAScale = aScale }
                "B" -> { bScale = (bScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnBScale = bScale }
                "X" -> { xScale = (xScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnXScale = xScale }
                "Y" -> { yScale = (yScale + delta).coerceIn(0.4f, 3.5f); ZenPreferences.xboxBtnYScale = yScale }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // =========================================================
        // TOP FLOATING PROFILES & TOOLBAR MINI WINDOW IN EDIT MODE
        // =========================================================
        if (isEditMode) {
            Surface(
                color = gamepadTheme.surfaceBg.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, gamepadTheme.accentGlow),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Profile Quick Selector Dropdown Pill
                    Box {
                        Surface(
                            color = gamepadTheme.pillKeyBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { showProfileDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = gamepadTheme.pillKeyText, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = profilesList.firstOrNull { it.id == activeProfileId }?.name ?: "Perfil",
                                    color = gamepadTheme.pillKeyText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = gamepadTheme.pillKeyText, modifier = Modifier.size(14.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showProfileDropdown,
                            onDismissRequest = { showProfileDropdown = false }
                        ) {
                            profilesList.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name, fontSize = 11.sp, fontWeight = if (p.id == activeProfileId) FontWeight.Black else FontWeight.Normal) },
                                    onClick = {
                                        applyProfile(p)
                                        showProfileDropdown = false
                                        onVibrate(15L)
                                    }
                                )
                            }
                        }
                    }

                    // Save Profile Button 💾
                    IconButton(
                        onClick = {
                            saveCurrentToActiveProfile()
                            onVibrate(20L)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar Perfil", tint = gamepadTheme.accentGlow, modifier = Modifier.size(16.dp))
                    }

                    // New Profile Button ➕
                    IconButton(
                        onClick = {
                            newProfileNameInput = ""
                            showCreateProfileDialog = true
                            onVibrate(15L)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo Perfil", tint = gamepadTheme.accentGlow, modifier = Modifier.size(16.dp))
                    }

                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(gamepadTheme.chasisBorder.copy(alpha = 0.4f)))

                    // ABXY Mode Toggle Button (Chasis 💎 vs Freeform 🕹️)
                    Surface(
                        color = if (abxyMode == "freeform") gamepadTheme.accentGlow else gamepadTheme.surfaceBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, gamepadTheme.accentGlow),
                        modifier = Modifier.clickable {
                            val newMode = if (abxyMode == "grouped") "freeform" else "grouped"
                            abxyMode = newMode
                            ZenPreferences.xboxAbxyMode = newMode
                            onVibrate(20L)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (abxyMode == "grouped") Icons.Default.Hexagon else Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = if (abxyMode == "freeform") gamepadTheme.chasisBg else gamepadTheme.accentGlow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (abxyMode == "grouped") "💎 Chasis Agrupado" else "🕹️ ABXY Libres",
                                color = if (abxyMode == "freeform") gamepadTheme.chasisBg else gamepadTheme.accentGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================
        // INDIVIDUAL TOP LEFT BUTTONS: LT & LB
        // =========================================================
        FreeformItemBox(
            id = "LT",
            align = Alignment.TopStart,
            offsetX = ltX,
            offsetY = ltY,
            scale = ltScale,
            isEditMode = isEditMode,
            isSelected = selectedIds.contains("LT"),
            gamepadTheme = gamepadTheme,
            onSelect = {
                if (selectedIds.contains("LT")) selectedIds.remove("LT") else selectedIds.add("LT")
                onVibrate(10L)
            },
            onDrag = { dx, dy -> moveItemAndLinked("LT", dx, dy) },
            onResize = { delta -> resizeItemAndLinked("LT", delta) }
        ) {
            GamepadRectangularKeyCap(
                label = "LT",
                scale = ltScale,
                gamepadTheme = gamepadTheme,
                isTrigger = true,
                isEditMode = isEditMode,
                onDown = { onSendJson("""{"type":"gp_axis","name":"Z","value":255}"""); onVibrate(15L) },
                onUp = { onSendJson("""{"type":"gp_axis","name":"Z","value":0}""") }
            )
        }

        FreeformItemBox(
            id = "LB",
            align = Alignment.TopStart,
            offsetX = lbX,
            offsetY = lbY,
            scale = lbScale,
            isEditMode = isEditMode,
            isSelected = selectedIds.contains("LB"),
            gamepadTheme = gamepadTheme,
            onSelect = {
                if (selectedIds.contains("LB")) selectedIds.remove("LB") else selectedIds.add("LB")
                onVibrate(10L)
            },
            onDrag = { dx, dy -> moveItemAndLinked("LB", dx, dy) },
            onResize = { delta -> resizeItemAndLinked("LB", delta) }
        ) {
            GamepadRectangularKeyCap(
                label = "LB",
                scale = lbScale,
                gamepadTheme = gamepadTheme,
                isEditMode = isEditMode,
                onDown = { onSendJson("""{"type":"gp_btn","name":"L","value":1}"""); onVibrate(15L) },
                onUp = { onSendJson("""{"type":"gp_btn","name":"L","value":0}""") }
            )
        }

        // =========================================================
        // INDIVIDUAL TOP CENTER MENU BUTTONS: SELECT & START
        // =========================================================
        FreeformItemBox(
            id = "SELECT",
            align = Alignment.TopCenter,
            offsetX = selectX,
            offsetY = selectY,
            scale = selectScale,
            isEditMode = isEditMode,
            isSelected = selectedIds.contains("SELECT"),
            gamepadTheme = gamepadTheme,
            onSelect = {
                if (selectedIds.contains("SELECT")) selectedIds.remove("SELECT") else selectedIds.add("SELECT")
                onVibrate(10L)
            },
            onDrag = { dx, dy -> moveItemAndLinked("SELECT", dx, dy) },
            onResize = { delta -> resizeItemAndLinked("SELECT", delta) }
        ) {
            GamepadPillKeyCap(
                label = "SELECT",
                icon = Icons.Default.Tune,
                gamepadTheme = gamepadTheme,
                scale = selectScale,
                isEditMode = isEditMode,
                onDown = { onSendJson("""{"type":"gp_btn","name":"select","value":1}"""); onVibrate(15L) },
                onUp = { onSendJson("""{"type":"gp_btn","name":"select","value":0}""") }
            )
        }

        FreeformItemBox(
            id = "START",
            align = Alignment.TopCenter,
            offsetX = startX,
            offsetY = startY,
            scale = startScale,
            isEditMode = isEditMode,
            isSelected = selectedIds.contains("START"),
            gamepadTheme = gamepadTheme,
            onSelect = {
                if (selectedIds.contains("START")) selectedIds.remove("START") else selectedIds.add("START")
                onVibrate(10L)
            },
            onDrag = { dx, dy -> moveItemAndLinked("START", dx, dy) },
            onResize = { delta -> resizeItemAndLinked("START", delta) }
        ) {
            GamepadPillKeyCap(
                label = "START",
                icon = Icons.Default.PlayArrow,
                gamepadTheme = gamepadTheme,
                scale = startScale,
                isEditMode = isEditMode,
                onDown = { onSendJson("""{"type":"gp_btn","name":"start","value":1}"""); onVibrate(15L) },
                onUp = { onSendJson("""{"type":"gp_btn","name":"start","value":0}""") }
            )
        }

        // =========================================================
        // INDIVIDUAL TOP RIGHT BUTTONS: RB & RT
        // =========================================================
        FreeformItemBox(
            id = "RB",
            align = Alignment.TopEnd,
            offsetX = -rbX,
            offsetY = rbY,
            scale = rbScale,
            isEditMode = isEditMode,
            isSelected = selectedIds.contains("RB"),
            gamepadTheme = gamepadTheme,
            onSelect = {
                if (selectedIds.contains("RB")) selectedIds.remove("RB") else selectedIds.add("RB")
                onVibrate(10L)
            },
            onDrag = { dx, dy -> moveItemAndLinked("RB", dx, dy) },
            onResize = { delta -> resizeItemAndLinked("RB", delta) }
        ) {
            GamepadRectangularKeyCap(
                label = "RB",
                scale = rbScale,
                gamepadTheme = gamepadTheme,
                isEditMode = isEditMode,
                onDown = { onSendJson("""{"type":"gp_btn","name":"R","value":1}"""); onVibrate(15L) },
                onUp = { onSendJson("""{"type":"gp_btn","name":"R","value":0}""") }
            )
        }

        FreeformItemBox(
            id = "RT",
            align = Alignment.TopEnd,
            offsetX = -rtX,
            offsetY = rtY,
            scale = rtScale,
            isEditMode = isEditMode,
            isSelected = selectedIds.contains("RT"),
            gamepadTheme = gamepadTheme,
            onSelect = {
                if (selectedIds.contains("RT")) selectedIds.remove("RT") else selectedIds.add("RT")
                onVibrate(10L)
            },
            onDrag = { dx, dy -> moveItemAndLinked("RT", dx, dy) },
            onResize = { delta -> resizeItemAndLinked("RT", delta) }
        ) {
            GamepadRectangularKeyCap(
                label = "RT",
                scale = rtScale,
                gamepadTheme = gamepadTheme,
                isTrigger = true,
                isEditMode = isEditMode,
                onDown = { onSendJson("""{"type":"gp_axis","name":"RZ","value":255}"""); onVibrate(15L) },
                onUp = { onSendJson("""{"type":"gp_axis","name":"RZ","value":0}""") }
            )
        }

        // =========================================================
        // ABXY DUAL MODE: GROUPED CHASIS VS FREEFORM INDIVIDUAL BUTTONS
        // =========================================================
        if (abxyMode == "grouped") {
            FreeformItemBox(
                id = "ABXY",
                align = Alignment.BottomEnd,
                offsetX = -abxyX,
                offsetY = -abxyY,
                scale = abxyScale,
                isEditMode = isEditMode,
                isSelected = selectedIds.contains("ABXY"),
                gamepadTheme = gamepadTheme,
                onSelect = {
                    if (selectedIds.contains("ABXY")) selectedIds.remove("ABXY") else selectedIds.add("ABXY")
                    onVibrate(10L)
                },
                onDrag = { dx, dy -> moveItemAndLinked("ABXY", dx, dy) },
                onResize = { delta -> resizeItemAndLinked("ABXY", delta) }
            ) {
                XboxAbxyCluster(
                    gamepadTheme = gamepadTheme,
                    scale = abxyScale,
                    innerScale = abxyInnerScale,
                    isEditMode = isEditMode,
                    onSendJson = onSendJson,
                    onVibrate = onVibrate
                )
            }
        } else {
            // MODE ALTERNATIVO: BOTONES A, B, X, Y COMPLETAMENTE INDIVIDUALES Y LIBRES
            FreeformItemBox(
                id = "A", align = Alignment.BottomEnd, offsetX = -aX, offsetY = -aY, scale = aScale,
                isEditMode = isEditMode, isSelected = selectedIds.contains("A"), gamepadTheme = gamepadTheme,
                onSelect = { if (selectedIds.contains("A")) selectedIds.remove("A") else selectedIds.add("A"); onVibrate(10L) },
                onDrag = { dx, dy -> moveItemAndLinked("A", dx, dy) }, onResize = { delta -> resizeItemAndLinked("A", delta) }
            ) {
                GamepadCircularKeyCap(
                    label = "A", scale = aScale, gamepadTheme = gamepadTheme, isEditMode = isEditMode,
                    modifier = Modifier.size(44.dp * aScale),
                    onDown = { onSendJson("""{"type":"gp_btn","name":"A","value":1}"""); onVibrate(15L) },
                    onUp = { onSendJson("""{"type":"gp_btn","name":"A","value":0}""") }
                )
            }

            FreeformItemBox(
                id = "B", align = Alignment.BottomEnd, offsetX = -bX, offsetY = -bY, scale = bScale,
                isEditMode = isEditMode, isSelected = selectedIds.contains("B"), gamepadTheme = gamepadTheme,
                onSelect = { if (selectedIds.contains("B")) selectedIds.remove("B") else selectedIds.add("B"); onVibrate(10L) },
                onDrag = { dx, dy -> moveItemAndLinked("B", dx, dy) }, onResize = { delta -> resizeItemAndLinked("B", delta) }
            ) {
                GamepadCircularKeyCap(
                    label = "B", scale = bScale, gamepadTheme = gamepadTheme, isEditMode = isEditMode,
                    modifier = Modifier.size(44.dp * bScale),
                    onDown = { onSendJson("""{"type":"gp_btn","name":"B","value":1}"""); onVibrate(15L) },
                    onUp = { onSendJson("""{"type":"gp_btn","name":"B","value":0}""") }
                )
            }

            FreeformItemBox(
                id = "X", align = Alignment.BottomEnd, offsetX = -xX, offsetY = -xY, scale = xScale,
                isEditMode = isEditMode, isSelected = selectedIds.contains("X"), gamepadTheme = gamepadTheme,
                onSelect = { if (selectedIds.contains("X")) selectedIds.remove("X") else selectedIds.add("X"); onVibrate(10L) },
                onDrag = { dx, dy -> moveItemAndLinked("X", dx, dy) }, onResize = { delta -> resizeItemAndLinked("X", delta) }
            ) {
                GamepadCircularKeyCap(
                    label = "X", scale = xScale, gamepadTheme = gamepadTheme, isEditMode = isEditMode,
                    modifier = Modifier.size(44.dp * xScale),
                    onDown = { onSendJson("""{"type":"gp_btn","name":"X","value":1}"""); onVibrate(15L) },
                    onUp = { onSendJson("""{"type":"gp_btn","name":"X","value":0}""") }
                )
            }

            FreeformItemBox(
                id = "Y", align = Alignment.BottomEnd, offsetX = -yX, offsetY = -yY, scale = yScale,
                isEditMode = isEditMode, isSelected = selectedIds.contains("Y"), gamepadTheme = gamepadTheme,
                onSelect = { if (selectedIds.contains("Y")) selectedIds.remove("Y") else selectedIds.add("Y"); onVibrate(10L) },
                onDrag = { dx, dy -> moveItemAndLinked("Y", dx, dy) }, onResize = { delta -> resizeItemAndLinked("Y", delta) }
            ) {
                GamepadCircularKeyCap(
                    label = "Y", scale = yScale, gamepadTheme = gamepadTheme, isEditMode = isEditMode,
                    modifier = Modifier.size(44.dp * yScale),
                    onDown = { onSendJson("""{"type":"gp_btn","name":"Y","value":1}"""); onVibrate(15L) },
                    onUp = { onSendJson("""{"type":"gp_btn","name":"Y","value":0}""") }
                )
            }
        }

        // =========================================================
        // MECHANICAL D-PAD (8-WAY DIAGONAL ENGINE)
        // =========================================================
        FreeformItemBox(
            id = "DPAD",
            align = Alignment.BottomStart,
            offsetX = dpadX,
            offsetY = -dpadY,
            scale = dpadScale,
            isEditMode = isEditMode,
            isSelected = selectedIds.contains("DPAD"),
            gamepadTheme = gamepadTheme,
            onSelect = {
                if (selectedIds.contains("DPAD")) selectedIds.remove("DPAD") else selectedIds.add("DPAD")
                onVibrate(10L)
            },
            onDrag = { dx, dy -> moveItemAndLinked("DPAD", dx, dy) },
            onResize = { delta -> resizeItemAndLinked("DPAD", delta) }
        ) {
            XboxDpadDisc(
                gamepadTheme = gamepadTheme,
                scale = dpadScale,
                isEditMode = isEditMode,
                onSendJson = onSendJson,
                onVibrate = onVibrate
            )
        }

        // =========================================================
        // ADVANCED EDIT TOOLBAR (FREEFORM & CUSTOM GROUPING ENGINE)
        // =========================================================
        if (isEditMode) {
            Surface(
                color = gamepadTheme.surfaceBg.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(1.dp, gamepadTheme.accentGlow),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.95f)
                    .padding(bottom = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedIds.isEmpty()) "✏️ TOCA CUALQUIER BOTÓN PARA MOVERLO/ESCALARLO"
                            else "✏️ SELECCIONADOS (${selectedIds.size}): ${selectedIds.joinToString(", ")}",
                            color = gamepadTheme.accentGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (selectedIds.size > 1) {
                                Button(
                                    onClick = {
                                        selectedIds.clear()
                                        onVibrate(15L)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = gamepadTheme.pillKeyBg),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("🔗 Desagrupar", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = gamepadTheme.pillKeyText)
                                }
                            }

                            TextButton(
                                onClick = {
                                    selectedIds.clear()
                                    selectX = -40f; selectY = 42f; selectScale = 1.0f
                                    startX = 40f; startY = 42f; startScale = 1.0f
                                    ltX = 14f; ltY = 42f; ltScale = 1.0f
                                    lbX = 70f; lbY = 42f; lbScale = 1.0f
                                    rbX = 70f; rbY = 42f; rbScale = 1.0f
                                    rtX = 14f; rtY = 42f; rtScale = 1.0f
                                    abxyX = 32f; abxyY = 32f; abxyScale = 1.0f; abxyInnerScale = 1.05f
                                    dpadX = 32f; dpadY = 32f; dpadScale = 1.0f
                                    aX = 32f; aY = 32f; aScale = 1.0f
                                    bX = 78f; bY = 78f; bScale = 1.0f
                                    xX = 14f; xY = 78f; xScale = 1.0f
                                    yX = 32f; yY = 124f; yScale = 1.0f

                                    ZenPreferences.xboxBtnSelectOffsetX = selectX; ZenPreferences.xboxBtnSelectOffsetY = selectY; ZenPreferences.xboxBtnSelectScale = selectScale
                                    ZenPreferences.xboxBtnStartOffsetX = startX; ZenPreferences.xboxBtnStartOffsetY = startY; ZenPreferences.xboxBtnStartScale = startScale
                                    ZenPreferences.xboxBtnLTOffsetX = ltX; ZenPreferences.xboxBtnLTOffsetY = ltY; ZenPreferences.xboxBtnLTScale = ltScale
                                    ZenPreferences.xboxBtnLBOffsetX = lbX; ZenPreferences.xboxBtnLBOffsetY = lbY; ZenPreferences.xboxBtnLBScale = lbScale
                                    ZenPreferences.xboxBtnRBOffsetX = rbX; ZenPreferences.xboxBtnRBOffsetY = rbY; ZenPreferences.xboxBtnRBScale = rbScale
                                    ZenPreferences.xboxBtnRTOffsetX = rtX; ZenPreferences.xboxBtnRTOffsetY = rtY; ZenPreferences.xboxBtnRTScale = rtScale
                                    ZenPreferences.xboxAbxyOffsetX = abxyX; ZenPreferences.xboxAbxyOffsetY = abxyY; ZenPreferences.xboxAbxyScale = abxyScale; ZenPreferences.xboxAbxyInnerScale = abxyInnerScale
                                    ZenPreferences.xboxDpadOffsetX = dpadX; ZenPreferences.xboxDpadOffsetY = dpadY; ZenPreferences.xboxDpadScale = dpadScale
                                    ZenPreferences.xboxBtnAOffsetX = aX; ZenPreferences.xboxBtnAOffsetY = aY; ZenPreferences.xboxBtnAScale = aScale
                                    ZenPreferences.xboxBtnBOffsetX = bX; ZenPreferences.xboxBtnBOffsetY = bY; ZenPreferences.xboxBtnBScale = bScale
                                    ZenPreferences.xboxBtnXOffsetX = xX; ZenPreferences.xboxBtnXOffsetY = xY; ZenPreferences.xboxBtnXScale = xScale
                                    ZenPreferences.xboxBtnYOffsetX = yX; ZenPreferences.xboxBtnYOffsetY = yY; ZenPreferences.xboxBtnYScale = yScale
                                    onVibrate(20L)
                                }
                            ) {
                                Text("Restablecer Todo", color = Color(0xFFF7768E), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selectedIds.contains("ABXY") && abxyMode == "grouped") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tamaño Teclas ABXY Internas: ${(abxyInnerScale * 100).toInt()}%",
                                color = gamepadTheme.accentGlow,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Slider(
                                value = abxyInnerScale,
                                onValueChange = {
                                    abxyInnerScale = it
                                    ZenPreferences.xboxAbxyInnerScale = it
                                },
                                valueRange = 0.7f..1.5f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================
        // DIALOG: CREATE NEW LAYOUT PROFILE MODAL
        // =========================================================
        if (showCreateProfileDialog) {
            AlertDialog(
                onDismissRequest = { showCreateProfileDialog = false },
                title = { Text("📁 Crear Nuevo Perfil de Mando", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column {
                        Text("Ingresa un nombre para guardar la disposición y tamaños actuales:", fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newProfileNameInput,
                            onValueChange = { newProfileNameInput = it },
                            placeholder = { Text("Ej: Minecraft Pvp, Roblox CoD...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newProfileNameInput.isNotBlank()) {
                                val newId = "profile_${System.currentTimeMillis()}"
                                val newP = GamepadProfile(
                                    id = newId,
                                    name = newProfileNameInput.trim(),
                                    abxyMode = abxyMode,
                                    selectX = selectX, selectY = selectY, selectScale = selectScale,
                                    startX = startX, startY = startY, startScale = startScale,
                                    ltX = ltX, ltY = ltY, ltScale = ltScale,
                                    lbX = lbX, lbY = lbY, lbScale = lbScale,
                                    rbX = rbX, rbY = rbY, rbScale = rbScale,
                                    rtX = rtX, rtY = rtY, rtScale = rtScale,
                                    abxyX = abxyX, abxyY = abxyY, abxyScale = abxyScale, abxyInnerScale = abxyInnerScale,
                                    dpadX = dpadX, dpadY = dpadY, dpadScale = dpadScale,
                                    aX = aX, aY = aY, aScale = aScale,
                                    bX = bX, bY = bY, bScale = bScale,
                                    xX = xX, xY = xY, xScale = xScale,
                                    yX = yX, yY = yY, yScale = yScale
                                )
                                profilesList.add(newP)
                                activeProfileId = newId
                                saveProfilesToPrefs()
                                showCreateProfileDialog = false
                                onVibrate(20L)
                            }
                        }
                    ) {
                        Text("Guardar Perfil")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateProfileDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

// FREEFORM DRAGGABLE ITEM BOX WITH CORNER RESIZE DOT HANDLE
@Composable
fun BoxScope.FreeformItemBox(
    id: String,
    align: Alignment,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    isEditMode: Boolean,
    isSelected: Boolean,
    gamepadTheme: ZenGamepadThemeConfig,
    onSelect: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .align(align)
            .offset(x = offsetX.dp, y = offsetY.dp)
            .then(
                if (isEditMode) {
                    Modifier
                        .clickable { onSelect() }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) gamepadTheme.accentGlow else gamepadTheme.accentGlow.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = if (isSelected) gamepadTheme.accentGlow.copy(alpha = 0.18f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x / 2.5f, dragAmount.y / 2.5f)
                            }
                        }
                } else Modifier
            )
            .padding(3.dp)
    ) {
        content()

        // CORNER RESIZE HANDLE (PUNTITO DE ESCALA EN LA ESQUINA INFERIOR DERECHA)
        if (isEditMode && isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(16.dp)
                    .background(gamepadTheme.accentGlow, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (dragAmount.x + dragAmount.y) / 100f
                            onResize(delta)
                        }
                    }
            )
        }
    }
}

// ABXY DIAMOND CLUSTER COMPOSABLE (EXPANDED QUADRANT HITBOX ENGINE)
@Composable
fun XboxAbxyCluster(
    gamepadTheme: ZenGamepadThemeConfig,
    scale: Float,
    innerScale: Float = 1.05f,
    isEditMode: Boolean,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val effectiveKeyScale = scale * innerScale
    val keySize = 38.dp * effectiveKeyScale
    val gap = 6.dp * scale
    val totalSize = (38.dp * scale * 3) + (gap * 4)

    var activeBtn by remember { mutableStateOf<String?>(null) }

    fun sendBtnState(btnName: String?, isDown: Boolean) {
        if (btnName != null) {
            val valInt = if (isDown) 1 else 0
            onSendJson("""{"type":"gp_btn","name":"$btnName","value":$valInt}""")
        }
    }

    Surface(
        color = gamepadTheme.abxyHousingBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, gamepadTheme.abxyHousingBorder),
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(totalSize)
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val widthPx = size.width.toFloat()
                            val heightPx = size.height.toFloat()
                            val centerXPx = widthPx / 2f
                            val centerYPx = heightPx / 2f

                            fun processTouch(touchX: Float, touchY: Float): String {
                                val dx = touchX - centerXPx
                                val dy = touchY - centerYPx
                                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))

                                return when {
                                    angle >= -135.0 && angle < -45.0 -> "Y"
                                    angle >= -45.0 && angle < 45.0 -> "B"
                                    angle >= 45.0 && angle < 135.0 -> "A"
                                    else -> "X"
                                }
                            }

                            val target = processTouch(down.position.x, down.position.y)
                            activeBtn = target
                            sendBtnState(target, true)
                            onVibrate(15L)
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = target)

                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                                if (pointer != null && pointer.pressed) {
                                    val newTarget = processTouch(pointer.position.x, pointer.position.y)
                                    if (newTarget != activeBtn) {
                                        sendBtnState(activeBtn, false)
                                        activeBtn = newTarget
                                        sendBtnState(newTarget, true)
                                        onVibrate(15L)
                                        com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = newTarget)
                                    }
                                }
                            } while (pointer != null && pointer.pressed)

                            if (activeBtn != null) {
                                sendBtnState(activeBtn, false)
                                com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = activeBtn!!)
                                activeBtn = null
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(gap)
        ) {
            // Y Button (Top)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(keySize)
            ) {
                GamepadCircularKeyCap(
                    label = "Y",
                    scale = effectiveKeyScale,
                    gamepadTheme = gamepadTheme,
                    isPressed = activeBtn == "Y",
                    isEditMode = isEditMode,
                    modifier = Modifier.size(keySize)
                )
            }

            // X Button (Left)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(keySize)
            ) {
                GamepadCircularKeyCap(
                    label = "X",
                    scale = effectiveKeyScale,
                    gamepadTheme = gamepadTheme,
                    isPressed = activeBtn == "X",
                    isEditMode = isEditMode,
                    modifier = Modifier.size(keySize)
                )
            }

            // B Button (Right)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(keySize)
            ) {
                GamepadCircularKeyCap(
                    label = "B",
                    scale = effectiveKeyScale,
                    gamepadTheme = gamepadTheme,
                    isPressed = activeBtn == "B",
                    isEditMode = isEditMode,
                    modifier = Modifier.size(keySize)
                )
            }

            // A Button (Bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(keySize)
            ) {
                GamepadCircularKeyCap(
                    label = "A",
                    scale = effectiveKeyScale,
                    gamepadTheme = gamepadTheme,
                    isPressed = activeBtn == "A",
                    isEditMode = isEditMode,
                    modifier = Modifier.size(keySize)
                )
            }
        }
    }
}

// MECHANICAL D-PAD GRID (8-WAY DIAGONAL ENGINE)
@Composable
fun XboxDpadDisc(
    gamepadTheme: ZenGamepadThemeConfig,
    scale: Float,
    isEditMode: Boolean,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val gap = 6.dp * scale
    val keySize = 38.dp * scale
    val totalSize = (keySize * 3) + (gap * 4)

    var activeState by remember { mutableStateOf(Dpad8State.NEUTRAL) }

    fun sendState(state: Dpad8State) {
        onSendJson("""{"type":"gp_axis","name":"HAT0X","value":${state.hatX}}""")
        onSendJson("""{"type":"gp_axis","name":"HAT0Y","value":${state.hatY}}""")
    }

    Surface(
        color = gamepadTheme.dpadHousingBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, gamepadTheme.dpadHousingBorder),
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(totalSize)
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val widthPx = size.width.toFloat()
                            val heightPx = size.height.toFloat()
                            val centerXPx = widthPx / 2f
                            val centerYPx = heightPx / 2f
                            val deadzonePx = 10.dp.toPx()

                            fun processDirection(touchX: Float, touchY: Float) {
                                val dx = touchX - centerXPx
                                val dy = touchY - centerYPx
                                val dist = sqrt(dx * dx + dy * dy)

                                if (dist < deadzonePx) {
                                    if (activeState != Dpad8State.NEUTRAL) {
                                        activeState = Dpad8State.NEUTRAL
                                        sendState(Dpad8State.NEUTRAL)
                                    }
                                    return
                                }

                                val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0
                                val newState = when {
                                    angle >= 337.5 || angle < 22.5 -> Dpad8State.RIGHT
                                    angle >= 22.5 && angle < 67.5 -> Dpad8State.DOWN_RIGHT
                                    angle >= 67.5 && angle < 112.5 -> Dpad8State.DOWN
                                    angle >= 112.5 && angle < 157.5 -> Dpad8State.DOWN_LEFT
                                    angle >= 157.5 && angle < 202.5 -> Dpad8State.LEFT
                                    angle >= 202.5 && angle < 247.5 -> Dpad8State.UP_LEFT
                                    angle >= 247.5 && angle < 292.5 -> Dpad8State.UP
                                    else -> Dpad8State.UP_RIGHT
                                }

                                if (newState != activeState) {
                                    activeState = newState
                                    sendState(newState)
                                    onVibrate(12L)
                                    com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = newState.name)
                                }
                            }

                            processDirection(down.position.x, down.position.y)

                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                                if (pointer != null && pointer.pressed) {
                                    processDirection(pointer.position.x, pointer.position.y)
                                }
                            } while (pointer != null && pointer.pressed)

                            if (activeState != Dpad8State.NEUTRAL) {
                                activeState = Dpad8State.NEUTRAL
                                sendState(Dpad8State.NEUTRAL)
                                com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = "DPAD")
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(gap)
        ) {
            Box(modifier = Modifier.align(Alignment.TopStart).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "↖",
                    isPressed = activeState == Dpad8State.UP_LEFT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }

            Box(modifier = Modifier.align(Alignment.TopCenter).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "▲",
                    isPressed = activeState == Dpad8State.UP || activeState == Dpad8State.UP_LEFT || activeState == Dpad8State.UP_RIGHT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }

            Box(modifier = Modifier.align(Alignment.TopEnd).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "↗",
                    isPressed = activeState == Dpad8State.UP_RIGHT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }

            Box(modifier = Modifier.align(Alignment.CenterStart).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "◀",
                    isPressed = activeState == Dpad8State.LEFT || activeState == Dpad8State.UP_LEFT || activeState == Dpad8State.DOWN_LEFT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.Center).size(keySize)) {
                Box(
                    modifier = Modifier
                        .size(keySize * 0.55f)
                        .background(gamepadTheme.dpadKeyShadow, CircleShape)
                        .border(1.dp, gamepadTheme.dpadHousingBorder, CircleShape)
                )
            }

            Box(modifier = Modifier.align(Alignment.CenterEnd).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "▶",
                    isPressed = activeState == Dpad8State.RIGHT || activeState == Dpad8State.UP_RIGHT || activeState == Dpad8State.DOWN_RIGHT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomStart).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "↙",
                    isPressed = activeState == Dpad8State.DOWN_LEFT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "▼",
                    isPressed = activeState == Dpad8State.DOWN || activeState == Dpad8State.DOWN_LEFT || activeState == Dpad8State.DOWN_RIGHT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd).size(keySize)) {
                GamepadRhombusKeyCap(
                    label = "↘",
                    isPressed = activeState == Dpad8State.DOWN_RIGHT,
                    scale = scale,
                    gamepadTheme = gamepadTheme
                )
            }
        }
    }
}

// 8-WAY DPAD STATE ENUM
enum class Dpad8State(val hatX: Int, val hatY: Int) {
    NEUTRAL(0, 0),
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    UP_RIGHT(1, -1),
    UP_LEFT(-1, -1),
    DOWN_RIGHT(1, 1),
    DOWN_LEFT(-1, 1)
}
