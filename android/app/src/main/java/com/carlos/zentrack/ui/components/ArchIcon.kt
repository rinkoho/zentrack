package com.carlos.zentrack.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ArchIcon: ImageVector
    get() {
        if (_archIcon != null) return _archIcon!!
        _archIcon = ImageVector.Builder(
            name = "ArchIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Path 1: Main Arch Linux Left & Center Body
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12.0f, 3.0f)
                lineTo(3.5f, 21.0f)
                lineTo(7.5f, 21.0f)
                curveTo(8.6f, 18.6f, 9.8f, 16.3f, 11.0f, 14.0f)
                curveTo(10.0f, 13.0f, 8.7f, 12.3f, 7.3f, 11.9f)
                curveTo(9.5f, 11.4f, 11.3f, 11.8f, 12.8f, 12.8f)
                curveTo(11.7f, 11.2f, 10.3f, 9.9f, 8.8f, 8.6f)
                curveTo(10.2f, 6.1f, 11.2f, 4.2f, 12.0f, 3.0f)
                close()
            }

            // Path 2: Right Wing & Swoosh Notch
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12.0f, 3.0f)
                lineTo(20.5f, 21.0f)
                lineTo(16.5f, 21.0f)
                curveTo(15.6f, 19.3f, 14.6f, 17.5f, 13.7f, 15.7f)
                curveTo(15.3f, 15.3f, 17.0f, 15.5f, 18.7f, 16.4f)
                curveTo(17.1f, 15.0f, 15.3f, 14.2f, 13.4f, 13.9f)
                curveTo(14.7f, 11.5f, 16.0f, 9.1f, 17.2f, 6.6f)
                curveTo(15.7f, 8.0f, 14.2f, 9.5f, 12.8f, 11.0f)
                curveTo(14.1f, 9.6f, 15.3f, 8.0f, 16.4f, 6.3f)
                curveTo(15.0f, 4.7f, 13.5f, 3.5f, 12.0f, 3.0f)
                close()
            }
        }.build()
        return _archIcon!!
    }

private var _archIcon: ImageVector? = null
