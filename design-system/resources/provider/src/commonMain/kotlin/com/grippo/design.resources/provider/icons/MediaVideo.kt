package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MediaVideo: ImageVector
    get() {
        if (_MediaVideo != null) {
            return _MediaVideo!!
        }
        _MediaVideo = ImageVector.Builder(
            name = "MediaVideo",
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
                moveTo(21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.898f, 8.513f)
                curveTo(9.498f, 8.284f, 9f, 8.573f, 9f, 9.034f)
                verticalLineTo(14.966f)
                curveTo(9f, 15.427f, 9.498f, 15.716f, 9.898f, 15.487f)
                lineTo(15.088f, 12.521f)
                curveTo(15.491f, 12.291f, 15.491f, 11.71f, 15.088f, 11.479f)
                lineTo(9.898f, 8.513f)
                close()
            }
        }.build()

        return _MediaVideo!!
    }

@Suppress("ObjectPropertyName")
private var _MediaVideo: ImageVector? = null
