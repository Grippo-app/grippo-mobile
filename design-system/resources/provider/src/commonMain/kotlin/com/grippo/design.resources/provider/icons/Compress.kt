package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Compress: ImageVector
    get() {
        if (_Compress != null) {
            return _Compress!!
        }
        _Compress = ImageVector.Builder(
            name = "Compress",
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
                moveTo(18f, 12f)
                horizontalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 16f)
                lineTo(9f, 19f)
                moveTo(12f, 22f)
                verticalLineTo(16f)
                verticalLineTo(22f)
                close()
                moveTo(12f, 16f)
                lineTo(15f, 19f)
                lineTo(12f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 8f)
                lineTo(9f, 5f)
                moveTo(12f, 2f)
                verticalLineTo(8f)
                verticalLineTo(2f)
                close()
                moveTo(12f, 8f)
                lineTo(15f, 5f)
                lineTo(12f, 8f)
                close()
            }
        }.build()

        return _Compress!!
    }

@Suppress("ObjectPropertyName")
private var _Compress: ImageVector? = null
