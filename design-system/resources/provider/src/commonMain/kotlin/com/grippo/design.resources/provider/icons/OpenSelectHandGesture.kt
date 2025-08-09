package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.OpenSelectHandGesture: ImageVector
    get() {
        if (_OpenSelectHandGesture != null) {
            return _OpenSelectHandGesture!!
        }
        _OpenSelectHandGesture = ImageVector.Builder(
            name = "OpenSelectHandGesture",
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
                moveTo(8f, 14.571f)
                lineTo(6.177f, 12.835f)
                curveTo(5.535f, 12.224f, 4.513f, 12.271f, 3.93f, 12.938f)
                curveTo(3.402f, 13.541f, 3.418f, 14.445f, 3.965f, 15.03f)
                lineTo(9.907f, 21.368f)
                curveTo(10.285f, 21.771f, 10.813f, 22f, 11.366f, 22f)
                curveTo(12.45f, 22f, 14.234f, 22f, 16f, 22f)
                curveTo(18.4f, 22f, 20f, 20f, 20f, 18f)
                curveTo(20f, 18f, 20f, 11.143f, 20f, 9.429f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 10f)
                curveTo(17f, 10f, 17f, 9.875f, 17f, 9.428f)
                curveTo(17f, 7.143f, 20f, 7.143f, 20f, 9.428f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 10f)
                curveTo(14f, 10f, 14f, 9.178f, 14f, 8.286f)
                curveTo(14f, 6f, 17f, 6f, 17f, 8.286f)
                curveTo(17f, 8.509f, 17f, 9.205f, 17f, 9.428f)
                curveTo(17f, 9.875f, 17f, 10f, 17f, 10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 10f)
                curveTo(11f, 10f, 11f, 8.616f, 11f, 7.5f)
                curveTo(11f, 5.214f, 14f, 5.214f, 14f, 7.5f)
                curveTo(14f, 7.5f, 14f, 8.063f, 14f, 8.286f)
                curveTo(14f, 9.179f, 14f, 10f, 14f, 10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 14.571f)
                verticalLineTo(3.5f)
                curveTo(8f, 2.672f, 8.672f, 2f, 9.5f, 2f)
                curveTo(10.328f, 2f, 11f, 2.671f, 11f, 3.499f)
                curveTo(11f, 4.69f, 11f, 6.342f, 11f, 7.5f)
                curveTo(11f, 8.616f, 11f, 10f, 11f, 10f)
            }
        }.build()

        return _OpenSelectHandGesture!!
    }

@Suppress("ObjectPropertyName")
private var _OpenSelectHandGesture: ImageVector? = null
