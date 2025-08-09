package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.UnderlineSquareOutline: ImageVector
    get() {
        if (_UnderlineSquareOutline != null) {
            return _UnderlineSquareOutline!!
        }
        _UnderlineSquareOutline = ImageVector.Builder(
            name = "UnderlineSquareOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 6f)
                verticalLineTo(10f)
                curveTo(16f, 12.209f, 14.209f, 14f, 12f, 14f)
                curveTo(9.791f, 14f, 8f, 12.209f, 8f, 10f)
                verticalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 18f)
                horizontalLineTo(18f)
            }
        }.build()

        return _UnderlineSquareOutline!!
    }

@Suppress("ObjectPropertyName")
private var _UnderlineSquareOutline: ImageVector? = null
