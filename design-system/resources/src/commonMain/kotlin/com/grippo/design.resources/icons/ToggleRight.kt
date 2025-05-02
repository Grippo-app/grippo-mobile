package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ToggleRight: ImageVector
    get() {
        if (_ToggleRight != null) {
            return _ToggleRight!!
        }
        _ToggleRight = ImageVector.Builder(
            name = "ToggleRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 5f)
                horizontalLineTo(8f)
                curveTo(4.134f, 5f, 1f, 8.134f, 1f, 12f)
                curveTo(1f, 15.866f, 4.134f, 19f, 8f, 19f)
                horizontalLineTo(16f)
                curveTo(19.866f, 19f, 23f, 15.866f, 23f, 12f)
                curveTo(23f, 8.134f, 19.866f, 5f, 16f, 5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 15f)
                curveTo(17.657f, 15f, 19f, 13.657f, 19f, 12f)
                curveTo(19f, 10.343f, 17.657f, 9f, 16f, 9f)
                curveTo(14.343f, 9f, 13f, 10.343f, 13f, 12f)
                curveTo(13f, 13.657f, 14.343f, 15f, 16f, 15f)
                close()
            }
        }.build()

        return _ToggleRight!!
    }

@Suppress("ObjectPropertyName")
private var _ToggleRight: ImageVector? = null
