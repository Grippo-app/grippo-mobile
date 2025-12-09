package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Trophy: ImageVector
    get() {
        if (_Trophy != null) {
            return _Trophy!!
        }
        _Trophy = ImageVector.Builder(
            name = "Trophy",
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
                moveTo(16.5f, 20.5f)
                horizontalLineTo(7.5f)
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(13f, 18.5f)
                curveTo(13f, 19.052f, 12.552f, 19.5f, 12f, 19.5f)
                curveTo(11.448f, 19.5f, 11f, 19.052f, 11f, 18.5f)
                horizontalLineTo(12f)
                horizontalLineTo(13f)
                close()
                moveTo(12f, 18.5f)
                horizontalLineTo(11f)
                verticalLineTo(16f)
                horizontalLineTo(12f)
                horizontalLineTo(13f)
                verticalLineTo(18.5f)
                horizontalLineTo(12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10.5f, 9.5f)
                horizontalLineTo(13.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.5f, 14.5f)
                curveTo(5.5f, 14.5f, 3.5f, 13f, 3.5f, 10.5f)
                curveTo(3.5f, 9.735f, 3.5f, 9.063f, 3.5f, 8.499f)
                curveTo(3.5f, 7.395f, 4.395f, 6.5f, 5.5f, 6.5f)
                curveTo(6.605f, 6.5f, 7.5f, 7.395f, 7.5f, 8.5f)
                verticalLineTo(9.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18.5f, 14.5f)
                curveTo(18.5f, 14.5f, 20.5f, 13f, 20.5f, 10.5f)
                curveTo(20.5f, 9.735f, 20.5f, 9.063f, 20.5f, 8.499f)
                curveTo(20.5f, 7.395f, 19.605f, 6.5f, 18.5f, 6.5f)
                curveTo(17.395f, 6.5f, 16.5f, 7.395f, 16.5f, 8.5f)
                verticalLineTo(9.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(16.5f, 11.359f)
                verticalLineTo(7.5f)
                curveTo(16.5f, 6.395f, 15.605f, 5.5f, 14.5f, 5.5f)
                horizontalLineTo(9.5f)
                curveTo(8.395f, 5.5f, 7.5f, 6.395f, 7.5f, 7.5f)
                verticalLineTo(11.359f)
                curveTo(7.5f, 12.697f, 8.168f, 13.946f, 9.281f, 14.688f)
                lineTo(11.445f, 16.13f)
                curveTo(11.781f, 16.354f, 12.219f, 16.354f, 12.555f, 16.13f)
                lineTo(14.719f, 14.688f)
                curveTo(15.832f, 13.946f, 16.5f, 12.697f, 16.5f, 11.359f)
                close()
            }
        }.build()

        return _Trophy!!
    }

@Suppress("ObjectPropertyName")
private var _Trophy: ImageVector? = null

