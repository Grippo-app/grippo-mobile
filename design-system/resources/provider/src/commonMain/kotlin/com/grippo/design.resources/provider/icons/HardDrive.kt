package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.HardDrive: ImageVector
    get() {
        if (_HardDrive != null) {
            return _HardDrive!!
        }
        _HardDrive = ImageVector.Builder(
            name = "HardDrive",
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
                moveTo(10f, 17.01f)
                lineTo(10.01f, 16.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 17.01f)
                lineTo(6.01f, 16.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 13f)
                lineTo(4.872f, 3.428f)
                curveTo(4.948f, 3.174f, 5.181f, 3f, 5.446f, 3f)
                horizontalLineTo(18.554f)
                curveTo(18.819f, 3f, 19.052f, 3.174f, 19.128f, 3.428f)
                lineTo(22f, 13f)
                moveTo(2f, 13f)
                verticalLineTo(20.4f)
                curveTo(2f, 20.731f, 2.269f, 21f, 2.6f, 21f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 21f, 22f, 20.731f, 22f, 20.4f)
                verticalLineTo(13f)
                horizontalLineTo(2f)
                close()
                moveTo(2f, 13f)
                horizontalLineTo(22f)
                horizontalLineTo(2f)
                close()
            }
        }.build()

        return _HardDrive!!
    }

@Suppress("ObjectPropertyName")
private var _HardDrive: ImageVector? = null
