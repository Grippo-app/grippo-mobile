package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.InputField: ImageVector
    get() {
        if (_InputField != null) {
            return _InputField!!
        }
        _InputField = ImageVector.Builder(
            name = "InputField",
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
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                curveTo(21.105f, 6f, 22f, 6.895f, 22f, 8f)
                verticalLineTo(16f)
                curveTo(22f, 17.105f, 21.105f, 18f, 20f, 18f)
                horizontalLineTo(4f)
                curveTo(2.895f, 18f, 2f, 17.105f, 2f, 16f)
                verticalLineTo(8f)
                curveTo(2f, 6.895f, 2.895f, 6f, 4f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 15.5f)
                horizontalLineTo(8f)
                moveTo(5f, 8.5f)
                horizontalLineTo(6.5f)
                horizontalLineTo(5f)
                close()
                moveTo(8f, 8.5f)
                horizontalLineTo(6.5f)
                horizontalLineTo(8f)
                close()
                moveTo(6.5f, 8.5f)
                verticalLineTo(15.5f)
                verticalLineTo(8.5f)
                close()
                moveTo(6.5f, 15.5f)
                horizontalLineTo(5f)
                horizontalLineTo(6.5f)
                close()
            }
        }.build()

        return _InputField!!
    }

@Suppress("ObjectPropertyName")
private var _InputField: ImageVector? = null
