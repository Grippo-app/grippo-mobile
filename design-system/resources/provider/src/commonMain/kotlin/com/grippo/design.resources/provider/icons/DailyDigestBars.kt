package com.grippo.design.resources.provider.icons


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.DailyDigestBars: ImageVector
    get() {
        if (_DailyDigestBars != null) {
            return _DailyDigestBars!!
        }
        _DailyDigestBars = ImageVector.Builder(
            name = "DailyDigestBars",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 18f)
                lineTo(5f, 16f)

                moveTo(9f, 18f)
                lineTo(9f, 14f)

                moveTo(13f, 18f)
                lineTo(13f, 12f)

                moveTo(17f, 18f)
                lineTo(17f, 10f)

                moveTo(21f, 18f)
                lineTo(21f, 8f)
            }
        }.build()

        return _DailyDigestBars!!
    }

@Suppress("ObjectPropertyName")
private var _DailyDigestBars: ImageVector? = null