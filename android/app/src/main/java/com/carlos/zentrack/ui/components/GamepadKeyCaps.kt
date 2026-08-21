package com.carlos.zentrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenGamepadThemeConfig
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 1. D-PAD RHOMBUS & DIAGONAL KEYCAP (Dedicated 3D Geometry for Directional Keys)
 */
@Composable
fun GamepadRhombusKeyCap(
    label: String,
    isPressed: Boolean,
    scale: Float,
    gamepadTheme: ZenGamepadThemeConfig,
    modifier: Modifier = Modifier
) {
    val keyBg = if (isPressed) gamepadTheme.dpadKeyActiveBg else gamepadTheme.dpadKeyBg
    val textColor = if (isPressed) gamepadTheme.dpadKeyActiveText else gamepadTheme.dpadKeyText
    val shadowColor = if (isPressed) gamepadTheme.dpadKeyActiveBg.copy(alpha = 0.5f) else gamepadTheme.dpadKeyShadow

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(y = if (isPressed) 2.5.dp else 0.dp)
    ) {
        // Layer 1: Ambient Drop Shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 1.5.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(7.dp))
        )

        // Layer 2: 3D Base Shadow Rim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shadowColor, RoundedCornerShape(7.dp))
        )

        // Layer 3: Top Keycap Face
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 1.dp else 3.5.dp)
                .background(keyBg, RoundedCornerShape(6.dp))
                .border(
                    width = 0.8.dp,
                    color = if (isPressed) gamepadTheme.accentGlow else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = (13.sp.value * scale).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * 2. ABXY CIRCULAR KEYCAP (Supports Parent Quadrant Driven Press State & Standalone Press State)
 */
@Composable
fun GamepadCircularKeyCap(
    label: String,
    scale: Float,
    gamepadTheme: ZenGamepadThemeConfig,
    isPressed: Boolean = false,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null
) {
    var internalIsPressed by remember { mutableStateOf(false) }
    val effectivePressed = isPressed || internalIsPressed

    val keyBg = if (effectivePressed) gamepadTheme.abxyKeyActiveBg else gamepadTheme.abxyKeyBg
    val textColor = if (effectivePressed) gamepadTheme.abxyKeyActiveText else gamepadTheme.abxyKeyText
    val shadowColor = if (effectivePressed) gamepadTheme.abxyKeyActiveBg.copy(alpha = 0.5f) else gamepadTheme.abxyKeyShadow

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!isEditMode && onDown != null && onUp != null) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val widthPx = size.width.toFloat()
                            val heightPx = size.height.toFloat()
                            val centerXPx = widthPx / 2f
                            val centerYPx = heightPx / 2f
                            val radiusPx = min(widthPx, heightPx) / 2f

                            val touchX = down.position.x
                            val touchY = down.position.y
                            val dist = sqrt((touchX - centerXPx).pow(2) + (touchY - centerYPx).pow(2))

                            if (dist > radiusPx) {
                                return@awaitEachGesture
                            }

                            down.consume()
                            internalIsPressed = true
                            onDown()
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = label)
                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                            } while (pointer != null && pointer.pressed)
                            internalIsPressed = false
                            onUp()
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = label)
                        }
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (effectivePressed) 2.5.dp else 0.dp)
        ) {
            // Layer 1: Ambient Drop Shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 1.5.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
            )

            // Layer 2: 3D Base Shadow Rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shadowColor, CircleShape)
            )

            // Layer 3: Top Keycap Face
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (effectivePressed) 1.dp else 3.5.dp)
                    .background(keyBg, CircleShape)
                    .border(
                        width = 0.8.dp,
                        color = if (effectivePressed) gamepadTheme.accentGlow else Color.White.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = (15.sp.value * scale).sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * 3. BUMPER & TRIGGER RECTANGULAR KEYCAP (LB / LT / RB / RT)
 */
@Composable
fun GamepadRectangularKeyCap(
    label: String,
    scale: Float,
    gamepadTheme: ZenGamepadThemeConfig,
    isTrigger: Boolean = false,
    isEditMode: Boolean = false,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val width = (if (isTrigger) 52.dp else 46.dp) * scale
    val height = 30.dp * scale

    val keyBg = if (isPressed) gamepadTheme.bumperKeyActiveBg else gamepadTheme.bumperKeyBg
    val textColor = if (isPressed) gamepadTheme.bumperKeyActiveText else gamepadTheme.bumperKeyText
    val shadowColor = if (isPressed) gamepadTheme.bumperKeyActiveBg.copy(alpha = 0.5f) else gamepadTheme.bumperKeyShadow

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            isPressed = true
                            onDown()
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = label)
                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                            } while (pointer != null && pointer.pressed)
                            isPressed = false
                            onUp()
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = label)
                        }
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (isPressed) 2.5.dp else 0.dp)
        ) {
            // Layer 1: Ambient Drop Shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 1.5.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            )

            // Layer 2: 3D Base Shadow Rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shadowColor, RoundedCornerShape(8.dp))
            )

            // Layer 3: Top Keycap Face
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isPressed) 1.dp else 3.5.dp)
                    .background(keyBg, RoundedCornerShape(7.dp))
                    .border(
                        width = 0.8.dp,
                        color = if (isPressed) gamepadTheme.accentGlow else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(7.dp)
                    )
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = (11.5.sp.value * scale).sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * 4. SPECIAL PILL KEYCAP (SELECT / START -> Symmetrical Press Animation & 3D Depth)
 */
@Composable
fun GamepadPillKeyCap(
    label: String,
    icon: ImageVector? = null,
    scale: Float,
    gamepadTheme: ZenGamepadThemeConfig,
    isEditMode: Boolean = false,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val size = 28.dp * scale

    val keyBg = if (isPressed) gamepadTheme.pillKeyActiveBg else gamepadTheme.pillKeyBg
    val textColor = if (isPressed) gamepadTheme.pillKeyActiveText else gamepadTheme.pillKeyText
    val shadowColor = if (isPressed) gamepadTheme.pillKeyActiveBg.copy(alpha = 0.5f) else gamepadTheme.pillKeyShadow

    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            isPressed = true
                            onDown()
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = label)
                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                            } while (pointer != null && pointer.pressed)
                            isPressed = false
                            onUp()
                            com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = label)
                        }
                    }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (isPressed) 2.5.dp else 0.dp)
        ) {
            // Layer 1: Ambient Drop Shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 1.5.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
            )

            // Layer 2: 3D Base Shadow Rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shadowColor, CircleShape)
            )

            // Layer 3: Top Keycap Face
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isPressed) 1.dp else 3.5.dp)
                    .background(keyBg, CircleShape)
                    .border(
                        width = 0.8.dp,
                        color = if (isPressed) gamepadTheme.accentGlow else Color.White.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = textColor,
                        modifier = Modifier.size((14.sp.value * scale).dp)
                    )
                } else {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = (11.sp.value * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
