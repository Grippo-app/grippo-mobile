package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Repeat: ImageVector
    get() {
        if (_Repeat != null) {
            return _Repeat!!
        }
        _Repeat = ImageVector.Builder(
            name = "Repeat",
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
                moveTo(17f, 1f)
                lineTo(21f, 5f)
                lineTo(17f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 11f)
                verticalLineTo(9f)
                curveTo(3f, 7.939f, 3.421f, 6.922f, 4.172f, 6.172f)
                curveTo(4.922f, 5.421f, 5.939f, 5f, 7f, 5f)
                horizontalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 23f)
                lineTo(3f, 19f)
                lineTo(7f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 13f)
                verticalLineTo(15f)
                curveTo(21f, 16.061f, 20.579f, 17.078f, 19.828f, 17.828f)
                curveTo(19.078f, 18.579f, 18.061f, 19f, 17f, 19f)
                horizontalLineTo(3f)
            }
        }.build()

        return _Repeat!!
    }

@Suppress("ObjectPropertyName")
private var _Repeat: ImageVector? = null
