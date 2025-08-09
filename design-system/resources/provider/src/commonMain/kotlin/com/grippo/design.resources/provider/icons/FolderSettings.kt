package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.FolderSettings: ImageVector
    get() {
        if (_FolderSettings != null) {
            return _FolderSettings!!
        }
        _FolderSettings = ImageVector.Builder(
            name = "FolderSettings",
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
                moveTo(2.6f, 4f)
                horizontalLineTo(8.778f)
                curveTo(8.921f, 4f, 9.06f, 4.051f, 9.169f, 4.144f)
                lineTo(12.332f, 6.856f)
                curveTo(12.44f, 6.949f, 12.579f, 7f, 12.722f, 7f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 7f, 22f, 7.269f, 22f, 7.6f)
                verticalLineTo(10.4f)
                curveTo(22f, 10.731f, 21.731f, 11f, 21.4f, 11f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 11f, 2f, 10.731f, 2f, 10.4f)
                verticalLineTo(4.6f)
                curveTo(2f, 4.269f, 2.269f, 4f, 2.6f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 10f)
                verticalLineTo(14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 10f)
                verticalLineTo(19.4f)
                curveTo(2f, 19.731f, 2.269f, 20f, 2.6f, 20f)
                horizontalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 21f)
                curveTo(20.105f, 21f, 21f, 20.105f, 21f, 19f)
                curveTo(21f, 17.895f, 20.105f, 17f, 19f, 17f)
                curveTo(18.636f, 17f, 18.294f, 17.097f, 18f, 17.268f)
                curveTo(17.402f, 17.613f, 17f, 18.26f, 17f, 19f)
                curveTo(17f, 19.74f, 17.402f, 20.387f, 18f, 20.732f)
                curveTo(18.294f, 20.903f, 18.636f, 21f, 19f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 22f)
                curveTo(20.657f, 22f, 22f, 20.657f, 22f, 19f)
                curveTo(22f, 17.343f, 20.657f, 16f, 19f, 16f)
                curveTo(17.343f, 16f, 16f, 17.343f, 16f, 19f)
                curveTo(16f, 20.657f, 17.343f, 22f, 19f, 22f)
                close()
            }
        }.build()

        return _FolderSettings!!
    }

@Suppress("ObjectPropertyName")
private var _FolderSettings: ImageVector? = null
