package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Logout: ImageVector
    get() {
        if (_Logout != null) {
            return _Logout!!
        }
        _Logout = ImageVector.Builder(
            name = "Logout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(3f, 12.5f)
                lineTo(2.215f, 11.88f)
                lineTo(1.726f, 12.5f)
                lineTo(2.215f, 13.12f)
                lineTo(3f, 12.5f)
                close()
                moveTo(11f, 13.5f)
                curveTo(11.552f, 13.5f, 12f, 13.052f, 12f, 12.5f)
                curveTo(12f, 11.948f, 11.552f, 11.5f, 11f, 11.5f)
                verticalLineTo(12.5f)
                verticalLineTo(13.5f)
                close()
                moveTo(6.556f, 8f)
                lineTo(5.771f, 7.38f)
                lineTo(2.215f, 11.88f)
                lineTo(3f, 12.5f)
                lineTo(3.785f, 13.12f)
                lineTo(7.34f, 8.62f)
                lineTo(6.556f, 8f)
                close()
                moveTo(3f, 12.5f)
                lineTo(2.215f, 13.12f)
                lineTo(5.771f, 17.62f)
                lineTo(6.556f, 17f)
                lineTo(7.34f, 16.38f)
                lineTo(3.785f, 11.88f)
                lineTo(3f, 12.5f)
                close()
                moveTo(3f, 12.5f)
                verticalLineTo(13.5f)
                horizontalLineTo(11f)
                verticalLineTo(12.5f)
                verticalLineTo(11.5f)
                horizontalLineTo(3f)
                verticalLineTo(12.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(10f, 8.519f)
                verticalLineTo(8.199f)
                curveTo(10f, 6.576f, 10f, 5.764f, 10.476f, 5.204f)
                curveTo(10.952f, 4.644f, 11.752f, 4.513f, 13.354f, 4.251f)
                lineTo(14.031f, 4.14f)
                curveTo(17.267f, 3.611f, 18.885f, 3.346f, 19.942f, 4.245f)
                curveTo(21f, 5.143f, 21f, 6.783f, 21f, 10.062f)
                verticalLineTo(13.938f)
                curveTo(21f, 17.217f, 21f, 18.857f, 19.942f, 19.755f)
                curveTo(18.885f, 20.654f, 17.267f, 20.389f, 14.031f, 19.86f)
                lineTo(13.354f, 19.749f)
                curveTo(11.752f, 19.487f, 10.952f, 19.356f, 10.476f, 18.796f)
                curveTo(10f, 18.236f, 10f, 17.424f, 10f, 15.801f)
                verticalLineTo(15.659f)
            }
        }.build()

        return _Logout!!
    }

@Suppress("ObjectPropertyName")
private var _Logout: ImageVector? = null


