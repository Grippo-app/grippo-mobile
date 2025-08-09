package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Reports: ImageVector
    get() {
        if (_Reports != null) {
            return _Reports!!
        }
        _Reports = ImageVector.Builder(
            name = "Reports",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(15f, 9f)
                horizontalLineTo(9.6f)
                curveTo(9.269f, 9f, 9f, 9.269f, 9f, 9.6f)
                verticalLineTo(16f)
                moveTo(9f, 21f)
                horizontalLineTo(15f)
                horizontalLineTo(9f)
                close()
                moveTo(9f, 21f)
                verticalLineTo(16f)
                verticalLineTo(21f)
                close()
                moveTo(9f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(16.6f)
                curveTo(3f, 16.269f, 3.269f, 16f, 3.6f, 16f)
                horizontalLineTo(9f)
                verticalLineTo(21f)
                close()
                moveTo(15f, 21f)
                verticalLineTo(9f)
                verticalLineTo(21f)
                close()
                moveTo(15f, 21f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 21f, 21f, 20.731f, 21f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(21f, 3.269f, 20.731f, 3f, 20.4f, 3f)
                horizontalLineTo(15.6f)
                curveTo(15.269f, 3f, 15f, 3.269f, 15f, 3.6f)
                verticalLineTo(9f)
                verticalLineTo(21f)
                close()
            }
        }.build()

        return _Reports!!
    }

@Suppress("ObjectPropertyName")
private var _Reports: ImageVector? = null
