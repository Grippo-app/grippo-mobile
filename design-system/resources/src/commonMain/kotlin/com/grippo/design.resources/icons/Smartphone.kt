package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Smartphone: ImageVector
    get() {
        if (_Smartphone != null) {
            return _Smartphone!!
        }
        _Smartphone = ImageVector.Builder(
            name = "Smartphone",
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
                moveTo(17f, 2f)
                horizontalLineTo(7f)
                curveTo(5.895f, 2f, 5f, 2.895f, 5f, 4f)
                verticalLineTo(20f)
                curveTo(5f, 21.105f, 5.895f, 22f, 7f, 22f)
                horizontalLineTo(17f)
                curveTo(18.105f, 22f, 19f, 21.105f, 19f, 20f)
                verticalLineTo(4f)
                curveTo(19f, 2.895f, 18.105f, 2f, 17f, 2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18f)
                horizontalLineTo(12.01f)
            }
        }.build()

        return _Smartphone!!
    }

@Suppress("ObjectPropertyName")
private var _Smartphone: ImageVector? = null
