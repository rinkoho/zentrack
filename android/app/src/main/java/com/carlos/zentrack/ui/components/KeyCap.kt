package com.carlos.zentrack.ui.components

import android.graphics.CornerPathEffect
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenThemeConfig
import org.json.JSONObject

/**
 * Centered Rounded Vector Arrow Indicator for Keyboard Directional Keys ( ▲ / ▼ / ◀ / ▶ )
 */
@Composable
fun RoundedArrowIndicator(
    direction: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = android.graphics.Path()
        when (direction) {
            "▲", "Up" -> {
                path.moveTo(cx, cy - h * 0.44f)
                path.lineTo(cx + w * 0.44f, cy + h * 0.38f)
                path.lineTo(cx - w * 0.44f, cy + h * 0.38f)
                path.close()
            }
            "▼", "Down" -> {
                path.moveTo(cx, cy + h * 0.44f)
                path.lineTo(cx - w * 0.44f, cy - h * 0.38f)
                path.lineTo(cx + w * 0.44f, cy - h * 0.38f)
                path.close()
            }
            "◀", "Left" -> {
                path.moveTo(cx - w * 0.44f, cy)
                path.lineTo(cx + w * 0.38f, cy - h * 0.44f)
                path.lineTo(cx + w * 0.38f, cy + h * 0.44f)
                path.close()
            }
            "▶", "Right" -> {
                path.moveTo(cx + w * 0.44f, cy)
                path.lineTo(cx - w * 0.38f, cy + h * 0.44f)
                path.lineTo(cx - w * 0.38f, cy - h * 0.44f)
                path.close()
            }
        }

        val paint = Paint().apply {
            this.color = color.toArgb()
            this.style = Paint.Style.FILL
            this.pathEffect = CornerPathEffect(2.2.dp.toPx())
            this.isAntiAlias = true
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(path, paint)
        }
    }
}

@Composable
fun KeyCap(
    label: String,
    keyCode: String,
    theme: ZenThemeConfig,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    icon: ImageVector? = null,
    nerdSymbol: String? = null,
    isAccent: Boolean = false,
    isModifier: Boolean = false,
    externalPressed: Boolean = false,
    isLedOn: Boolean = false,
    onPressStateChanged: ((Boolean) -> Unit)? = null,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var internalIsPressed by remember { mutableStateOf(false) }
    val isPressed = internalIsPressed || externalPressed
    val currentKeyCode by rememberUpdatedState(keyCode)

    val keyBg = when {
        isPressed -> if (isAccent) theme.keyAccentActiveBg else if (isModifier) theme.keyModActiveBg else theme.keyAlphaActiveBg
        isAccent -> theme.keyAccentBg
        isModifier -> theme.keyModBg
        else -> theme.keyAlphaBg
    }

    val textColor = when {
        isAccent -> theme.keyAccentText
        isModifier -> theme.keyModText
        else -> theme.keyAlphaText
    }

    val shadowColor = when {
        isAccent -> theme.keyAccentShadow
        isModifier -> theme.keyModShadow
        else -> theme.keyAlphaShadow
    }

    val sublabelColor = textColor.copy(alpha = 0.65f)

    // Outer Box: FULL CELL HITBOX (0 gaps / 0 dead zones across the grid)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val activeKey = currentKeyCode
                    internalIsPressed = true
                    
                    if (activeKey.isNotEmpty()) {
                        val json = JSONObject().put("type", "keydown").put("key", activeKey).toString()
                        onSendJson(json)
                    }
                    onVibrate(12L)
                    com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = activeKey)
                    onPressStateChanged?.invoke(true)

                    waitForUpOrCancellation()

                    internalIsPressed = false
                    
                    if (activeKey.isNotEmpty()) {
                        val upJson = JSONObject().put("type", "keyup").put("key", activeKey).toString()
                        onSendJson(upJson)
                    }
                    com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = activeKey)
                    onPressStateChanged?.invoke(false)
                }
            }
    ) {
        // Inner Box: Visual Keycap Face & Shadow (Preserves exact 3D appearance & visual spacing)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.0.dp)
                .offset(y = if (isPressed) 2.5.dp else 0.dp)
        ) {
            // Layer 1: Ambient Drop Shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 1.5.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(7.dp)
                    )
            )

            // Layer 2: 3D Cherry MX Keycap Base Shadow Rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = shadowColor,
                        shape = RoundedCornerShape(7.dp)
                    )
            )

            // Layer 3: Top Keycap Face
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isPressed) 1.dp else 3.5.dp)
                    .background(
                        color = keyBg,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .then(
                        if (com.carlos.zentrack.preferences.ZenPreferences.keycapBordersEnabled) {
                            Modifier.border(
                                width = 0.5.dp,
                                color = if (isPressed) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                val isArrow = label in listOf("▲", "▼", "◀", "▶") || keyCode in listOf("Up", "Down", "Left", "Right")

                if (isArrow) {
                    RoundedArrowIndicator(
                        direction = label.ifEmpty { keyCode },
                        color = textColor,
                        modifier = Modifier.size(12.5.dp)
                    )
                } else if (nerdSymbol != null) {
                    Text(
                        text = nerdSymbol,
                        fontFamily = com.carlos.zentrack.ui.theme.NerdFontFamily,
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(x = (-2).dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Main Label ON TOP
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = if (label.length > 5) 8.5.sp else if (label.length > 2) 9.5.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                        )
                        // Sublabel BELOW
                        if (sublabel != null) {
                            Text(
                                text = sublabel,
                                color = sublabelColor,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 8.sp
                            )
                        }
                    }
                }

                if (isLedOn) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 3.dp, end = 3.dp)
                            .size(5.dp)
                            .background(theme.primaryAccent, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
    }
}
