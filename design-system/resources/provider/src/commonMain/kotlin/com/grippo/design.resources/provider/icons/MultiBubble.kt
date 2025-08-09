package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MultiBubble: ImageVector
    get() {
        if (_MultiBubble != null) {
            return _MultiBubble!!
        }
        _MultiBubble = ImageVector.Builder(
            name = "MultiBubble",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.5f, 22f)
                curveTo(10.538f, 22f, 13f, 19.538f, 13f, 16.5f)
                curveTo(13f, 13.462f, 10.538f, 11f, 7.5f, 11f)
                curveTo(4.462f, 11f, 2f, 13.462f, 2f, 16.5f)
                curveTo(2f, 17.502f, 2.268f, 18.441f, 2.736f, 19.25f)
                lineTo(2.275f, 21.725f)
                lineTo(4.75f, 21.264f)
                curveTo(5.559f, 21.732f, 6.498f, 22f, 7.5f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.283f, 17.898f)
                curveTo(16.259f, 17.74f, 17.176f, 17.406f, 18f, 16.93f)
                lineTo(21.6f, 17.6f)
                lineTo(20.93f, 14f)
                curveTo(21.611f, 12.823f, 22f, 11.457f, 22f, 10f)
                curveTo(22f, 5.582f, 18.418f, 2f, 14f, 2f)
                curveTo(9.973f, 2f, 6.64f, 4.976f, 6.082f, 8.849f)
            }
        }.build()

        return _MultiBubble!!
    }

@Suppress("ObjectPropertyName")
private var _MultiBubble: ImageVector? = null
