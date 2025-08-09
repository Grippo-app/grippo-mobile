package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.FastArrowDown: ImageVector
    get() {
        if (_FastArrowDown != null) {
            return _FastArrowDown!!
        }
        _FastArrowDown = ImageVector.Builder(
            name = "FastArrowDown",
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
                moveTo(15.5f, 7f)
                lineTo(12f, 10.5f)
                lineTo(8.5f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 13f)
                lineTo(12f, 16.5f)
                lineTo(8.5f, 13f)
            }
        }.build()

        return _FastArrowDown!!
    }

@Suppress("ObjectPropertyName")
private var _FastArrowDown: ImageVector? = null
