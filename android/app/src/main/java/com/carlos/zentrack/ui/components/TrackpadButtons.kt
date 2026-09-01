package com.carlos.zentrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carlos.zentrack.audio.ZenSoundEngine
import com.carlos.zentrack.preferences.ZenPreferences
import com.carlos.zentrack.theme.ZenThemeConfig
import org.json.JSONObject

/**
 * Minimalist Mouse Vector Indicator (Clean silhouette with active button highlight)
 */
@Composable
fun MinimalistMouseIcon(
    buttonCode: Int,
    isPressed: Boolean,
    accentColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val mouseW = (w * 0.52f).coerceIn(12.dp.toPx(), 22.dp.toPx())
        val mouseH = (h * 0.62f).coerceIn(16.dp.toPx(), 28.dp.toPx())
        val left = (w - mouseW) / 2f
        val top = (h - mouseH) / 2f
        val r = mouseW * 0.38f

        val strokeColor = if (isPressed) accentColor else mutedColor.copy(alpha = 0.55f)
        val activeFill = if (isPressed) accentColor else accentColor.copy(alpha = 0.85f)

        // Mouse Outer Body Outline
        val bodyPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = left + mouseW,
                    bottom = top + mouseH,
                    radiusX = r,
                    radiusY = r
                )
            )
        }
        drawPath(bodyPath, color = strokeColor, style = Stroke(width = 1.2.dp.toPx()))

        val midX = left + mouseW / 2f
        val splitY = top + mouseH * 0.45f

        // Horizontal split line separating top click buttons from palm rest
        drawLine(
            color = strokeColor.copy(alpha = 0.45f),
            start = Offset(left, splitY),
            end = Offset(left + mouseW, splitY),
            strokeWidth = 1.dp.toPx()
        )

        // Vertical split line separating left and right click zones
        drawLine(
            color = strokeColor.copy(alpha = 0.45f),
            start = Offset(midX, top),
            end = Offset(midX, splitY),
            strokeWidth = 1.dp.toPx()
        )

        // Active Segment Highlight
        when (buttonCode) {
            1 -> { // Left Click Highlight
                val leftClickPath = Path().apply {
                    moveTo(left, splitY)
                    lineTo(left, top + r)
                    arcTo(Rect(left, top, left + 2 * r, top + 2 * r), 180f, 90f, false)
                    lineTo(midX, top)
                    lineTo(midX, splitY)
                    close()
                }
                drawPath(leftClickPath, color = activeFill)
            }
            2 -> { // Middle Click (Center Wheel)
                val wheelW = (mouseW * 0.22f).coerceIn(2.5.dp.toPx(), 4.5.dp.toPx())
                val wheelH = (mouseH * 0.28f).coerceIn(4.dp.toPx(), 8.dp.toPx())
                val wheelLeft = midX - wheelW / 2f
                val wheelTop = top + (splitY - top - wheelH) / 2f

                drawRoundRect(
                    color = activeFill,
                    topLeft = Offset(wheelLeft, wheelTop),
                    size = Size(wheelW, wheelH),
                    cornerRadius = CornerRadius(wheelW / 2f, wheelW / 2f)
                )
            }
            3 -> { // Right Click Highlight
                val rightClickPath = Path().apply {
                    moveTo(midX, splitY)
                    lineTo(midX, top)
                    lineTo(left + mouseW - r, top)
                    arcTo(Rect(left + mouseW - 2 * r, top, left + mouseW, top + 2 * r), 270f, 90f, false)
                    lineTo(left + mouseW, splitY)
                    close()
                }
                drawPath(rightClickPath, color = activeFill)
            }
        }
    }
}

/**
 * 3-Layer Keyboard/Gamepad-grade Tactile Keycap Button for Trackpad
 * Features:
 * - 100% Full-Cell Consuming Hitbox via down.consume() (Instant 0ms mousedown anywhere across corners & edges)
 * - 3-Layer 3D Mechanical Geometry (Ambient Drop Shadow + 3D Base Shadow Rim + Top Keycap Face)
 * - Clean tactile color highlight without harsh neon borders
 * - Configurable semi-transparent border via ZenPreferences.keycapBordersEnabled
 * - Tactile press offset & drop (2.5dp drop on press)
 * - ZenSoundEngine switch clicks + Haptic vibration
 */
