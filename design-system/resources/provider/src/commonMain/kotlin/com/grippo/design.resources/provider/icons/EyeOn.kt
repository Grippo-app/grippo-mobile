package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EyeOn: ImageVector
    get() {
        if (_EyeOn != null) {
            return _EyeOn!!
        }
        _EyeOn = ImageVector.Builder(
            name = "EyeOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 5f)
                curveTo(6.556f, 5f, 3.531f, 9.234f, 2.456f, 11.116f)
                curveTo(2.235f, 11.502f, 2.125f, 11.696f, 2.137f, 11.984f)
                curveTo(2.149f, 12.272f, 2.279f, 12.46f, 2.538f, 12.836f)
                curveTo(3.818f, 14.693f, 7.294f, 19f, 12f, 19f)
                curveTo(16.706f, 19f, 20.182f, 14.693f, 21.462f, 12.836f)
                curveTo(21.721f, 12.46f, 21.851f, 12.272f, 21.863f, 11.984f)
                curveTo(21.875f, 11.696f, 21.765f, 11.502f, 21.545f, 11.116f)
                curveTo(20.469f, 9.234f, 17.444f, 5f, 12f, 5f)
                close()
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 12f)
                moveToRelative(-4f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
            }
        }.build()

        return _EyeOn!!
    }

@Suppress("ObjectPropertyName")
private var _EyeOn: ImageVector? = null
