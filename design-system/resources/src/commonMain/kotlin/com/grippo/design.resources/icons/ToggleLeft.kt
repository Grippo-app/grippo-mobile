package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ToggleLeft: ImageVector
    get() {
        if (_ToggleLeft != null) {
            return _ToggleLeft!!
        }
        _ToggleLeft = ImageVector.Builder(
            name = "ToggleLeft",
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
                moveTo(8f, 15f)
                curveTo(9.657f, 15f, 11f, 13.657f, 11f, 12f)
                curveTo(11f, 10.343f, 9.657f, 9f, 8f, 9f)
                curveTo(6.343f, 9f, 5f, 10.343f, 5f, 12f)
                curveTo(5f, 13.657f, 6.343f, 15f, 8f, 15f)
                close()
            }
        }.build()

        return _ToggleLeft!!
    }

@Suppress("ObjectPropertyName")
private var _ToggleLeft: ImageVector? = null
