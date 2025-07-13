package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.`1StMedal`: ImageVector
    get() {
        if (_1StMedal != null) {
            return _1StMedal!!
        }
        _1StMedal = ImageVector.Builder(
            name = "1StMedal",
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
                moveTo(14.272f, 10.445f)
                lineTo(18f, 2f)
                lineTo(14.272f, 10.445f)
                close()
                moveTo(9.316f, 10.632f)
                lineTo(5f, 2f)
                lineTo(9.316f, 10.632f)
                close()
                moveTo(12.762f, 10.048f)
                lineTo(8.835f, 2f)
                lineTo(12.762f, 10.048f)
                close()
                moveTo(14.36f, 2f)
                lineTo(13.32f, 4.5f)
                lineTo(14.36f, 2f)
                close()
                moveTo(6f, 16f)
                curveTo(6f, 19.314f, 8.686f, 22f, 12f, 22f)
                curveTo(15.314f, 22f, 18f, 19.314f, 18f, 16f)
                curveTo(18f, 12.686f, 15.314f, 10f, 12f, 10f)
                curveTo(8.686f, 10f, 6f, 12.686f, 6f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 15f)
                lineTo(12.5f, 13.5f)
                verticalLineTo(18.5f)
            }
        }.build()

        return _1StMedal!!
    }

@Suppress("ObjectPropertyName")
private var _1StMedal: ImageVector? = null
