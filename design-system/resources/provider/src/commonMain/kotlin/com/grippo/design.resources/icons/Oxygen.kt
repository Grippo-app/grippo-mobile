package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Oxygen: ImageVector
    get() {
        if (_Oxygen != null) {
            return _Oxygen!!
        }
        _Oxygen = ImageVector.Builder(
            name = "Oxygen",
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
                moveTo(21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.2f, 8f)
                horizontalLineTo(11.8f)
                curveTo(10.806f, 8f, 10f, 8.806f, 10f, 9.8f)
                verticalLineTo(14.2f)
                curveTo(10f, 15.194f, 10.806f, 16f, 11.8f, 16f)
                horizontalLineTo(12.2f)
                curveTo(13.194f, 16f, 14f, 15.194f, 14f, 14.2f)
                verticalLineTo(9.8f)
                curveTo(14f, 8.806f, 13.194f, 8f, 12.2f, 8f)
                close()
            }
        }.build()

        return _Oxygen!!
    }

@Suppress("ObjectPropertyName")
private var _Oxygen: ImageVector? = null
