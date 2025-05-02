package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ChevronsDown: ImageVector
    get() {
        if (_ChevronsDown != null) {
            return _ChevronsDown!!
        }
        _ChevronsDown = ImageVector.Builder(
            name = "ChevronsDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 13f)
                lineTo(12f, 18f)
                lineTo(17f, 13f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 6f)
                lineTo(12f, 11f)
                lineTo(17f, 6f)
            }
        }.build()

        return _ChevronsDown!!
    }

@Suppress("ObjectPropertyName")
private var _ChevronsDown: ImageVector? = null
