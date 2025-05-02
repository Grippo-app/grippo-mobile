package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Thermometer: ImageVector
    get() {
        if (_Thermometer != null) {
            return _Thermometer!!
        }
        _Thermometer = ImageVector.Builder(
            name = "Thermometer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(14f, 14.76f)
                    verticalLineTo(3.5f)
                    curveTo(14f, 2.837f, 13.737f, 2.201f, 13.268f, 1.732f)
                    curveTo(12.799f, 1.263f, 12.163f, 1f, 11.5f, 1f)
                    curveTo(10.837f, 1f, 10.201f, 1.263f, 9.732f, 1.732f)
                    curveTo(9.263f, 2.201f, 9f, 2.837f, 9f, 3.5f)
                    verticalLineTo(14.76f)
                    curveTo(8.197f, 15.296f, 7.588f, 16.077f, 7.263f, 16.986f)
                    curveTo(6.938f, 17.895f, 6.914f, 18.884f, 7.194f, 19.808f)
                    curveTo(7.474f, 20.732f, 8.044f, 21.541f, 8.819f, 22.116f)
                    curveTo(9.595f, 22.691f, 10.535f, 23.002f, 11.5f, 23.002f)
                    curveTo(12.465f, 23.002f, 13.405f, 22.691f, 14.181f, 22.116f)
                    curveTo(14.956f, 21.541f, 15.526f, 20.732f, 15.806f, 19.808f)
                    curveTo(16.087f, 18.884f, 16.062f, 17.895f, 15.737f, 16.986f)
                    curveTo(15.412f, 16.077f, 14.803f, 15.296f, 14f, 14.76f)
                    close()
                }
            }
        }.build()

        return _Thermometer!!
    }

@Suppress("ObjectPropertyName")
private var _Thermometer: ImageVector? = null
