package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PlayOutline: ImageVector
    get() {
        if (_PlayOutline != null) {
            return _PlayOutline!!
        }
        _PlayOutline = ImageVector.Builder(
            name = "PlayOutline",
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
                moveTo(6.906f, 4.537f)
                curveTo(6.506f, 4.3f, 6f, 4.588f, 6f, 5.053f)
                verticalLineTo(18.947f)
                curveTo(6f, 19.412f, 6.506f, 19.7f, 6.906f, 19.463f)
                lineTo(18.629f, 12.516f)
                curveTo(19.021f, 12.284f, 19.021f, 11.716f, 18.629f, 11.484f)
                lineTo(6.906f, 4.537f)
                close()
            }
        }.build()

        return _PlayOutline!!
    }

@Suppress("ObjectPropertyName")
private var _PlayOutline: ImageVector? = null
