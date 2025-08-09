package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.InputOutput: ImageVector
    get() {
        if (_InputOutput != null) {
            return _InputOutput!!
        }
        _InputOutput = ImageVector.Builder(
            name = "InputOutput",
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
                moveTo(14f, 19f)
                curveTo(17.866f, 19f, 21f, 15.866f, 21f, 12f)
                curveTo(21f, 8.134f, 17.866f, 5f, 14f, 5f)
                curveTo(10.134f, 5f, 7f, 8.134f, 7f, 12f)
                curveTo(7f, 15.866f, 10.134f, 19f, 14f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 19f)
                verticalLineTo(5f)
            }
        }.build()

        return _InputOutput!!
    }

@Suppress("ObjectPropertyName")
private var _InputOutput: ImageVector? = null
