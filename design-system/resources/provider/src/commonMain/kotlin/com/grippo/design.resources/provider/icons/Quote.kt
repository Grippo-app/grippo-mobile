package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Quote: ImageVector
    get() {
        if (_Quote != null) {
            return _Quote!!
        }
        _Quote = ImageVector.Builder(
            name = "Quote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10f, 12f)
                curveTo(10f, 14.5f, 9f, 16f, 6f, 17.5f)
                moveTo(10f, 12f)
                horizontalLineTo(5f)
                curveTo(4.448f, 12f, 4f, 11.552f, 4f, 11f)
                verticalLineTo(7.5f)
                curveTo(4f, 6.948f, 4.448f, 6.5f, 5f, 6.5f)
                horizontalLineTo(9f)
                curveTo(9.552f, 6.5f, 10f, 6.948f, 10f, 7.5f)
                verticalLineTo(12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20f, 12f)
                curveTo(20f, 14.5f, 19f, 16f, 16f, 17.5f)
                moveTo(20f, 12f)
                horizontalLineTo(15f)
                curveTo(14.448f, 12f, 14f, 11.552f, 14f, 11f)
                verticalLineTo(7.5f)
                curveTo(14f, 6.948f, 14.448f, 6.5f, 15f, 6.5f)
                horizontalLineTo(19f)
                curveTo(19.552f, 6.5f, 20f, 6.948f, 20f, 7.5f)
                verticalLineTo(12f)
                close()
            }
        }.build()

        return _Quote!!
    }

@Suppress("ObjectPropertyName")
private var _Quote: ImageVector? = null
