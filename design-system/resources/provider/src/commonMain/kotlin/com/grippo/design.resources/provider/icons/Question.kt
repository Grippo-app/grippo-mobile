package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Question: ImageVector
    get() {
        if (_Question != null) {
            return _Question!!
        }
        _Question = ImageVector.Builder(
            name = "Question",
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
                moveTo(12f, 18f)
                moveToRelative(-0.5f, 0f)
                arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 1f, 0f)
                arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -1f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 16f)
                verticalLineTo(14.581f)
                curveTo(12f, 13.637f, 12.604f, 12.799f, 13.5f, 12.5f)
                curveTo(14.396f, 12.201f, 15f, 11.363f, 15f, 10.419f)
                verticalLineTo(9.906f)
                curveTo(15f, 8.301f, 13.699f, 7f, 12.094f, 7f)
                horizontalLineTo(12f)
                curveTo(10.343f, 7f, 9f, 8.343f, 9f, 10f)
            }
        }.build()

        return _Question!!
    }

@Suppress("ObjectPropertyName")
private var _Question: ImageVector? = null
