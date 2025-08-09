package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.`2X2Cell`: ImageVector
    get() {
        if (_2X2Cell != null) {
            return _2X2Cell!!
        }
        _2X2Cell = ImageVector.Builder(
            name = "2X2Cell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(21f, 3.6f)
                verticalLineTo(12f)
                horizontalLineTo(12f)
                verticalLineTo(3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(21f, 20.4f)
                verticalLineTo(12f)
                horizontalLineTo(12f)
                verticalLineTo(21f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 21f, 21f, 20.731f, 21f, 20.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 12f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(12f)
                verticalLineTo(12f)
                horizontalLineTo(3f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 12f)
                verticalLineTo(20.4f)
                curveTo(3f, 20.731f, 3.269f, 21f, 3.6f, 21f)
                horizontalLineTo(12f)
                verticalLineTo(12f)
                horizontalLineTo(3f)
                close()
            }
        }.build()

        return _2X2Cell!!
    }

@Suppress("ObjectPropertyName")
private var _2X2Cell: ImageVector? = null
