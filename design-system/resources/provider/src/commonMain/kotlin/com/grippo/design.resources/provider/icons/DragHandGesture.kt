package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.DragHandGesture: ImageVector
    get() {
        if (_DragHandGesture != null) {
            return _DragHandGesture!!
        }
        _DragHandGesture = ImageVector.Builder(
            name = "DragHandGesture",
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
                moveTo(7f, 10.5f)
                lineTo(4.996f, 13.172f)
                curveTo(4.418f, 13.942f, 4.471f, 15.014f, 5.122f, 15.724f)
                lineTo(8.906f, 19.852f)
                curveTo(9.284f, 20.265f, 9.818f, 20.5f, 10.379f, 20.5f)
                curveTo(11.465f, 20.5f, 13.241f, 20.5f, 15f, 20.5f)
                curveTo(17.4f, 20.5f, 19f, 19f, 19f, 16.5f)
                curveTo(19f, 16.5f, 19f, 9.643f, 19f, 7.929f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 8.5f)
                curveTo(16f, 8.5f, 16f, 8.375f, 16f, 7.928f)
                curveTo(16f, 5.643f, 19f, 5.643f, 19f, 7.928f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 8.5f)
                curveTo(16f, 8.5f, 16f, 8.375f, 16f, 7.929f)
                curveTo(16f, 7.706f, 16f, 7.25f, 16f, 7.027f)
                curveTo(16f, 4.742f, 13f, 4.742f, 13f, 7.027f)
                moveTo(13f, 8.5f)
                curveTo(13f, 8.5f, 13f, 7.92f, 13f, 7.027f)
                verticalLineTo(8.5f)
                close()
                moveTo(13f, 6.5f)
                curveTo(13f, 6.5f, 13f, 6.804f, 13f, 7.027f)
                verticalLineTo(6.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 8.5f)
                curveTo(13f, 8.5f, 13f, 7.92f, 13f, 7.027f)
                curveTo(13f, 4.742f, 16f, 4.742f, 16f, 7.027f)
                curveTo(16f, 7.25f, 16f, 7.706f, 16f, 7.929f)
                curveTo(16f, 8.375f, 16f, 8.5f, 16f, 8.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 8.5f)
                curveTo(10f, 8.5f, 10f, 7.857f, 10f, 6.5f)
                curveTo(10f, 4.214f, 13f, 4.214f, 13f, 6.5f)
                curveTo(13f, 6.5f, 13f, 6.804f, 13f, 7.027f)
                curveTo(13f, 7.92f, 13f, 8.5f, 13f, 8.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 13.5f)
                verticalLineTo(6.5f)
                curveTo(7f, 5.672f, 7.672f, 5f, 8.5f, 5f)
                curveTo(9.328f, 5f, 10f, 5.555f, 10f, 6.384f)
                curveTo(10f, 6.421f, 10f, 6.46f, 10f, 6.5f)
                curveTo(10f, 7.857f, 10f, 8.5f, 10f, 8.5f)
            }
        }.build()

        return _DragHandGesture!!
    }

@Suppress("ObjectPropertyName")
private var _DragHandGesture: ImageVector? = null
