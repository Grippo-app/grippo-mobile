package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AugmentedReality: ImageVector
    get() {
        if (_AugmentedReality != null) {
            return _AugmentedReality!!
        }
        _AugmentedReality = ImageVector.Builder(
            name = "AugmentedReality",
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
                moveTo(6.114f, 13.782f)
                horizontalLineTo(9.886f)
                moveTo(5.5f, 15.5f)
                lineTo(6.114f, 13.782f)
                lineTo(5.5f, 15.5f)
                close()
                moveTo(10.5f, 15.5f)
                lineTo(9.886f, 13.782f)
                lineTo(10.5f, 15.5f)
                close()
                moveTo(6.114f, 13.782f)
                lineTo(8f, 8.5f)
                lineTo(9.886f, 13.782f)
                horizontalLineTo(6.114f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.857f, 12.7f)
                lineTo(18f, 15.5f)
                moveTo(13f, 15.5f)
                verticalLineTo(12.7f)
                verticalLineTo(15.5f)
                close()
                moveTo(15.857f, 12.7f)
                curveTo(16.571f, 12.7f, 18f, 12.7f, 18f, 10.6f)
                curveTo(18f, 8.5f, 16.571f, 8.5f, 15.857f, 8.5f)
                horizontalLineTo(13f)
                verticalLineTo(12.7f)
                horizontalLineTo(15.857f)
                close()
                moveTo(15.857f, 12.7f)
                curveTo(14.714f, 12.7f, 13.476f, 12.7f, 13f, 12.7f)
                horizontalLineTo(15.857f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 18.4f)
                verticalLineTo(5.6f)
                curveTo(2f, 5.269f, 2.269f, 5f, 2.6f, 5f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 5f, 22f, 5.269f, 22f, 5.6f)
                verticalLineTo(18.4f)
                curveTo(22f, 18.731f, 21.731f, 19f, 21.4f, 19f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 19f, 2f, 18.731f, 2f, 18.4f)
                close()
            }
        }.build()

        return _AugmentedReality!!
    }

@Suppress("ObjectPropertyName")
private var _AugmentedReality: ImageVector? = null
