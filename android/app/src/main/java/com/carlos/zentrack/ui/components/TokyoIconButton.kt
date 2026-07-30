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
import org.json.JSONObject

@Composable
fun TokyoIconButton(
    icon: ImageVector,
    label: String,
    buttonCode: Int,
    activeColor: Color,
    cardColor: Color = Color(0xFF24283B),
    textColor: Color = Color(0xFFC0CAF5),
    modifier: Modifier = Modifier,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isPressed) activeColor else cardColor,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (isPressed) activeColor else activeColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(buttonCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onSendJson(JSONObject().put("type", "mousedown").put("button", buttonCode).toString())
                    onVibrate(20L)

                    waitForUpOrCancellation()

                    isPressed = false
                    onSendJson(JSONObject().put("type", "mouseup").put("button", buttonCode).toString())
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) Color.Black else activeColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isPressed) Color.Black else textColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

