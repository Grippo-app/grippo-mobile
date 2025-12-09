package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Height: ImageVector
    get() {
        if (_Height != null) {
            return _Height!!
        }
        _Height = ImageVector.Builder(
            name = "Height",
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
                moveTo(8f, 3f)
                verticalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 14f)
                verticalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(6.8f, 6f)
                lineTo(9.2f, 6f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10f, 6.8f)
                lineTo(10f, 13.2f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9.2f, 14f)
                lineTo(6.8f, 14f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6f, 13.2f)
                lineTo(6f, 6.8f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.8f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 7f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 17f)
                verticalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(14.8f, 12f)
                lineTo(17.2f, 12f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 12.8f)
                lineTo(18f, 16.2f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.2f, 17f)
                lineTo(14.8f, 17f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14f, 16.2f)
                lineTo(14f, 12.8f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14.8f, 12f)
                close()
            }
        }.build()

        return _Height!!
    }

@Suppress("ObjectPropertyName")
private var _Height: ImageVector? = null

