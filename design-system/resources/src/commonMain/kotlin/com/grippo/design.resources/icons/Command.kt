package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Command: ImageVector
    get() {
        if (_Command != null) {
            return _Command!!
        }
        _Command = ImageVector.Builder(
            name = "Command",
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
                moveTo(18f, 3f)
                curveTo(17.204f, 3f, 16.441f, 3.316f, 15.879f, 3.879f)
                curveTo(15.316f, 4.441f, 15f, 5.204f, 15f, 6f)
                verticalLineTo(18f)
                curveTo(15f, 18.796f, 15.316f, 19.559f, 15.879f, 20.121f)
                curveTo(16.441f, 20.684f, 17.204f, 21f, 18f, 21f)
                curveTo(18.796f, 21f, 19.559f, 20.684f, 20.121f, 20.121f)
                curveTo(20.684f, 19.559f, 21f, 18.796f, 21f, 18f)
                curveTo(21f, 17.204f, 20.684f, 16.441f, 20.121f, 15.879f)
                curveTo(19.559f, 15.316f, 18.796f, 15f, 18f, 15f)
                horizontalLineTo(6f)
                curveTo(5.204f, 15f, 4.441f, 15.316f, 3.879f, 15.879f)
                curveTo(3.316f, 16.441f, 3f, 17.204f, 3f, 18f)
                curveTo(3f, 18.796f, 3.316f, 19.559f, 3.879f, 20.121f)
                curveTo(4.441f, 20.684f, 5.204f, 21f, 6f, 21f)
                curveTo(6.796f, 21f, 7.559f, 20.684f, 8.121f, 20.121f)
                curveTo(8.684f, 19.559f, 9f, 18.796f, 9f, 18f)
                verticalLineTo(6f)
                curveTo(9f, 5.204f, 8.684f, 4.441f, 8.121f, 3.879f)
                curveTo(7.559f, 3.316f, 6.796f, 3f, 6f, 3f)
                curveTo(5.204f, 3f, 4.441f, 3.316f, 3.879f, 3.879f)
                curveTo(3.316f, 4.441f, 3f, 5.204f, 3f, 6f)
                curveTo(3f, 6.796f, 3.316f, 7.559f, 3.879f, 8.121f)
                curveTo(4.441f, 8.684f, 5.204f, 9f, 6f, 9f)
                horizontalLineTo(18f)
                curveTo(18.796f, 9f, 19.559f, 8.684f, 20.121f, 8.121f)
                curveTo(20.684f, 7.559f, 21f, 6.796f, 21f, 6f)
                curveTo(21f, 5.204f, 20.684f, 4.441f, 20.121f, 3.879f)
                curveTo(19.559f, 3.316f, 18.796f, 3f, 18f, 3f)
                close()
            }
        }.build()

        return _Command!!
    }

@Suppress("ObjectPropertyName")
private var _Command: ImageVector? = null
