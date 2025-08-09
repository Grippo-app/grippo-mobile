package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LineSpace: ImageVector
    get() {
        if (_LineSpace != null) {
            return _LineSpace!!
        }
        _LineSpace = ImageVector.Builder(
            name = "LineSpace",
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
                moveTo(11f, 7f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 12f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 17f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 7f)
                lineTo(8f, 9f)
                moveTo(6f, 17f)
                verticalLineTo(7f)
                verticalLineTo(17f)
                close()
                moveTo(6f, 17f)
                lineTo(4f, 14.5f)
                lineTo(6f, 17f)
                close()
                moveTo(6f, 17f)
                lineTo(8f, 14.5f)
                lineTo(6f, 17f)
                close()
                moveTo(6f, 7f)
                lineTo(4f, 9f)
                lineTo(6f, 7f)
                close()
            }
        }.build()

        return _LineSpace!!
    }

@Suppress("ObjectPropertyName")
private var _LineSpace: ImageVector? = null
