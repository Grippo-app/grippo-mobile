package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Combine: ImageVector
    get() {
        if (_Combine != null) {
            return _Combine!!
        }
        _Combine = ImageVector.Builder(
            name = "Combine",
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
                moveTo(21f, 9.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(9.6f)
                curveTo(9.269f, 21f, 9f, 20.731f, 9f, 20.4f)
                verticalLineTo(9.6f)
                curveTo(9f, 9.269f, 9.269f, 9f, 9.6f, 9f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 9f, 21f, 9.269f, 21f, 9.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 3.6f)
                verticalLineTo(14.4f)
                curveTo(15f, 14.731f, 14.731f, 15f, 14.4f, 15f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 15f, 3f, 14.731f, 3f, 14.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(14.4f)
                curveTo(14.731f, 3f, 15f, 3.269f, 15f, 3.6f)
                close()
            }
        }.build()

        return _Combine!!
    }

@Suppress("ObjectPropertyName")
private var _Combine: ImageVector? = null
