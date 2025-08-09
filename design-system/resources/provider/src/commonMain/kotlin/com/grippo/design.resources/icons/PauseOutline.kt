package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PauseOutline: ImageVector
    get() {
        if (_PauseOutline != null) {
            return _PauseOutline!!
        }
        _PauseOutline = ImageVector.Builder(
            name = "PauseOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(6f, 18.4f)
                verticalLineTo(5.6f)
                curveTo(6f, 5.269f, 6.269f, 5f, 6.6f, 5f)
                horizontalLineTo(9.4f)
                curveTo(9.731f, 5f, 10f, 5.269f, 10f, 5.6f)
                verticalLineTo(18.4f)
                curveTo(10f, 18.731f, 9.731f, 19f, 9.4f, 19f)
                horizontalLineTo(6.6f)
                curveTo(6.269f, 19f, 6f, 18.731f, 6f, 18.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(14f, 18.4f)
                verticalLineTo(5.6f)
                curveTo(14f, 5.269f, 14.269f, 5f, 14.6f, 5f)
                horizontalLineTo(17.4f)
                curveTo(17.731f, 5f, 18f, 5.269f, 18f, 5.6f)
                verticalLineTo(18.4f)
                curveTo(18f, 18.731f, 17.731f, 19f, 17.4f, 19f)
                horizontalLineTo(14.6f)
                curveTo(14.269f, 19f, 14f, 18.731f, 14f, 18.4f)
                close()
            }
        }.build()

        return _PauseOutline!!
    }

@Suppress("ObjectPropertyName")
private var _PauseOutline: ImageVector? = null
