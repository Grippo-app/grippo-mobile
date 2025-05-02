package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ChevronsUp: ImageVector
    get() {
        if (_ChevronsUp != null) {
            return _ChevronsUp!!
        }
        _ChevronsUp = ImageVector.Builder(
            name = "ChevronsUp",
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
                moveTo(17f, 11f)
                lineTo(12f, 6f)
                lineTo(7f, 11f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 18f)
                lineTo(12f, 13f)
                lineTo(7f, 18f)
            }
        }.build()

        return _ChevronsUp!!
    }

@Suppress("ObjectPropertyName")
private var _ChevronsUp: ImageVector? = null
