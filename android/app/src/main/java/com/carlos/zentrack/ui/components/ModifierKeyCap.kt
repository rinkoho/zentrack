package com.carlos.zentrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.audio.ZenSoundEngine
import com.carlos.zentrack.theme.ZenThemeConfig
import kotlinx.coroutines.withTimeoutOrNull

enum class ModifierMode {
    OFF, STICKY, LOCKED
}

@Composable
fun ModifierKeyCap(
    label: String,
    keyCode: String,
    theme: ZenThemeConfig,
    mode: ModifierMode,
    modifier: Modifier = Modifier,
    nerdSymbol: String? = null,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    val isLatched = mode != ModifierMode.OFF
    val isLocked = mode == ModifierMode.LOCKED

    // 100% ISOLATED AUDIO ENGINE: Sound plays ONLY when the key's 3D animation state changes
    var previousLatched by remember { mutableStateOf(isLatched) }
    LaunchedEffect(isLatched) {
        if (isLatched != previousLatched) {
            if (isLatched) {
                ZenSoundEngine.playSwitchSound(isPress = true, keyCode = keyCode)
            } else {
                ZenSoundEngine.playSwitchSound(isPress = false, keyCode = keyCode)
            }
            previousLatched = isLatched
        }
    }

    val keyBg = if (isLatched) theme.keyModActiveBg else theme.keyModBg
    val textColor = theme.keyModText
    val shadowColor = theme.keyModShadow

    // Outer Box: FULL CELL HITBOX (0 gaps / 0 dead zones across the grid)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(keyCode, mode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var isLongPressed = false

                    val upOrCancel = withTimeoutOrNull(350L) {
                        waitForUpOrCancellation()
                    }

                    if (upOrCancel == null) {
                        isLongPressed = true
                        onVibrate(30L)
                        onLongPress()
                        waitForUpOrCancellation()
                    } else {
                        if (!isLongPressed) {
                            onVibrate(12L)
                            onTap()
                        }
                    }
                }
            }
    ) {
        // Inner Box: Visual Keycap Face & Shadow (Preserves exact 3D appearance & visual spacing)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.0.dp)
                .offset(y = if (isLatched) 2.5.dp else 0.dp)
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
                    .padding(bottom = if (isLatched) 1.dp else 3.5.dp)
                    .background(
                        color = keyBg,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .then(
                        if (com.carlos.zentrack.preferences.ZenPreferences.keycapBordersEnabled) {
                            Modifier.border(
                                width = 0.5.dp,
                                color = if (isLatched) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (nerdSymbol != null) {
                    Text(
                        text = nerdSymbol,
                        fontFamily = com.carlos.zentrack.ui.theme.NerdFontFamily,
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(x = (-2).dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = if (label.length > 5) 8.5.sp else if (label.length > 2) 9.5.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                        )
                    }
                }

                // Sleek Mechanical Keyboard LED Dot Indicator for LOCKED Mode
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 3.dp, end = 3.dp)
                            .size(5.dp)
                            .background(theme.primaryAccent, CircleShape)
                    )
                }
            }
        }
    }
}
