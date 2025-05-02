package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Moon: ImageVector
    get() {
        if (_Moon != null) {
            return _Moon!!
        }
        _Moon = ImageVector.Builder(
            name = "Moon",
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
                moveTo(21f, 12.79f)
                curveTo(20.843f, 14.492f, 20.204f, 16.114f, 19.158f, 17.467f)
                curveTo(18.113f, 18.819f, 16.704f, 19.846f, 15.096f, 20.427f)
                curveTo(13.488f, 21.007f, 11.748f, 21.118f, 10.08f, 20.746f)
                curveTo(8.411f, 20.374f, 6.883f, 19.535f, 5.674f, 18.326f)
                curveTo(4.465f, 17.117f, 3.626f, 15.589f, 3.254f, 13.92f)
                curveTo(2.882f, 12.252f, 2.993f, 10.512f, 3.573f, 8.904f)
                curveTo(4.154f, 7.297f, 5.181f, 5.887f, 6.533f, 4.842f)
                curveTo(7.886f, 3.796f, 9.508f, 3.157f, 11.21f, 3f)
                curveTo(10.213f, 4.348f, 9.734f, 6.009f, 9.859f, 7.681f)
                curveTo(9.983f, 9.353f, 10.704f, 10.925f, 11.889f, 12.111f)
                curveTo(13.075f, 13.296f, 14.647f, 14.017f, 16.319f, 14.142f)
                curveTo(17.991f, 14.266f, 19.652f, 13.787f, 21f, 12.79f)
                verticalLineTo(12.79f)
                close()
            }
        }.build()

        return _Moon!!
    }

@Suppress("ObjectPropertyName")
private var _Moon: ImageVector? = null
