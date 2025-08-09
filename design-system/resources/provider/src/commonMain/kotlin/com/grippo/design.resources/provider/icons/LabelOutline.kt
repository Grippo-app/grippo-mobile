package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LabelOutline: ImageVector
    get() {
        if (_LabelOutline != null) {
            return _LabelOutline!!
        }
        _LabelOutline = ImageVector.Builder(
            name = "LabelOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 17.4f)
                verticalLineTo(6.6f)
                curveTo(3f, 6.269f, 3.269f, 6f, 3.6f, 6f)
                horizontalLineTo(16.679f)
                curveTo(16.879f, 6f, 17.067f, 6.1f, 17.178f, 6.267f)
                lineTo(20.778f, 11.667f)
                curveTo(20.913f, 11.869f, 20.913f, 12.131f, 20.778f, 12.333f)
                lineTo(17.178f, 17.733f)
                curveTo(17.067f, 17.9f, 16.879f, 18f, 16.679f, 18f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 18f, 3f, 17.731f, 3f, 17.4f)
                close()
            }
        }.build()

        return _LabelOutline!!
    }

@Suppress("ObjectPropertyName")
private var _LabelOutline: ImageVector? = null
