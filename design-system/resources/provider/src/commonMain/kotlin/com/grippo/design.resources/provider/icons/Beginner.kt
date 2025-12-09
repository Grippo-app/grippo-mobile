package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Beginner: ImageVector
    get() {
        if (_Beginner != null) {
            return _Beginner!!
        }
        _Beginner = ImageVector.Builder(
            name = "Beginner",
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
                moveTo(7.881f, 15.756f)
                curveTo(8.374f, 16.183f, 9.023f, 16.491f, 9.723f, 16.693f)
                curveTo(10.43f, 16.897f, 11.214f, 17f, 12f, 17f)
                curveTo(12.786f, 17f, 13.57f, 16.897f, 14.277f, 16.693f)
                curveTo(14.977f, 16.491f, 15.626f, 16.183f, 16.119f, 15.756f)
            }
            path(
                fill = SolidColor(Color(0xFF33363F)),
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 0.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 10f)
                moveToRelative(-1.25f, 0f)
                arcToRelative(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2.5f, 0f)
                arcToRelative(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, -2.5f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF33363F)),
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 0.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15f, 10f)
                moveToRelative(-1.25f, 0f)
                arcToRelative(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2.5f, 0f)
                arcToRelative(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, -2.5f, 0f)
            }
        }.build()

        return _Beginner!!
    }

@Suppress("ObjectPropertyName")
private var _Beginner: ImageVector? = null
