package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Filter: ImageVector
    get() {
        if (_Filter != null) {
            return _Filter!!
        }
        _Filter = ImageVector.Builder(
            name = "Filter",
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
                moveTo(12f, 7f)
                lineTo(20f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 7f)
                lineTo(8f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(17f, 17f)
                lineTo(20f, 17f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 17f)
                lineTo(12f, 17f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10f, 7f)
                moveToRelative(-0f, -2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -0f, 4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -0f, -4f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15f, 17f)
                moveToRelative(-0f, -2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -0f, 4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -0f, -4f)
            }
        }.build()

        return _Filter!!
    }

@Suppress("ObjectPropertyName")
private var _Filter: ImageVector? = null
