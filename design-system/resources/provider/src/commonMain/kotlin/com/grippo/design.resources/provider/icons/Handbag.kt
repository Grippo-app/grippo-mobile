package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Handbag: ImageVector
    get() {
        if (_Handbag != null) {
            return _Handbag!!
        }
        _Handbag = ImageVector.Builder(
            name = "Handbag",
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
                moveTo(15f, 8f)
                verticalLineTo(14f)
                moveTo(9f, 8f)
                horizontalLineTo(4f)
                curveTo(2.895f, 8f, 2f, 8.895f, 2f, 10f)
                verticalLineTo(19f)
                curveTo(2f, 20.105f, 2.895f, 21f, 4f, 21f)
                horizontalLineTo(20f)
                curveTo(21.105f, 21f, 22f, 20.105f, 22f, 19f)
                verticalLineTo(10f)
                curveTo(22f, 8.895f, 21.105f, 8f, 20f, 8f)
                horizontalLineTo(15f)
                horizontalLineTo(9f)
                close()
                moveTo(9f, 8f)
                verticalLineTo(3.6f)
                curveTo(9f, 3.269f, 9.269f, 3f, 9.6f, 3f)
                horizontalLineTo(14.4f)
                curveTo(14.731f, 3f, 15f, 3.269f, 15f, 3.6f)
                verticalLineTo(8f)
                horizontalLineTo(9f)
                close()
                moveTo(9f, 8f)
                horizontalLineTo(15f)
                horizontalLineTo(9f)
                close()
                moveTo(9f, 8f)
                verticalLineTo(14f)
                verticalLineTo(8f)
                close()
            }
        }.build()

        return _Handbag!!
    }

@Suppress("ObjectPropertyName")
private var _Handbag: ImageVector? = null
