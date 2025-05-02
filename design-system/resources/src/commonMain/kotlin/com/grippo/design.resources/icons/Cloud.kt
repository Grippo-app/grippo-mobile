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

public val AppIcon.Cloud: ImageVector
    get() {
        if (_Cloud != null) {
            return _Cloud!!
        }
        _Cloud = ImageVector.Builder(
            name = "Cloud",
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
                    moveTo(18f, 10f)
                    horizontalLineTo(16.74f)
                    curveTo(16.366f, 8.551f, 15.593f, 7.236f, 14.509f, 6.204f)
                    curveTo(13.425f, 5.173f, 12.073f, 4.466f, 10.607f, 4.164f)
                    curveTo(9.141f, 3.863f, 7.62f, 3.978f, 6.217f, 4.498f)
                    curveTo(4.813f, 5.017f, 3.584f, 5.92f, 2.668f, 7.103f)
                    curveTo(1.752f, 8.287f, 1.186f, 9.703f, 1.035f, 11.192f)
                    curveTo(0.884f, 12.681f, 1.153f, 14.183f, 1.813f, 15.526f)
                    curveTo(2.473f, 16.869f, 3.496f, 18.001f, 4.766f, 18.792f)
                    curveTo(6.037f, 19.582f, 7.503f, 20.001f, 9f, 20f)
                    horizontalLineTo(18f)
                    curveTo(19.326f, 20f, 20.598f, 19.473f, 21.535f, 18.535f)
                    curveTo(22.473f, 17.598f, 23f, 16.326f, 23f, 15f)
                    curveTo(23f, 13.674f, 22.473f, 12.402f, 21.535f, 11.465f)
                    curveTo(20.598f, 10.527f, 19.326f, 10f, 18f, 10f)
                    close()
                }
            }
        }.build()

        return _Cloud!!
    }

@Suppress("ObjectPropertyName")
private var _Cloud: ImageVector? = null
