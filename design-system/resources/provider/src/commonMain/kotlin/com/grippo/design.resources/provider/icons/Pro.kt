package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Pro: ImageVector
    get() {
        if (_Pro != null) {
            return _Pro!!
        }
        _Pro = ImageVector.Builder(
            name = "Pro",
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
                moveTo(12f, 12f)
                moveToRelative(-10f, 0f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20f, 0f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, -20f, 0f)
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(8f, 14.556f)
                curveTo(8f, 14.504f, 8f, 14.478f, 8.002f, 14.456f)
                curveTo(8.023f, 14.215f, 8.215f, 14.023f, 8.456f, 14.002f)
                curveTo(8.478f, 14f, 8.504f, 14f, 8.556f, 14f)
                horizontalLineTo(15.444f)
                curveTo(15.496f, 14f, 15.522f, 14f, 15.544f, 14.002f)
                curveTo(15.785f, 14.023f, 15.977f, 14.215f, 15.998f, 14.456f)
                curveTo(16f, 14.478f, 16f, 14.504f, 16f, 14.556f)
                verticalLineTo(15f)
                curveTo(16f, 17.209f, 14.209f, 19f, 12f, 19f)
                curveTo(9.791f, 19f, 8f, 17.209f, 8f, 15f)
                verticalLineTo(14.556f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF33363F)),
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 0.25f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 7.875f)
                lineTo(9f, 7.875f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10.125f, 9f)
                lineTo(10.125f, 9f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 10.125f)
                lineTo(8f, 10.125f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.875f, 9f)
                lineTo(6.875f, 9f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8f, 7.875f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF33363F)),
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 0.25f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15f, 7.875f)
                lineTo(16f, 7.875f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.125f, 9f)
                lineTo(17.125f, 9f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 10.125f)
                lineTo(15f, 10.125f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 13.875f, 9f)
                lineTo(13.875f, 9f)
                arcTo(1.125f, 1.125f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 7.875f)
                close()
            }
        }.build()

        return _Pro!!
    }

@Suppress("ObjectPropertyName")
private var _Pro: ImageVector? = null
