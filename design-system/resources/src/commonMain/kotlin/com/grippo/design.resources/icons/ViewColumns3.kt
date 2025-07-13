package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ViewColumns3: ImageVector
    get() {
        if (_ViewColumns3 != null) {
            return _ViewColumns3!!
        }
        _ViewColumns3 = ImageVector.Builder(
            name = "ViewColumns3",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(15f, 3f)
                verticalLineTo(21f)
                moveTo(9f, 3f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 3f, 3f, 3.269f, 3f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(3f, 20.731f, 3.269f, 21f, 3.6f, 21f)
                horizontalLineTo(9f)
                verticalLineTo(3f)
                close()
                moveTo(9f, 3f)
                verticalLineTo(21f)
                verticalLineTo(3f)
                close()
                moveTo(9f, 3f)
                horizontalLineTo(15f)
                horizontalLineTo(9f)
                close()
                moveTo(9f, 21f)
                horizontalLineTo(15f)
                horizontalLineTo(9f)
                close()
                moveTo(15f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(15f)
                verticalLineTo(3f)
                close()
            }
        }.build()

        return _ViewColumns3!!
    }

@Suppress("ObjectPropertyName")
private var _ViewColumns3: ImageVector? = null
