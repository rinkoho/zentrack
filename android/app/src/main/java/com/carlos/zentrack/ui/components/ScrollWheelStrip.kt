package com.carlos.zentrack.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var lastStripY by remember { mutableFloatStateOf(0f) }
    var stripAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(32.dp)
            .fillMaxHeight()
            .background(cardColor.copy(alpha = 0.6f))
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
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
                }
                true
            }
    ) {
        Icon(
            imageVector = Icons.Default.UnfoldMore,
            contentDescription = "Scroll Strip",
            tint = iconColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
        )
    }
}

