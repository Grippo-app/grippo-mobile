package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ArrowArchery: ImageVector
    get() {
        if (_ArrowArchery != null) {
            return _ArrowArchery!!
        }
        _ArrowArchery = ImageVector.Builder(
            name = "ArrowArchery",
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
                moveTo(20.632f, 3.868f)
                verticalLineTo(6.697f)
                moveTo(8.611f, 15.889f)
                lineTo(20.632f, 3.868f)
                lineTo(8.611f, 15.889f)
                close()
                moveTo(8.611f, 15.889f)
                horizontalLineTo(5.783f)
                lineTo(2.954f, 18.718f)
                horizontalLineTo(5.783f)
                verticalLineTo(21.546f)
                lineTo(8.611f, 18.718f)
                verticalLineTo(15.889f)
                close()
                moveTo(20.632f, 3.868f)
                horizontalLineTo(17.803f)
                horizontalLineTo(20.632f)
                close()
            }
        }.build()

        return _ArrowArchery!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowArchery: ImageVector? = null
