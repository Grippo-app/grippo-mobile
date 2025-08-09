package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LogDenied: ImageVector
    get() {
        if (_LogDenied != null) {
            return _LogDenied!!
        }
        _LogDenied = ImageVector.Builder(
            name = "LogDenied",
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
                moveTo(17.857f, 9.2f)
                lineTo(12.143f, 14.8f)
                moveTo(17.857f, 9.2f)
                curveTo(17.131f, 8.459f, 16.119f, 8f, 15f, 8f)
                curveTo(12.791f, 8f, 11f, 9.791f, 11f, 12f)
                curveTo(11f, 13.09f, 11.436f, 14.078f, 12.143f, 14.8f)
                lineTo(17.857f, 9.2f)
                close()
                moveTo(17.857f, 9.2f)
                curveTo(18.564f, 9.922f, 19f, 10.91f, 19f, 12f)
                curveTo(19f, 14.209f, 17.209f, 16f, 15f, 16f)
                curveTo(13.881f, 16f, 12.869f, 15.54f, 12.143f, 14.8f)
                lineTo(17.857f, 9.2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 6f)
                verticalLineTo(5f)
                curveTo(19f, 3.895f, 18.105f, 3f, 17f, 3f)
                horizontalLineTo(7f)
                curveTo(5.895f, 3f, 5f, 3.895f, 5f, 5f)
                verticalLineTo(19f)
                curveTo(5f, 20.105f, 5.895f, 21f, 7f, 21f)
                horizontalLineTo(17f)
                curveTo(18.105f, 21f, 19f, 20.105f, 19f, 19f)
                verticalLineTo(18f)
            }
        }.build()

        return _LogDenied!!
    }

@Suppress("ObjectPropertyName")
private var _LogDenied: ImageVector? = null
