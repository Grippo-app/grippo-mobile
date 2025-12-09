package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Warning: ImageVector
    get() {
        if (_Warning != null) {
            return _Warning!!
        }
        _Warning = ImageVector.Builder(
            name = "Warning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 12f)
                moveToRelative(-9f, 0f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18f, 0f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, -18f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF33363F)),
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 1f
            ) {
                moveTo(12.5f, 7.5f)
                curveTo(12.5f, 7.776f, 12.276f, 8f, 12f, 8f)
                curveTo(11.724f, 8f, 11.5f, 7.776f, 11.5f, 7.5f)
                curveTo(11.5f, 7.224f, 11.724f, 7f, 12f, 7f)
                curveTo(12.276f, 7f, 12.5f, 7.224f, 12.5f, 7.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 17f)
                verticalLineTo(10f)
            }
        }.build()

        return _Warning!!
    }

@Suppress("ObjectPropertyName")
private var _Warning: ImageVector? = null
