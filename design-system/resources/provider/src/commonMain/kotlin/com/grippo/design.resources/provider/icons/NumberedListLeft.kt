package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.NumberedListLeft: ImageVector
    get() {
        if (_NumberedListLeft != null) {
            return _NumberedListLeft!!
        }
        _NumberedListLeft = ImageVector.Builder(
            name = "NumberedListLeft",
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
                moveTo(9f, 6f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 8f)
                verticalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 14f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 14f, 4f, 13.731f, 4f, 13.4f)
                verticalLineTo(12.6f)
                curveTo(4f, 12.269f, 4.269f, 12f, 4.6f, 12f)
                horizontalLineTo(5.4f)
                curveTo(5.731f, 12f, 6f, 11.731f, 6f, 11.4f)
                verticalLineTo(10.6f)
                curveTo(6f, 10.269f, 5.731f, 10f, 5.4f, 10f)
                horizontalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 16f)
                horizontalLineTo(5.4f)
                curveTo(5.731f, 16f, 6f, 16.269f, 6f, 16.6f)
                verticalLineTo(19.4f)
                curveTo(6f, 19.731f, 5.731f, 20f, 5.4f, 20f)
                horizontalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 18f)
                horizontalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 12f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                horizontalLineTo(20f)
            }
        }.build()

        return _NumberedListLeft!!
    }

@Suppress("ObjectPropertyName")
private var _NumberedListLeft: ImageVector? = null
