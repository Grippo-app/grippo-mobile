package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Axes: ImageVector
    get() {
        if (_Axes != null) {
            return _Axes!!
        }
        _Axes = ImageVector.Builder(
            name = "Axes",
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
                moveTo(12f, 12.843f)
                lineTo(3f, 19.452f)
                moveTo(21f, 19.452f)
                lineTo(12f, 12.843f)
                lineTo(21f, 19.452f)
                close()
                moveTo(12f, 12.843f)
                verticalLineTo(3f)
                verticalLineTo(12.843f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.438f, 16.71f)
                lineTo(21f, 19.452f)
                lineTo(18.188f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.75f, 5.194f)
                lineTo(12f, 3f)
                lineTo(14.25f, 5.194f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.813f, 20f)
                lineTo(3f, 19.452f)
                lineTo(3.563f, 16.71f)
            }
        }.build()

        return _Axes!!
    }

@Suppress("ObjectPropertyName")
private var _Axes: ImageVector? = null
