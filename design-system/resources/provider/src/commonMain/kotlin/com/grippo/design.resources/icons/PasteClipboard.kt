package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PasteClipboard: ImageVector
    get() {
        if (_PasteClipboard != null) {
            return _PasteClipboard!!
        }
        _PasteClipboard = ImageVector.Builder(
            name = "PasteClipboard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8.5f, 4f)
                horizontalLineTo(6f)
                curveTo(4.895f, 4f, 4f, 4.895f, 4f, 6f)
                verticalLineTo(20f)
                curveTo(4f, 21.105f, 4.895f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.105f, 22f, 20f, 21.105f, 20f, 20f)
                verticalLineTo(6f)
                curveTo(20f, 4.895f, 19.105f, 4f, 18f, 4f)
                horizontalLineTo(15.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 6.4f)
                verticalLineTo(4.5f)
                curveTo(8f, 4.224f, 8.224f, 4f, 8.5f, 4f)
                curveTo(8.776f, 4f, 9.004f, 3.776f, 9.052f, 3.504f)
                curveTo(9.2f, 2.652f, 9.774f, 1f, 12f, 1f)
                curveTo(14.226f, 1f, 14.8f, 2.652f, 14.948f, 3.504f)
                curveTo(14.996f, 3.776f, 15.224f, 4f, 15.5f, 4f)
                curveTo(15.776f, 4f, 16f, 4.224f, 16f, 4.5f)
                verticalLineTo(6.4f)
                curveTo(16f, 6.731f, 15.731f, 7f, 15.4f, 7f)
                horizontalLineTo(8.6f)
                curveTo(8.269f, 7f, 8f, 6.731f, 8f, 6.4f)
                close()
            }
        }.build()

        return _PasteClipboard!!
    }

@Suppress("ObjectPropertyName")
private var _PasteClipboard: ImageVector? = null
