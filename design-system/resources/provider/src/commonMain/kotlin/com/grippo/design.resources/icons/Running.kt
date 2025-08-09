package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Running: ImageVector
    get() {
        if (_Running != null) {
            return _Running!!
        }
        _Running = ImageVector.Builder(
            name = "Running",
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
                moveTo(15f, 7f)
                curveTo(16.105f, 7f, 17f, 6.105f, 17f, 5f)
                curveTo(17f, 3.895f, 16.105f, 3f, 15f, 3f)
                curveTo(13.895f, 3f, 13f, 3.895f, 13f, 5f)
                curveTo(13f, 6.105f, 13.895f, 7f, 15f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.613f, 8.267f)
                lineTo(9.305f, 12.402f)
                lineTo(13.44f, 16.537f)
                lineTo(11.373f, 21.086f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.41f, 9.507f)
                lineTo(9.797f, 6.199f)
                lineTo(12.613f, 8.267f)
                lineTo(15.508f, 11.575f)
                horizontalLineTo(19.23f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.892f, 15.71f)
                lineTo(7.651f, 16.537f)
                horizontalLineTo(4.343f)
            }
        }.build()

        return _Running!!
    }

@Suppress("ObjectPropertyName")
private var _Running: ImageVector? = null
