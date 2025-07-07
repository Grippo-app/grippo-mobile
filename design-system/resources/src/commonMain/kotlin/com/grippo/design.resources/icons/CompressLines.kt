package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.CompressLines: ImageVector
    get() {
        if (_CompressLines != null) {
            return _CompressLines!!
        }
        _CompressLines = ImageVector.Builder(
            name = "CompressLines",
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
                moveTo(18f, 2f)
                horizontalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 22f)
                horizontalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 10f)
                lineTo(9f, 7f)
                moveTo(12f, 5f)
                verticalLineTo(10f)
                verticalLineTo(5f)
                close()
                moveTo(12f, 10f)
                lineTo(15f, 7f)
                lineTo(12f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 14f)
                lineTo(9f, 17f)
                moveTo(12f, 19f)
                verticalLineTo(14f)
                verticalLineTo(19f)
                close()
                moveTo(12f, 14f)
                lineTo(15f, 17f)
                lineTo(12f, 14f)
                close()
            }
        }.build()

        return _CompressLines!!
    }

@Suppress("ObjectPropertyName")
private var _CompressLines: ImageVector? = null
