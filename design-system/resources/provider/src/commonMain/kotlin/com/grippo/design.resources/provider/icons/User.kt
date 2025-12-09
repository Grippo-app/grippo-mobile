package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.User: ImageVector
    get() {
        if (_User != null) {
            return _User!!
        }
        _User = ImageVector.Builder(
            name = "User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 7f)
                moveToRelative(-4f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.338f, 18.321f)
                curveTo(5.999f, 15.527f, 8.772f, 14f, 11.643f, 14f)
                horizontalLineTo(12.357f)
                curveTo(15.228f, 14f, 18.001f, 15.527f, 18.662f, 18.321f)
                curveTo(18.79f, 18.861f, 18.892f, 19.427f, 18.949f, 20.002f)
                curveTo(19.004f, 20.551f, 18.552f, 21f, 18f, 21f)
                horizontalLineTo(6f)
                curveTo(5.448f, 21f, 4.996f, 20.551f, 5.051f, 20.002f)
                curveTo(5.108f, 19.427f, 5.21f, 18.861f, 5.338f, 18.321f)
                close()
            }
        }.build()

        return _User!!
    }

@Suppress("ObjectPropertyName")
private var _User: ImageVector? = null

