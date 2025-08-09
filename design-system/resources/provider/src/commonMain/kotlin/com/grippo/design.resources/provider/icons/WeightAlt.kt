package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.WeightAlt: ImageVector
    get() {
        if (_WeightAlt != null) {
            return _WeightAlt!!
        }
        _WeightAlt = ImageVector.Builder(
            name = "WeightAlt",
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
                moveTo(16.5f, 5f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 5f, 21f, 5.269f, 21f, 5.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(5.6f)
                curveTo(3f, 5.269f, 3.269f, 5f, 3.6f, 5f)
                horizontalLineTo(7.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.278f, 6.329f)
                lineTo(16.484f, 5.099f)
                curveTo(16.494f, 5.033f, 16.494f, 4.967f, 16.484f, 4.901f)
                lineTo(16.278f, 3.671f)
                curveTo(16.118f, 2.707f, 15.283f, 2f, 14.306f, 2f)
                horizontalLineTo(9.694f)
                curveTo(8.717f, 2f, 7.882f, 2.707f, 7.721f, 3.671f)
                lineTo(7.516f, 4.901f)
                curveTo(7.506f, 4.967f, 7.506f, 5.033f, 7.516f, 5.099f)
                lineTo(7.721f, 6.329f)
                curveTo(7.882f, 7.293f, 8.717f, 8f, 9.694f, 8f)
                horizontalLineTo(14.306f)
                curveTo(15.283f, 8f, 16.118f, 7.293f, 16.278f, 6.329f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 8f)
                lineTo(11f, 5.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 17f)
                horizontalLineTo(17f)
            }
        }.build()

        return _WeightAlt!!
    }

@Suppress("ObjectPropertyName")
private var _WeightAlt: ImageVector? = null
