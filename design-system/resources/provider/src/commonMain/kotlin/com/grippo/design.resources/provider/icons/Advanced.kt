package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Advanced: ImageVector
    get() {
        if (_Advanced != null) {
            return _Advanced!!
        }
        _Advanced = ImageVector.Builder(
            name = "Advanced",
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
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9.402f, 13.5f)
                curveTo(9.665f, 13.956f, 10.044f, 14.335f, 10.5f, 14.598f)
                curveTo(10.956f, 14.861f, 11.473f, 15f, 12f, 15f)
                curveTo(12.527f, 15f, 13.044f, 14.861f, 13.5f, 14.598f)
                curveTo(13.956f, 14.335f, 14.335f, 13.956f, 14.598f, 13.5f)
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

        return _Advanced!!
    }

@Suppress("ObjectPropertyName")
private var _Advanced: ImageVector? = null

