package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.AppleImac2021: ImageVector
    get() {
        if (_AppleImac2021 != null) {
            return _AppleImac2021!!
        }
        _AppleImac2021 = ImageVector.Builder(
            name = "AppleImac2021",
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
                moveTo(13.5f, 22f)
                verticalLineTo(18f)
                moveTo(2f, 15.5f)
                verticalLineTo(2.6f)
                curveTo(2f, 2.269f, 2.269f, 2f, 2.6f, 2f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 2f, 22f, 2.269f, 22f, 2.6f)
                verticalLineTo(15.5f)
                horizontalLineTo(2f)
                close()
                moveTo(2f, 15.5f)
                verticalLineTo(17.4f)
                curveTo(2f, 17.731f, 2.269f, 18f, 2.6f, 18f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 18f, 22f, 17.731f, 22f, 17.4f)
                verticalLineTo(15.5f)
                horizontalLineTo(2f)
                close()
                moveTo(2f, 15.5f)
                horizontalLineTo(22f)
                horizontalLineTo(2f)
                close()
                moveTo(9f, 22f)
                horizontalLineTo(10.5f)
                horizontalLineTo(9f)
                close()
                moveTo(10.5f, 22f)
                verticalLineTo(18f)
                verticalLineTo(22f)
                close()
                moveTo(10.5f, 22f)
                horizontalLineTo(13.5f)
                horizontalLineTo(10.5f)
                close()
                moveTo(13.5f, 22f)
                horizontalLineTo(15f)
                horizontalLineTo(13.5f)
                close()
            }
        }.build()

        return _AppleImac2021!!
    }

@Suppress("ObjectPropertyName")
private var _AppleImac2021: ImageVector? = null
