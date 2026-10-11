package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Trash: ImageVector
    get() {
        if (_Trash != null) return _Trash!!
        _Trash = ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(9f, 7f)
                lineTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 7f)
                moveTo(6f, 7f)
                lineTo(7f, 20f)
                lineTo(17f, 20f)
                lineTo(18f, 7f)
                moveTo(10f, 11f)
                lineTo(10.5f, 16f)
                moveTo(14f, 11f)
                lineTo(13.5f, 16f)
            }
        }.build()
        return _Trash!!
    }

@Suppress("ObjectPropertyName")
private var _Trash: ImageVector? = null
