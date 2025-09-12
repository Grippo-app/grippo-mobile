package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Year: ImageVector
    get() {
        if (_Year != null) return _Year!!
        _Year = ImageVector.Builder(
            name = "Year",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Binder rings
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 2f); verticalLineTo(6f)
                moveTo(16f, 2f); verticalLineTo(6f)
            }

            // Outer square frame
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 6f)
                horizontalLineTo(21f)
                verticalLineTo(20f)
                horizontalLineTo(3f)
                verticalLineTo(6f)
                close()
            }

            // Header separator
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 9f)
                horizontalLineTo(21f)
            }

            // Year grid (4 columns × 3 rows) inside content area
            // Inner content margins: left/right = 5..19, top/bottom = 10..19
            // Vertical dividers at x = 8.5, 12.0, 15.5 (from y = 10 to 19)
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 10f); verticalLineTo(19f)
                moveTo(12.0f, 10f); verticalLineTo(19f)
                moveTo(15.5f, 10f); verticalLineTo(19f)
            }

            // Horizontal dividers at y = 12.25, 14.5, 16.75 (from x = 5 to 19)
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12.25f); horizontalLineTo(19f)
                moveTo(5f, 14.5f); horizontalLineTo(19f)
                moveTo(5f, 16.75f); horizontalLineTo(19f)
            }
        }.build()
        return _Year!!
    }

@Suppress("ObjectPropertyName")
private var _Year: ImageVector? = null