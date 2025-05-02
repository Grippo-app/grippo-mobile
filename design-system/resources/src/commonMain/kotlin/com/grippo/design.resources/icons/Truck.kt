package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Truck: ImageVector
    get() {
        if (_Truck != null) {
            return _Truck!!
        }
        _Truck = ImageVector.Builder(
            name = "Truck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 3f)
                horizontalLineTo(1f)
                verticalLineTo(16f)
                horizontalLineTo(16f)
                verticalLineTo(3f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 8f)
                horizontalLineTo(20f)
                lineTo(23f, 11f)
                verticalLineTo(16f)
                horizontalLineTo(16f)
                verticalLineTo(8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 21f)
                curveTo(6.881f, 21f, 8f, 19.881f, 8f, 18.5f)
                curveTo(8f, 17.119f, 6.881f, 16f, 5.5f, 16f)
                curveTo(4.119f, 16f, 3f, 17.119f, 3f, 18.5f)
                curveTo(3f, 19.881f, 4.119f, 21f, 5.5f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.5f, 21f)
                curveTo(19.881f, 21f, 21f, 19.881f, 21f, 18.5f)
                curveTo(21f, 17.119f, 19.881f, 16f, 18.5f, 16f)
                curveTo(17.119f, 16f, 16f, 17.119f, 16f, 18.5f)
                curveTo(16f, 19.881f, 17.119f, 21f, 18.5f, 21f)
                close()
            }
        }.build()

        return _Truck!!
    }

@Suppress("ObjectPropertyName")
private var _Truck: ImageVector? = null
