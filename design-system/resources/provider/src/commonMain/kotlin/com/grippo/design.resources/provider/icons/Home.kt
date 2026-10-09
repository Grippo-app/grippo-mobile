package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Home: ImageVector
    get() {
        if (_Home != null) {
            return _Home!!
        }
        _Home = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
            ) {
                moveTo(8.09525f, 7.34693f)
                lineTo(7.09525f, 8.20407f)
                curveTo(6.06437f, 9.08769f, 5.54892f, 9.5295f, 5.27446f, 10.1262f)
                curveTo(5f, 10.723f, 5f, 11.4019f, 5f, 12.7596f)
                verticalLineTo(17f)
                curveTo(5f, 18.8856f, 5f, 19.8284f, 5.58579f, 20.4142f)
                curveTo(6.17157f, 21f, 7.11438f, 21f, 9f, 21f)
                horizontalLineTo(15f)
                curveTo(16.8856f, 21f, 17.8284f, 21f, 18.4142f, 20.4142f)
                curveTo(19f, 19.8284f, 19f, 18.8856f, 19f, 17f)
                verticalLineTo(12.7596f)
                curveTo(19f, 11.4019f, 19f, 10.723f, 18.7255f, 10.1262f)
                curveTo(18.4511f, 9.5295f, 17.9356f, 9.08769f, 16.9047f, 8.20407f)
                lineTo(15.9047f, 7.34693f)
                curveTo(14.0414f, 5.7498f, 13.1098f, 4.95123f, 12f, 4.95123f)
                curveTo(10.8902f, 4.95123f, 9.95857f, 5.7498f, 8.09525f, 7.34693f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14.5f, 21f)
                verticalLineTo(16f)
                curveTo(14.5f, 15.4477f, 14.0523f, 15f, 13.5f, 15f)
                horizontalLineTo(10.5f)
                curveTo(9.94772f, 15f, 9.5f, 15.4477f, 9.5f, 16f)
                verticalLineTo(21f)
            }
        }.build()

        return _Home!!
    }

@Suppress("ObjectPropertyName")
private var _Home: ImageVector? = null
