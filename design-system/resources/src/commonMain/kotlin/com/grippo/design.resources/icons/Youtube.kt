package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Youtube: ImageVector
    get() {
        if (_Youtube != null) {
            return _Youtube!!
        }
        _Youtube = ImageVector.Builder(
            name = "Youtube",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 12f)
                lineTo(10.5f, 14f)
                verticalLineTo(10f)
                lineTo(14f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 12.708f)
                verticalLineTo(11.292f)
                curveTo(2f, 8.397f, 2f, 6.949f, 2.905f, 6.018f)
                curveTo(3.811f, 5.086f, 5.237f, 5.046f, 8.088f, 4.965f)
                curveTo(9.439f, 4.927f, 10.819f, 4.9f, 12f, 4.9f)
                curveTo(13.181f, 4.9f, 14.561f, 4.927f, 15.912f, 4.965f)
                curveTo(18.763f, 5.046f, 20.189f, 5.086f, 21.094f, 6.018f)
                curveTo(22f, 6.949f, 22f, 8.397f, 22f, 11.292f)
                verticalLineTo(12.708f)
                curveTo(22f, 15.603f, 22f, 17.051f, 21.094f, 17.982f)
                curveTo(20.189f, 18.913f, 18.764f, 18.954f, 15.912f, 19.034f)
                curveTo(14.561f, 19.073f, 13.181f, 19.1f, 12f, 19.1f)
                curveTo(10.819f, 19.1f, 9.439f, 19.073f, 8.088f, 19.034f)
                curveTo(5.237f, 18.954f, 3.811f, 18.913f, 2.905f, 17.982f)
                curveTo(2f, 17.051f, 2f, 15.603f, 2f, 12.708f)
                close()
            }
        }.build()

        return _Youtube!!
    }

@Suppress("ObjectPropertyName")
private var _Youtube: ImageVector? = null
