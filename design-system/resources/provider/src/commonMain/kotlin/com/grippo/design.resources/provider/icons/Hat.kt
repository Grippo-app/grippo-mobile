package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Hat: ImageVector
    get() {
        if (_Hat != null) {
            return _Hat!!
        }
        _Hat = ImageVector.Builder(
            name = "Hat",
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
                moveTo(7f, 17f)
                horizontalLineTo(2f)
                moveTo(7f, 17f)
                verticalLineTo(15f)
                curveTo(7f, 11.134f, 10.134f, 8f, 14f, 8f)
                curveTo(17.866f, 8f, 21f, 11.134f, 21f, 15f)
                verticalLineTo(17f)
                horizontalLineTo(7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 6.01f)
                lineTo(14.01f, 5.999f)
            }
        }.build()

        return _Hat!!
    }

@Suppress("ObjectPropertyName")
private var _Hat: ImageVector? = null
