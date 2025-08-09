package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ViewStructureDown: ImageVector
    get() {
        if (_ViewStructureDown != null) {
            return _ViewStructureDown!!
        }
        _ViewStructureDown = ImageVector.Builder(
            name = "ViewStructureDown",
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
                verticalLineTo(14.6f)
                curveTo(3f, 14.269f, 3.269f, 14f, 3.6f, 14f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 14f, 21f, 14.269f, 21f, 14.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(14f, 9.4f)
                verticalLineTo(3.6f)
                curveTo(14f, 3.269f, 14.269f, 3f, 14.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(9.4f)
                curveTo(21f, 9.731f, 20.731f, 10f, 20.4f, 10f)
                horizontalLineTo(14.6f)
                curveTo(14.269f, 10f, 14f, 9.731f, 14f, 9.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 9.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(9.4f)
                curveTo(9.731f, 3f, 10f, 3.269f, 10f, 3.6f)
                verticalLineTo(9.4f)
                curveTo(10f, 9.731f, 9.731f, 10f, 9.4f, 10f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 10f, 3f, 9.731f, 3f, 9.4f)
                close()
            }
        }.build()

        return _ViewStructureDown!!
    }

@Suppress("ObjectPropertyName")
private var _ViewStructureDown: ImageVector? = null
