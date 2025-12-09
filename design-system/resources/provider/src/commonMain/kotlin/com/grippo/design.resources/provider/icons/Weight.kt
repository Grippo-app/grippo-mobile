package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Weight: ImageVector
    get() {
        if (_Weight != null) {
            return _Weight!!
        }
        _Weight = ImageVector.Builder(
            name = "Weight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20.693f, 17.329f)
                curveTo(21.051f, 15.996f, 21.096f, 14.598f, 20.827f, 13.244f)
                curveTo(20.558f, 11.89f, 19.981f, 10.616f, 19.14f, 9.521f)
                curveTo(18.3f, 8.426f, 17.219f, 7.539f, 15.981f, 6.928f)
                curveTo(14.743f, 6.318f, 13.38f, 6f, 12f, 6f)
                curveTo(10.62f, 6f, 9.258f, 6.318f, 8.019f, 6.928f)
                curveTo(6.781f, 7.539f, 5.7f, 8.426f, 4.86f, 9.521f)
                curveTo(4.019f, 10.616f, 3.442f, 11.89f, 3.173f, 13.244f)
                curveTo(2.904f, 14.598f, 2.949f, 15.996f, 3.307f, 17.329f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(12.766f, 15.582f)
                curveTo(13.253f, 16.292f, 12.91f, 17.374f, 12f, 17.999f)
                curveTo(11.09f, 18.625f, 9.957f, 18.557f, 9.469f, 17.848f)
                curveTo(8.95f, 17.092f, 7.156f, 12.841f, 6.067f, 10.211f)
                curveTo(5.862f, 9.716f, 6.468f, 9.3f, 6.856f, 9.669f)
                curveTo(8.921f, 11.628f, 12.246f, 14.826f, 12.766f, 15.582f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 6f)
                verticalLineTo(8f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.636f, 8.636f)
                lineTo(7.05f, 10.05f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18.364f, 8.636f)
                lineTo(16.95f, 10.05f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20.693f, 17.329f)
                lineTo(18.761f, 16.812f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.307f, 17.329f)
                lineTo(5.238f, 16.812f)
            }
        }.build()

        return _Weight!!
    }

@Suppress("ObjectPropertyName")
private var _Weight: ImageVector? = null
