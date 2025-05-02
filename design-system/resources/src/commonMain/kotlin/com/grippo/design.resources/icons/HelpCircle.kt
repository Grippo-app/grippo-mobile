package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.HelpCircle: ImageVector
    get() {
        if (_HelpCircle != null) {
            return _HelpCircle!!
        }
        _HelpCircle = ImageVector.Builder(
            name = "HelpCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.09f, 9f)
                curveTo(9.325f, 8.332f, 9.789f, 7.768f, 10.4f, 7.409f)
                curveTo(11.011f, 7.05f, 11.729f, 6.919f, 12.427f, 7.039f)
                curveTo(13.125f, 7.159f, 13.759f, 7.522f, 14.215f, 8.064f)
                curveTo(14.671f, 8.606f, 14.921f, 9.292f, 14.92f, 10f)
                curveTo(14.92f, 12f, 11.92f, 13f, 11.92f, 13f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17f)
                horizontalLineTo(12.01f)
            }
        }.build()

        return _HelpCircle!!
    }

@Suppress("ObjectPropertyName")
private var _HelpCircle: ImageVector? = null
