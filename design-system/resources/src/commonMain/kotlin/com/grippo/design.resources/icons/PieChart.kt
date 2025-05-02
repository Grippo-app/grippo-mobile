package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PieChart: ImageVector
    get() {
        if (_PieChart != null) {
            return _PieChart!!
        }
        _PieChart = ImageVector.Builder(
            name = "PieChart",
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
                moveTo(21.21f, 15.89f)
                curveTo(20.574f, 17.394f, 19.579f, 18.72f, 18.312f, 19.751f)
                curveTo(17.045f, 20.782f, 15.545f, 21.487f, 13.943f, 21.805f)
                curveTo(12.34f, 22.122f, 10.684f, 22.042f, 9.12f, 21.572f)
                curveTo(7.556f, 21.101f, 6.131f, 20.255f, 4.969f, 19.107f)
                curveTo(3.807f, 17.958f, 2.945f, 16.543f, 2.457f, 14.984f)
                curveTo(1.968f, 13.425f, 1.87f, 11.77f, 2.169f, 10.165f)
                curveTo(2.468f, 8.559f, 3.156f, 7.051f, 4.172f, 5.772f)
                curveTo(5.189f, 4.493f, 6.503f, 3.483f, 8f, 2.83f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 12f)
                curveTo(22f, 10.687f, 21.741f, 9.386f, 21.239f, 8.173f)
                curveTo(20.736f, 6.96f, 20f, 5.858f, 19.071f, 4.929f)
                curveTo(18.142f, 4f, 17.04f, 3.264f, 15.827f, 2.761f)
                curveTo(14.614f, 2.259f, 13.313f, 2f, 12f, 2f)
                verticalLineTo(12f)
                horizontalLineTo(22f)
                close()
            }
        }.build()

        return _PieChart!!
    }

@Suppress("ObjectPropertyName")
private var _PieChart: ImageVector? = null
