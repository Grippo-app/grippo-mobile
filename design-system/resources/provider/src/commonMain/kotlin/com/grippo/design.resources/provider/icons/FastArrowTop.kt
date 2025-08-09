package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.FastArrowTop: ImageVector
    get() {
        if (_FastArrowTop != null) {
            return _FastArrowTop!!
        }
        _FastArrowTop = ImageVector.Builder(
            name = "FastArrowTop",
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
                moveTo(15.5f, 16.5f)
                lineTo(12f, 13f)
                lineTo(8.5f, 16.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 10.5f)
                lineTo(12f, 7f)
                lineTo(8.5f, 10.5f)
            }
        }.build()

        return _FastArrowTop!!
    }

@Suppress("ObjectPropertyName")
private var _FastArrowTop: ImageVector? = null