@Composable
fun TrackpadKeycapButton(
    buttonCode: Int,
    theme: ZenThemeConfig,
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 8.dp,
    isMiddleButton: Boolean = false,
    onButtonPressChanged: ((Boolean) -> Unit)? = null,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val accentColor = if (isMiddleButton) theme.secondaryAccent else theme.primaryAccent
    val keyBg = if (isPressed) {
        if (isMiddleButton) theme.keyModActiveBg else theme.keyAlphaActiveBg
    } else {
        if (isMiddleButton) theme.keyModBg else theme.keyAlphaBg
    }
    val shadowColor = if (isMiddleButton) theme.keyModShadow else theme.keyAlphaShadow
    val shape = RoundedCornerShape(shapeRadius)

    // OUTER CONTAINER: 100% FULL-CELL HITBOX WITH EVENT CONSUMPTION
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(buttonCode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onButtonPressChanged?.invoke(true)
                    onSendJson(JSONObject().put("type", "mousedown").put("button", buttonCode).toString())
                    onVibrate(15L)
                    ZenSoundEngine.playSwitchSound(isPress = true, keyCode = "mouse_$buttonCode")

                    val up = waitForUpOrCancellation()
                    up?.consume()

                    isPressed = false
                    onButtonPressChanged?.invoke(false)
                    onSendJson(JSONObject().put("type", "mouseup").put("button", buttonCode).toString())
                    ZenSoundEngine.playSwitchSound(isPress = false, keyCode = "mouse_$buttonCode")
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 3-LAYER 3D KEYCAP ARCHITECTURE (IDENTICAL TO KEYBOARD & GAMEPAD ENGINE)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .offset(y = if (isPressed) 2.5.dp else 0.dp)
        ) {
            // Layer 1: Ambient Drop Shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 1.5.dp)
                    .background(Color.Black.copy(alpha = 0.65f), shape)
            )

            // Layer 2: 3D Base Shadow Rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shadowColor, shape)
            )

            // Layer 3: Top Keycap Face (Clean color highlight, border disabled if keycapBordersEnabled is false)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isPressed) 1.dp else 3.5.dp)
                    .background(keyBg, shape)
                    .then(
                        if (ZenPreferences.keycapBordersEnabled) {
                            Modifier.border(
                                width = 0.8.dp,
                                color = Color.White.copy(alpha = if (isPressed) 0.18f else 0.08f),
                                shape = shape
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                MinimalistMouseIcon(
                    buttonCode = buttonCode,
                    isPressed = isPressed,
                    accentColor = accentColor,
                    mutedColor = theme.textMuted,
                    modifier = Modifier.size(if (isMiddleButton) 20.dp else 26.dp)
                )
            }
        }
    }
}

/**
 * Bottom Trackpad Click Bar (Seamless Floating 3D Keycaps)
 * 3-Button Layout:
 * - Left Click (weight: 1.0f)
 * - Middle Click (weight: 0.35f -> ~35% size)
 * - Right Click (weight: 1.0f)
 */
@Composable
fun TrackpadBottomBar(
    currentTheme: ZenThemeConfig,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    onButtonPressChanged: ((Boolean) -> Unit)? = null,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Click (Large)
        TrackpadKeycapButton(
            buttonCode = 1,
            theme = currentTheme,
            shapeRadius = 7.dp,
            onButtonPressChanged = onButtonPressChanged,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            onSendJson = onSendJson,
            onVibrate = onVibrate
        )

        // Middle Click (Small ~35%)
        TrackpadKeycapButton(
            buttonCode = 2,
            theme = currentTheme,
            shapeRadius = 5.dp,
            isMiddleButton = true,
            onButtonPressChanged = onButtonPressChanged,
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight(),
            onSendJson = onSendJson,
            onVibrate = onVibrate
        )

        // Right Click (Large)
        TrackpadKeycapButton(
            buttonCode = 3,
            theme = currentTheme,
            shapeRadius = 7.dp,
            onButtonPressChanged = onButtonPressChanged,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            onSendJson = onSendJson,
            onVibrate = onVibrate
        )
    }
}

/**
 * Vertical Trackpad Sidebar (Seamless Floating 3D Keycaps)
 * 3-Button Layout:
 * - Top: Left Click (weight: 1.0f)
 * - Mid: Middle Click (weight: 0.35f -> ~35% size)
 * - Bottom: Right Click (weight: 1.0f)
 */
@Composable
fun TrackpadVerticalButtons(
    currentTheme: ZenThemeConfig,
    modifier: Modifier = Modifier,
    onButtonPressChanged: ((Boolean) -> Unit)? = null,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Button: Left Click (Large)
        TrackpadKeycapButton(
            buttonCode = 1,
            theme = currentTheme,
            shapeRadius = 7.dp,
            onButtonPressChanged = onButtonPressChanged,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth(),
            onSendJson = onSendJson,
            onVibrate = onVibrate
        )

        // Center Button: Middle Click (Small ~35%)
        TrackpadKeycapButton(
            buttonCode = 2,
            theme = currentTheme,
            shapeRadius = 5.dp,
            isMiddleButton = true,
            onButtonPressChanged = onButtonPressChanged,
            modifier = Modifier
                .weight(0.35f)
                .fillMaxWidth(),
            onSendJson = onSendJson,
            onVibrate = onVibrate
        )

        // Bottom Button: Right Click (Large)
        TrackpadKeycapButton(
            buttonCode = 3,
            theme = currentTheme,
            shapeRadius = 7.dp,
            onButtonPressChanged = onButtonPressChanged,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth(),
            onSendJson = onSendJson,
            onVibrate = onVibrate
        )
    }
}
