package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Facebook: ImageVector
    get() {
        if (_Facebook != null) {
            return _Facebook!!
        }
        _Facebook = ImageVector.Builder(
            name = "Facebook",
            defaultWidth = 38.dp,
            defaultHeight = 71.dp,
            viewportWidth = 38f,
            viewportHeight = 71f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(24.831f, 15.783f)
                verticalLineTo(25.658f)
                horizontalLineTo(37.043f)
                lineTo(35.11f, 38.96f)
                horizontalLineTo(24.831f)
                verticalLineTo(69.608f)
                curveTo(22.77f, 69.894f, 20.662f, 70.043f, 18.522f, 70.043f)
                curveTo(16.051f, 70.043f, 13.625f, 69.846f, 11.263f, 69.465f)
                verticalLineTo(38.96f)
                horizontalLineTo(0f)
                verticalLineTo(25.658f)
                horizontalLineTo(11.263f)
                verticalLineTo(13.575f)
                curveTo(11.263f, 6.079f, 17.337f, 0f, 24.834f, 0f)
                verticalLineTo(0.006f)
                curveTo(24.857f, 0.006f, 24.876f, 0f, 24.898f, 0f)
                horizontalLineTo(37.047f)
                verticalLineTo(11.505f)
                horizontalLineTo(29.108f)
                curveTo(26.749f, 11.505f, 24.834f, 13.42f, 24.834f, 15.78f)
                lineTo(24.831f, 15.783f)
                close()
            }
        }.build()

        return _Facebook!!
    }

@Suppress("ObjectPropertyName")
private var _Facebook: ImageVector? = null