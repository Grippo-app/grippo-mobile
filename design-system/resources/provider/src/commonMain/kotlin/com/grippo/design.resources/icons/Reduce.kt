package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Reduce: ImageVector
    get() {
        if (_Reduce != null) {
            return _Reduce!!
        }
        _Reduce = ImageVector.Builder(
            name = "Reduce",
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
                moveTo(9f, 15f)
                horizontalLineTo(5f)
                moveTo(4f, 20f)
                lineTo(9f, 15f)
                lineTo(4f, 20f)
                close()
                moveTo(9f, 15f)
                verticalLineTo(19f)
                verticalLineTo(15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 9f)
                horizontalLineTo(19f)
                moveTo(20f, 4f)
                lineTo(15f, 9f)
                lineTo(20f, 4f)
                close()
                moveTo(15f, 9f)
                verticalLineTo(5f)
                verticalLineTo(9f)
                close()
            }
        }.build()

        return _Reduce!!
    }

@Suppress("ObjectPropertyName")
private var _Reduce: ImageVector? = null
