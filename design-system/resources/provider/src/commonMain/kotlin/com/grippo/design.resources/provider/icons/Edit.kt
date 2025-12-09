package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Edit: ImageVector
    get() {
        if (_Edit != null) {
            return _Edit!!
        }
        _Edit = ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(15.188f, 5.424f)
                curveTo(15.612f, 5.469f, 15.95f, 5.664f, 16.219f, 5.869f)
                curveTo(16.503f, 6.086f, 16.808f, 6.394f, 17.121f, 6.707f)
                lineTo(17.293f, 6.879f)
                curveTo(17.606f, 7.192f, 17.914f, 7.497f, 18.131f, 7.781f)
                curveTo(18.365f, 8.089f, 18.586f, 8.486f, 18.586f, 9f)
                curveTo(18.586f, 9.514f, 18.365f, 9.911f, 18.131f, 10.219f)
                curveTo(17.914f, 10.503f, 17.606f, 10.808f, 17.293f, 11.121f)
                lineTo(10.099f, 18.315f)
                curveTo(9.942f, 18.472f, 9.738f, 18.689f, 9.475f, 18.838f)
                curveTo(9.211f, 18.987f, 8.921f, 19.051f, 8.705f, 19.104f)
                lineTo(6.097f, 19.756f)
                lineTo(6.095f, 19.757f)
                lineTo(6.051f, 19.768f)
                curveTo(5.903f, 19.805f, 5.682f, 19.863f, 5.487f, 19.882f)
                curveTo(5.281f, 19.902f, 4.829f, 19.909f, 4.46f, 19.54f)
                curveTo(4.091f, 19.171f, 4.098f, 18.719f, 4.118f, 18.513f)
                curveTo(4.137f, 18.318f, 4.195f, 18.097f, 4.232f, 17.949f)
                lineTo(4.896f, 15.295f)
                curveTo(4.949f, 15.079f, 5.013f, 14.789f, 5.162f, 14.525f)
                curveTo(5.311f, 14.262f, 5.528f, 14.058f, 5.685f, 13.901f)
                lineTo(12.879f, 6.707f)
                curveTo(13.192f, 6.394f, 13.497f, 6.086f, 13.781f, 5.869f)
                curveTo(14.089f, 5.635f, 14.486f, 5.414f, 15f, 5.414f)
                lineTo(15.188f, 5.424f)
                close()
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12.5f, 7.5f)
                lineTo(15.5f, 5.5f)
                lineTo(18.5f, 8.5f)
                lineTo(16.5f, 11.5f)
                lineTo(12.5f, 7.5f)
                close()
            }
        }.build()

        return _Edit!!
    }

@Suppress("ObjectPropertyName")
private var _Edit: ImageVector? = null
