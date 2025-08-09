package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AddMediaVideo: ImageVector
    get() {
        if (_AddMediaVideo != null) {
            return _AddMediaVideo!!
        }
        _AddMediaVideo = ImageVector.Builder(
            name = "AddMediaVideo",
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
                moveTo(13f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 19f)
                verticalLineTo(22f)
                moveTo(16f, 19f)
                horizontalLineTo(19f)
                horizontalLineTo(16f)
                close()
                moveTo(22f, 19f)
                horizontalLineTo(19f)
                horizontalLineTo(22f)
                close()
                moveTo(19f, 19f)
                verticalLineTo(16f)
                verticalLineTo(19f)
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
                curveTo(15.491f, 12.291f, 15.491f, 11.709f, 15.088f, 11.479f)
                lineTo(9.898f, 8.513f)
                close()
            }
        }.build()

        return _AddMediaVideo!!
    }

@Suppress("ObjectPropertyName")
private var _AddMediaVideo: ImageVector? = null
