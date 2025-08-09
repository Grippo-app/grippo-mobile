package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.InputSearch: ImageVector
    get() {
        if (_InputSearch != null) {
            return _InputSearch!!
        }
        _InputSearch = ImageVector.Builder(
            name = "InputSearch",
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
                moveTo(21f, 12f)
                verticalLineTo(10f)
                curveTo(21f, 7.239f, 18.761f, 5f, 16f, 5f)
                horizontalLineTo(8f)
                curveTo(5.239f, 5f, 3f, 7.239f, 3f, 10f)
                curveTo(3f, 12.761f, 5.239f, 15f, 8f, 15f)
                horizontalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.124f, 19.118f)
                lineTo(22f, 21f)
                moveTo(20.124f, 19.118f)
                curveTo(20.665f, 18.576f, 21f, 17.827f, 21f, 17f)
                curveTo(21f, 15.343f, 19.657f, 14f, 18f, 14f)
                curveTo(16.343f, 14f, 15f, 15.343f, 15f, 17f)
                curveTo(15f, 18.657f, 16.343f, 20f, 18f, 20f)
                curveTo(18.83f, 20f, 19.581f, 19.663f, 20.124f, 19.118f)
                close()
            }
        }.build()

        return _InputSearch!!
    }

@Suppress("ObjectPropertyName")
private var _InputSearch: ImageVector? = null
