package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ArrowDown: ImageVector
    get() {
        if (_ArrowDown != null) {
            return _ArrowDown!!
        }
        _ArrowDown = ImageVector.Builder(
            name = "ArrowDown",
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
                moveTo(12.25f, 18f)
                lineTo(18.25f, 12f)
                moveTo(12.25f, 5.5f)
                verticalLineTo(18f)
                verticalLineTo(5.5f)
                close()
                moveTo(12.25f, 18f)
                lineTo(6.25f, 12f)
                lineTo(12.25f, 18f)
                close()
            }
        }.build()

        return _ArrowDown!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowDown: ImageVector? = null
