package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LineUp: ImageVector
    get() {
        if (_LineUp != null) {
            return _LineUp!!
        }
        _LineUp = ImageVector.Builder(
            name = "LineUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // L-axis: vertical (left) + horizontal (bottom).
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(4f, 3f)
                curveTo(4.552f, 3f, 5f, 3.448f, 5f, 4f)
                verticalLineTo(20f)
                horizontalLineTo(21f)
                curveTo(21.552f, 20f, 22f, 20.448f, 22f, 21f)
                curveTo(22f, 21.552f, 21.552f, 22f, 21f, 22f)
                horizontalLineTo(4f)
                curveTo(3.448f, 22f, 3f, 21.552f, 3f, 21f)
                verticalLineTo(4f)
                curveTo(3f, 3.448f, 3.448f, 3f, 4f, 3f)
                close()
            }
            // Trend polyline as rounded stroke — endpoint at the inner corner of the arrow.
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 17f)
                lineTo(11f, 13f)
                lineTo(14f, 15f)
                lineTo(19f, 7f)
            }
            // Arrow head — L-bracket pointing up-right; inner corner at (19, 7) so the
            // trend stroke visually flows into it.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(15.5f, 5f)
                horizontalLineTo(20f)
                curveTo(20.552f, 5f, 21f, 5.448f, 21f, 6f)
                verticalLineTo(10.5f)
                curveTo(21f, 11.052f, 20.552f, 11.5f, 20f, 11.5f)
                curveTo(19.448f, 11.5f, 19f, 11.052f, 19f, 10.5f)
                verticalLineTo(7f)
                horizontalLineTo(15.5f)
                curveTo(14.948f, 7f, 14.5f, 6.552f, 14.5f, 6f)
                curveTo(14.5f, 5.448f, 14.948f, 5f, 15.5f, 5f)
                close()
            }
        }.build()

        return _LineUp!!
    }

@Suppress("ObjectPropertyName")
private var _LineUp: ImageVector? = null
