package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Molecule: ImageVector
    get() {
        if (_Molecule != null) {
            return _Molecule!!
        }
        _Molecule = ImageVector.Builder(
            name = "Molecule",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 12f)
                moveToRelative(-3f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 14f)
                lineTo(7f, 17f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 10.5f)
                lineTo(6f, 6f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(18f, 19f)
                moveToRelative(-1f, 0f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2f, 0f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, -2f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 14f)
                lineTo(18f, 19f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(19f, 5f)
                moveToRelative(-2f, 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 7f)
                lineTo(14f, 10f)
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(5.5f, 5.5f)
                moveToRelative(-2.5f, 0f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5f, 0f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -5f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(5.5f, 18.5f)
                moveToRelative(-2.5f, 0f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5f, 0f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -5f, 0f)
            }
        }.build()

        return _Molecule!!
    }

@Suppress("ObjectPropertyName")
private var _Molecule: ImageVector? = null

