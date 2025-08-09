package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Folder: ImageVector
    get() {
        if (_Folder != null) {
            return _Folder!!
        }
        _Folder = ImageVector.Builder(
            name = "Folder",
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
                moveTo(2f, 11f)
                horizontalLineTo(22f)
                moveTo(2f, 11f)
                verticalLineTo(4.6f)
                curveTo(2f, 4.269f, 2.269f, 4f, 2.6f, 4f)
                horizontalLineTo(8.778f)
                curveTo(8.921f, 4f, 9.06f, 4.051f, 9.169f, 4.144f)
                lineTo(12.332f, 6.856f)
                curveTo(12.44f, 6.949f, 12.579f, 7f, 12.722f, 7f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 7f, 22f, 7.269f, 22f, 7.6f)
                verticalLineTo(11f)
                horizontalLineTo(2f)
                close()
                moveTo(2f, 11f)
                verticalLineTo(19.4f)
                curveTo(2f, 19.731f, 2.269f, 20f, 2.6f, 20f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 20f, 22f, 19.731f, 22f, 19.4f)
                verticalLineTo(11f)
                horizontalLineTo(2f)
                close()
            }
        }.build()

        return _Folder!!
    }

@Suppress("ObjectPropertyName")
private var _Folder: ImageVector? = null
