package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.NumberedListRight: ImageVector
    get() {
        if (_NumberedListRight != null) {
            return _NumberedListRight!!
        }
        _NumberedListRight = ImageVector.Builder(
            name = "NumberedListRight",
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
                moveTo(4f, 6f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 8f)
                verticalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 14f)
                horizontalLineTo(18.6f)
                curveTo(18.269f, 14f, 18f, 13.731f, 18f, 13.4f)
                verticalLineTo(12.6f)
                curveTo(18f, 12.269f, 18.269f, 12f, 18.6f, 12f)
                horizontalLineTo(19.4f)
                curveTo(19.731f, 12f, 20f, 11.731f, 20f, 11.4f)
                verticalLineTo(10.6f)
                curveTo(20f, 10.269f, 19.731f, 10f, 19.4f, 10f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 16f)
                horizontalLineTo(19.4f)
                curveTo(19.731f, 16f, 20f, 16.269f, 20f, 16.6f)
                verticalLineTo(19.4f)
                curveTo(20f, 19.731f, 19.731f, 20f, 19.4f, 20f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 18f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 12f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 18f)
                horizontalLineTo(15f)
            }
        }.build()

        return _NumberedListRight!!
    }

@Suppress("ObjectPropertyName")
private var _NumberedListRight: ImageVector? = null
