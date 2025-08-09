package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Translate: ImageVector
    get() {
        if (_Translate != null) {
            return _Translate!!
        }
        _Translate = ImageVector.Builder(
            name = "Translate",
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
                moveTo(9f, 12.593f)
                lineTo(12f, 15.5f)
                moveTo(2f, 5f)
                horizontalLineTo(9f)
                horizontalLineTo(2f)
                close()
                moveTo(16f, 5f)
                horizontalLineTo(13.5f)
                horizontalLineTo(16f)
                close()
                moveTo(9f, 5f)
                horizontalLineTo(13.5f)
                horizontalLineTo(9f)
                close()
                moveTo(9f, 5f)
                verticalLineTo(3f)
                verticalLineTo(5f)
                close()
                moveTo(13.5f, 5f)
                curveTo(12.679f, 7.735f, 10.961f, 10.321f, 9f, 12.593f)
                lineTo(13.5f, 5f)
                close()
                moveTo(4f, 17.5f)
                curveTo(5.585f, 16.141f, 7.376f, 14.474f, 9f, 12.593f)
                lineTo(4f, 17.5f)
                close()
                moveTo(9f, 12.593f)
                curveTo(8f, 11.5f, 6.4f, 9.3f, 6f, 8.5f)
                lineTo(9f, 12.593f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.643f, 18f)
                horizontalLineTo(20.357f)
                moveTo(13.5f, 21f)
                lineTo(14.643f, 18f)
                lineTo(13.5f, 21f)
                close()
                moveTo(21.5f, 21f)
                lineTo(20.357f, 18f)
                lineTo(21.5f, 21f)
                close()
                moveTo(14.643f, 18f)
                lineTo(17.5f, 10.5f)
                lineTo(20.357f, 18f)
                horizontalLineTo(14.643f)
                close()
            }
        }.build()

        return _Translate!!
    }

@Suppress("ObjectPropertyName")
private var _Translate: ImageVector? = null
