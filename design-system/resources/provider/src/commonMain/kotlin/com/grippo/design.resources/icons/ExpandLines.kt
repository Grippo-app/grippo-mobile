package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ExpandLines: ImageVector
    get() {
        if (_ExpandLines != null) {
            return _ExpandLines!!
        }
        _ExpandLines = ImageVector.Builder(
            name = "ExpandLines",
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
                moveTo(12f, 19f)
                lineTo(9f, 16f)
                moveTo(12f, 14f)
                verticalLineTo(19f)
                verticalLineTo(14f)
                close()
                moveTo(12f, 19f)
                lineTo(15f, 16f)
                lineTo(12f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                lineTo(9f, 8f)
                moveTo(12f, 10f)
                verticalLineTo(5f)
                verticalLineTo(10f)
                close()
                moveTo(12f, 5f)
                lineTo(15f, 8f)
                lineTo(12f, 5f)
                close()
            }
        }.build()

        return _ExpandLines!!
    }

@Suppress("ObjectPropertyName")
private var _ExpandLines: ImageVector? = null
