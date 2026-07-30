package com.carlos.zentrack.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenThemeConfig
import org.json.JSONObject

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
    onPressStateChanged: ((Boolean) -> Unit)? = null,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

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

    Box(
        modifier = modifier
            .padding(2.5.dp)
            .fillMaxHeight()
            .offset(y = if (isPressed) 2.5.dp else 0.dp)
            .pointerInput(keyCode, label) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPressStateChanged?.invoke(true)
                    
                    // 100% Real Physical Keyboard Actuation
                    if (keyCode.isNotEmpty()) {
                        val json = JSONObject().put("type", "keydown").put("key", keyCode).toString()
                        val payloadToSend = com.carlos.zentrack.security.ZenCrypto.encryptKeyboardPayload(json) ?: json
                        onSendJson(payloadToSend)
                    }
                    onVibrate(12L)
                    com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = true, keyCode = keyCode)

                    waitForUpOrCancellation()

                    isPressed = false
                    onPressStateChanged?.invoke(false)
                    
                    // 100% Real Physical Keyboard Release
                    if (keyCode.isNotEmpty()) {
                        val upJson = JSONObject().put("type", "keyup").put("key", keyCode).toString()
                        val upPayloadToSend = com.carlos.zentrack.security.ZenCrypto.encryptKeyboardPayload(upJson) ?: upJson
                        onSendJson(upPayloadToSend)
                    }
                    com.carlos.zentrack.audio.ZenSoundEngine.playSwitchSound(isPress = false, keyCode = keyCode)
                }
            }
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

        // Layer 2: 3D Cherry MX Keycap Base Shadow Rim (Web box-shadow: 0 3px 0 shadowColor)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = shadowColor,
                    shape = RoundedCornerShape(7.dp)
                )
        )

        // Layer 3: Top Keycap Face (Web Cherry MX Profile Top Face)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 1.dp else 3.5.dp)
                .background(
                    color = keyBg,
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = if (isPressed) Color.Transparent else Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(6.dp)
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
                    // Main Label ON TOP (Larger Bold Text)
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = if (label.length > 5) 8.5.sp else if (label.length > 2) 9.5.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp
                    )
                    // Sublabel BELOW (Smaller Muted Symbol)
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
        }
    }
}




