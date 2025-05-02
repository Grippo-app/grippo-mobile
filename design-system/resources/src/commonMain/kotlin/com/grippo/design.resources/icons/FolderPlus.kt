package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.FolderPlus: ImageVector
    get() {
        if (_FolderPlus != null) {
            return _FolderPlus!!
        }
        _FolderPlus = ImageVector.Builder(
            name = "FolderPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 19f)
                curveTo(22f, 19.53f, 21.789f, 20.039f, 21.414f, 20.414f)
                curveTo(21.039f, 20.789f, 20.53f, 21f, 20f, 21f)
                horizontalLineTo(4f)
                curveTo(3.47f, 21f, 2.961f, 20.789f, 2.586f, 20.414f)
                curveTo(2.211f, 20.039f, 2f, 19.53f, 2f, 19f)
                verticalLineTo(5f)
                curveTo(2f, 4.47f, 2.211f, 3.961f, 2.586f, 3.586f)
                curveTo(2.961f, 3.211f, 3.47f, 3f, 4f, 3f)
                horizontalLineTo(9f)
                lineTo(11f, 6f)
                horizontalLineTo(20f)
                curveTo(20.53f, 6f, 21.039f, 6.211f, 21.414f, 6.586f)
                curveTo(21.789f, 6.961f, 22f, 7.47f, 22f, 8f)
                verticalLineTo(19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 11f)
                verticalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 14f)
                horizontalLineTo(15f)
            }
        }.build()

        return _FolderPlus!!
    }

@Suppress("ObjectPropertyName")
private var _FolderPlus: ImageVector? = null
