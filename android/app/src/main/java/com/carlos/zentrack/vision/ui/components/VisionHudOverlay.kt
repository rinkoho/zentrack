package com.carlos.zentrack.vision.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.vision.data.HandSkeleton
import com.carlos.zentrack.vision.data.TrackedHand
import com.carlos.zentrack.vision.data.VisionFrameResult

@Composable
fun VisionHudOverlay(
    frameResult: VisionFrameResult,
    currentTheme: ZenThemeConfig,
    showSkeleton: Boolean = true,
    showFingertipLabels: Boolean = true,
    mirrorX: Boolean = true,
    cameraAspectRatio: Float = 16f / 9f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0 || height <= 0) return@Canvas

        for (hand in frameResult.hands) {
            drawHandOverlay(
                hand = hand,
                width = width,
                height = height,
                theme = currentTheme,
                showSkeleton = showSkeleton,
                showFingertipLabels = showFingertipLabels,
                mirrorX = mirrorX,
                cameraAspectRatio = cameraAspectRatio
            )
        }
    }
}

private fun DrawScope.drawHandOverlay(
    hand: TrackedHand,
    width: Float,
    height: Float,
    theme: ZenThemeConfig,
    showSkeleton: Boolean,
    showFingertipLabels: Boolean,
    mirrorX: Boolean,
    cameraAspectRatio: Float
) {
    val landmarks = hand.landmarks
    if (landmarks.isEmpty()) return

    // Exact FILL_CENTER viewport transformation
    val screenAr = width / height
    val points = landmarks.map { p ->
        val rawX = if (mirrorX) (1.0f - p.x) else p.x
        val rawY = p.y

        val screenX: Float
        val screenY: Float

        if (screenAr > cameraAspectRatio) {
            // Screen is wider than camera: camera is scaled to fit width, cropped top/bottom
            val scaledHeight = width / cameraAspectRatio
            val cropY = (scaledHeight - height) / 2f
            screenX = rawX * width
            screenY = rawY * scaledHeight - cropY
        } else {
            // Screen is narrower than camera: camera is scaled to fit height, cropped left/right
            val scaledWidth = height * cameraAspectRatio
            val cropX = (scaledWidth - width) / 2f
            screenX = rawX * scaledWidth - cropX
            screenY = rawY * height
        }

        Offset(screenX, screenY)
    }

    val primaryColor = theme.primaryAccent
    val secondaryColor = theme.secondaryAccent

    // 1. Draw Bounding Box / Corner Brackets
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (p in points) {
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }

    val padding = 24.dp.toPx()
    val boxLeft = (minX - padding).coerceAtLeast(0f)
    val boxTop = (minY - padding).coerceAtLeast(0f)
    val boxRight = (maxX + padding).coerceAtMost(width)
    val boxBottom = (maxY + padding).coerceAtMost(height)

    drawCyberpunkBrackets(
        left = boxLeft,
        top = boxTop,
        right = boxRight,
        bottom = boxBottom,
        bracketLength = 20.dp.toPx(),
        color = primaryColor.copy(alpha = 0.6f)
    )

    // 2. Draw Skeletal Connection Lines
    if (showSkeleton) {
        for ((startIdx, endIdx) in HandSkeleton.CONNECTIONS) {
            if (startIdx < points.size && endIdx < points.size) {
                val start = points[startIdx]
                val end = points[endIdx]

                // Glowing outer line
                drawLine(
                    color = primaryColor.copy(alpha = 0.35f),
                    start = start,
                    end = end,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Core neon line
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor),
                        start = start,
                        end = end
                    ),
                    start = start,
                    end = end,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // 3. Draw Joint Nodes
    for (i in points.indices) {
        val pt = points[i]
        val isFingertip = i in HandSkeleton.FINGERTIP_INDICES
        val isKnuckle = i in HandSkeleton.KNUCKLE_INDICES

        if (isFingertip) {
            // Fingertip Radar: Multi-layered pulsing rings
            drawCircle(
                color = secondaryColor.copy(alpha = 0.3f),
                radius = 14.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = primaryColor,
                radius = 9.dp.toPx(),
                center = pt,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = pt
            )

            // Index tip crosshair (Pointer focus)
            if (i == 8) {
                val armLen = 8.dp.toPx()
                drawLine(
                    color = primaryColor,
                    start = Offset(pt.x - armLen, pt.y),
                    end = Offset(pt.x + armLen, pt.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(pt.x, pt.y - armLen),
                    end = Offset(pt.x, pt.y + armLen),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        } else if (isKnuckle) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.4f),
                radius = 6.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = primaryColor,
                radius = 3.5.dp.toPx(),
                center = pt
            )
        } else {
            drawCircle(
                color = theme.textMuted.copy(alpha = 0.8f),
                radius = 2.5.dp.toPx(),
                center = pt
            )
        }
    }
}

private fun DrawScope.drawCyberpunkBrackets(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    bracketLength: Float,
    color: Color
) {
    val strokeWidth = 2.dp.toPx()

    // Top-Left
    drawLine(color, Offset(left, top), Offset(left + bracketLength, top), strokeWidth)
    drawLine(color, Offset(left, top), Offset(left, top + bracketLength), strokeWidth)

    // Top-Right
    drawLine(color, Offset(right, top), Offset(right - bracketLength, top), strokeWidth)
    drawLine(color, Offset(right, top), Offset(right, top + bracketLength), strokeWidth)

    // Bottom-Left
    drawLine(color, Offset(left, bottom), Offset(left + bracketLength, bottom), strokeWidth)
    drawLine(color, Offset(left, bottom), Offset(left, bottom - bracketLength), strokeWidth)

    // Bottom-Right
    drawLine(color, Offset(right, bottom), Offset(right - bracketLength, bottom), strokeWidth)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - bracketLength), strokeWidth)
}
