package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Umbrella: ImageVector
    get() {
        if (_Umbrella != null) {
            return _Umbrella!!
        }
        _Umbrella = ImageVector.Builder(
            name = "Umbrella",
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
                moveTo(18f, 19f)
                curveTo(18f, 19.796f, 17.684f, 20.559f, 17.121f, 21.121f)
                curveTo(16.559f, 21.684f, 15.796f, 22f, 15f, 22f)
                curveTo(14.204f, 22f, 13.441f, 21.684f, 12.879f, 21.121f)
                curveTo(12.316f, 20.559f, 12f, 19.796f, 12f, 19f)
                verticalLineTo(12f)
                moveTo(23f, 12f)
                curveTo(22.739f, 9.264f, 21.467f, 6.723f, 19.433f, 4.874f)
                curveTo(17.399f, 3.025f, 14.749f, 2f, 12f, 2f)
                curveTo(9.251f, 2f, 6.601f, 3.025f, 4.567f, 4.874f)
                curveTo(2.533f, 6.723f, 1.261f, 9.264f, 1f, 12f)
                horizontalLineTo(23f)
                close()
            }
        }.build()

        return _Umbrella!!
    }

@Suppress("ObjectPropertyName")
private var _Umbrella: ImageVector? = null
