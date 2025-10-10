package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Magic: ImageVector
    get() {
        if (_Magic != null) {
            return _Magic!!
        }
        _Magic = ImageVector.Builder(
            name = "Magic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Wand shaft
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 18.5f)
                lineTo(14.5f, 9f)
            }

            // Tip sparkle (diagonal "X")
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.5f, 9f)
                lineTo(13.7f, 8.2f)
                moveTo(14.5f, 9f)
                lineTo(15.3f, 8.2f)
                moveTo(14.5f, 9f)
                lineTo(13.7f, 9.8f)
                moveTo(14.5f, 9f)
                lineTo(15.3f, 9.8f)
            }

            // Big sparkle
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 3.5f)
                lineTo(19f, 7.5f)
                moveTo(17f, 5.5f)
                lineTo(21f, 5.5f)
            }

            // Small sparkle
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 6f)
                lineTo(8f, 8f)
                moveTo(7f, 7f)
                lineTo(9f, 7f)
            }
        }.build()

        return _Magic!!
    }

@Suppress("ObjectPropertyName")
private var _Magic: ImageVector? = null
