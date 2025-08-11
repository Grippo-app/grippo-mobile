package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.DeCompress: ImageVector
    get() {
        if (_DeCompress != null) {
            return _DeCompress!!
        }
        _DeCompress = ImageVector.Builder(
            name = "DeCompress",
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
                moveTo(12f, 22f)
                lineTo(9f, 19f)
                moveTo(12f, 16f)
                verticalLineTo(22f)
                verticalLineTo(16f)
                close()
                moveTo(12f, 22f)
                lineTo(15f, 19f)
                lineTo(12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                lineTo(9f, 5f)
                moveTo(12f, 8f)
                verticalLineTo(2f)
                verticalLineTo(8f)
                close()
                moveTo(12f, 2f)
                lineTo(15f, 5f)
                lineTo(12f, 2f)
                close()
            }
        }.build()

        return _DeCompress!!
    }

@Suppress("ObjectPropertyName")
private var _DeCompress: ImageVector? = null
