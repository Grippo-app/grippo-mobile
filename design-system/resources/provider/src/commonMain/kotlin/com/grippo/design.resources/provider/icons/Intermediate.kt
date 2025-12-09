package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Intermediate: ImageVector
    get() {
        if (_Intermediate != null) {
            return _Intermediate!!
        }
        _Intermediate = ImageVector.Builder(
            name = "Intermediate",
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
                moveTo(15.37f, 15f)
                curveTo(15.65f, 15f, 15.79f, 15f, 15.865f, 15.107f)
                curveTo(15.939f, 15.214f, 15.896f, 15.332f, 15.81f, 15.568f)
                curveTo(15.293f, 16.979f, 13.783f, 18f, 12f, 18f)
                curveTo(10.217f, 18f, 8.706f, 16.979f, 8.19f, 15.568f)
                curveTo(8.104f, 15.332f, 8.06f, 15.214f, 8.135f, 15.107f)
                curveTo(8.21f, 15f, 8.349f, 15f, 8.629f, 15f)
                horizontalLineTo(15.37f)
                close()
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

        return _Intermediate!!
    }

@Suppress("ObjectPropertyName")
private var _Intermediate: ImageVector? = null
