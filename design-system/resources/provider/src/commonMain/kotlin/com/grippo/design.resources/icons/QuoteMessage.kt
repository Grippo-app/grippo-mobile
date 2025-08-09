package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.QuoteMessage: ImageVector
    get() {
        if (_QuoteMessage != null) {
            return _QuoteMessage!!
        }
        _QuoteMessage = ImageVector.Builder(
            name = "QuoteMessage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 20.289f)
                verticalLineTo(5f)
                curveTo(3f, 3.895f, 3.895f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                verticalLineTo(15f)
                curveTo(21f, 16.105f, 20.105f, 17f, 19f, 17f)
                horizontalLineTo(7.961f)
                curveTo(7.354f, 17f, 6.779f, 17.276f, 6.4f, 17.751f)
                lineTo(4.069f, 20.664f)
                curveTo(3.714f, 21.107f, 3f, 20.857f, 3f, 20.289f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10.5f, 10f)
                curveTo(10.5f, 11f, 9.5f, 12f, 8.5f, 13f)
                moveTo(10.5f, 10f)
                horizontalLineTo(8.5f)
                curveTo(7.948f, 10f, 7.5f, 9.552f, 7.5f, 9f)
                verticalLineTo(8f)
                curveTo(7.5f, 7.448f, 7.948f, 7f, 8.5f, 7f)
                horizontalLineTo(9.5f)
                curveTo(10.052f, 7f, 10.5f, 7.448f, 10.5f, 8f)
                verticalLineTo(10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16.5f, 10f)
                curveTo(16.5f, 11f, 15.5f, 12f, 14.5f, 13f)
                moveTo(16.5f, 10f)
                horizontalLineTo(14.5f)
                curveTo(13.948f, 10f, 13.5f, 9.552f, 13.5f, 9f)
                verticalLineTo(8f)
                curveTo(13.5f, 7.448f, 13.948f, 7f, 14.5f, 7f)
                horizontalLineTo(15.5f)
                curveTo(16.052f, 7f, 16.5f, 7.448f, 16.5f, 8f)
                verticalLineTo(10f)
                close()
            }
        }.build()

        return _QuoteMessage!!
    }

@Suppress("ObjectPropertyName")
private var _QuoteMessage: ImageVector? = null
