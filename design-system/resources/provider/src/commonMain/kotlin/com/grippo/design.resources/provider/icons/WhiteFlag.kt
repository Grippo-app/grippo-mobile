package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.WhiteFlag: ImageVector
    get() {
        if (_WhiteFlag != null) {
            return _WhiteFlag!!
        }
        _WhiteFlag = ImageVector.Builder(
            name = "WhiteFlag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 15f)
                lineTo(4.4f, 21f)
                moveTo(5f, 15f)
                lineTo(5.95f, 4.546f)
                curveTo(5.978f, 4.237f, 6.238f, 4f, 6.548f, 4f)
                horizontalLineTo(20.343f)
                curveTo(20.696f, 4f, 20.972f, 4.303f, 20.94f, 4.654f)
                lineTo(20.049f, 14.454f)
                curveTo(20.021f, 14.763f, 19.762f, 15f, 19.452f, 15f)
                horizontalLineTo(5f)
                close()
            }
        }.build()

        return _WhiteFlag!!
    }

@Suppress("ObjectPropertyName")
private var _WhiteFlag: ImageVector? = null
