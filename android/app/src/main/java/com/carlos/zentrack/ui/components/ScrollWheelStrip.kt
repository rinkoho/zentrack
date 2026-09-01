package com.carlos.zentrack.ui.components

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScrollWheelStrip(
    naturalScroll: Boolean,
    cardColor: Color = Color(0xFF24283B),
    iconColor: Color = Color(0xFF565F89),
    isLeftPosition: Boolean = false,
    modifier: Modifier = Modifier,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var lastStripY by remember { mutableFloatStateOf(0f) }
    var stripAccumulator by remember { mutableFloatStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }

    val activeAlpha by animateFloatAsState(
        targetValue = if (isTouching) 0.95f else 0.40f,
        label = "scrollStripAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isTouching = true
                        lastStripY = motionEvent.y
                        stripAccumulator = 0f
                        onVibrate(10L)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.y - lastStripY
                        stripAccumulator += deltaY

                        if (abs(stripAccumulator) >= 12f) {
                            val steps = (abs(stripAccumulator) / 12f).toInt()
                            var dir = if (stripAccumulator > 0) "down" else "up"
                            if (naturalScroll) {
                                dir = if (stripAccumulator > 0) "up" else "down"
                            }
                            val json = JSONObject()
                                .put("type", "scroll")
                                .put("direction", dir)
                                .put("steps", steps)
                            onSendJson(json.toString())
                            onVibrate(8L)
                            stripAccumulator %= 12f
                        }
                        lastStripY = motionEvent.y
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isTouching = false
                    }
                }
                true
            }
    ) {
        // Transparent Overlay with Discrete Dividing Line & Minimalist Vector Chevrons ( ^ and v )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f

            // 1. Discrete separator line against trackpad surface
            val sepX = if (isLeftPosition) w - 0.5.dp.toPx() else 0.5.dp.toPx()
            drawLine(
                color = iconColor.copy(alpha = 0.22f),
                start = Offset(sepX, h * 0.04f),
                end = Offset(sepX, h * 0.96f),
                strokeWidth = 1.dp.toPx()
            )

            // 2. Minimalist Up Chevron (^ Vector)
            val chevronW = (w * 0.45f).coerceIn(6.dp.toPx(), 14.dp.toPx())
            val chevronH = (chevronW * 0.55f).coerceIn(3.5.dp.toPx(), 8.dp.toPx())
            val topY = h * 0.10f

            val upPath = Path().apply {
                moveTo(centerX - chevronW / 2f, topY + chevronH)
                lineTo(centerX, topY)
                lineTo(centerX + chevronW / 2f, topY + chevronH)
            }
            drawPath(
                path = upPath,
                color = iconColor.copy(alpha = activeAlpha),
                style = Stroke(width = 1.4.dp.toPx())
            )

            // 3. Minimalist Down Chevron (v Vector)
            val botY = h * 0.90f - chevronH
            val downPath = Path().apply {
                moveTo(centerX - chevronW / 2f, botY)
                lineTo(centerX, botY + chevronH)
                lineTo(centerX + chevronW / 2f, botY)
            }
            drawPath(
                path = downPath,
                color = iconColor.copy(alpha = activeAlpha),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
    }
}
