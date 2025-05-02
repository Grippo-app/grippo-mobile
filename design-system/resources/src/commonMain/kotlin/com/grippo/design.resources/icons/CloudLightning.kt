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

public val AppIcon.CloudLightning: ImageVector
    get() {
        if (_CloudLightning != null) {
            return _CloudLightning!!
        }
        _CloudLightning = ImageVector.Builder(
            name = "CloudLightning",
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
                    moveTo(19f, 16.9f)
                    curveTo(20.215f, 16.653f, 21.295f, 15.964f, 22.031f, 14.965f)
                    curveTo(22.766f, 13.967f, 23.104f, 12.731f, 22.98f, 11.498f)
                    curveTo(22.855f, 10.264f, 22.277f, 9.12f, 21.356f, 8.289f)
                    curveTo(20.436f, 7.458f, 19.24f, 6.999f, 18f, 7f)
                    horizontalLineTo(16.74f)
                    curveTo(16.409f, 5.717f, 15.764f, 4.537f, 14.864f, 3.565f)
                    curveTo(13.964f, 2.593f, 12.836f, 1.86f, 11.583f, 1.431f)
                    curveTo(10.329f, 1.002f, 8.989f, 0.892f, 7.682f, 1.109f)
                    curveTo(6.375f, 1.326f, 5.143f, 1.865f, 4.096f, 2.676f)
                    curveTo(3.048f, 3.487f, 2.219f, 4.546f, 1.681f, 5.757f)
                    curveTo(1.144f, 6.968f, 0.916f, 8.293f, 1.018f, 9.614f)
                    curveTo(1.119f, 10.935f, 1.547f, 12.21f, 2.263f, 13.325f)
                    curveTo(2.979f, 14.439f, 3.961f, 15.358f, 5.12f, 16f)
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(13f, 11f)
                    lineTo(9f, 17f)
                    horizontalLineTo(15f)
                    lineTo(11f, 23f)
                }
            }
        }.build()

        return _CloudLightning!!
    }

@Suppress("ObjectPropertyName")
private var _CloudLightning: ImageVector? = null
