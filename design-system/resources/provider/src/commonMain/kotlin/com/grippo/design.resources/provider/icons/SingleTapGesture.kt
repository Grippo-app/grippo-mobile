package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.SingleTapGesture: ImageVector
    get() {
        if (_SingleTapGesture != null) {
            return _SingleTapGesture!!
        }
        _SingleTapGesture = ImageVector.Builder(
            name = "SingleTapGesture",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 20.5f)
                curveTo(15.866f, 20.5f, 19f, 17.366f, 19f, 13.5f)
                curveTo(19f, 9.634f, 15.866f, 6.5f, 12f, 6.5f)
                curveTo(8.134f, 6.5f, 5f, 9.634f, 5f, 13.5f)
                curveTo(5f, 17.366f, 8.134f, 20.5f, 12f, 20.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 7.29f)
                curveTo(5.496f, 5.039f, 8.517f, 3.5f, 12f, 3.5f)
                curveTo(15.483f, 3.5f, 18.504f, 5.039f, 20f, 7.29f)
            }
        }.build()

        return _SingleTapGesture!!
    }

@Suppress("ObjectPropertyName")
private var _SingleTapGesture: ImageVector? = null
