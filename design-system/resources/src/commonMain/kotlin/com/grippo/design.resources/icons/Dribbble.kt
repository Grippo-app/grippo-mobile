package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Dribbble: ImageVector
    get() {
        if (_Dribbble != null) {
            return _Dribbble!!
        }
        _Dribbble = ImageVector.Builder(
            name = "Dribbble",
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
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.75f, 12.84f)
                curveTo(18.25f, 11.91f, 15.12f, 12.02f, 12.81f, 12.84f)
                curveTo(10.23f, 13.76f, 7.8f, 15.7f, 5.37f, 19.16f)
                moveTo(8.56f, 2.75f)
                curveTo(12.93f, 8.78f, 14.58f, 12.17f, 16.59f, 20.47f)
                lineTo(8.56f, 2.75f)
                close()
                moveTo(19.13f, 5.09f)
                curveTo(15.41f, 9.44f, 10.19f, 10.75f, 2.25f, 10.94f)
                lineTo(19.13f, 5.09f)
                close()
            }
        }.build()

        return _Dribbble!!
    }

@Suppress("ObjectPropertyName")
private var _Dribbble: ImageVector? = null
