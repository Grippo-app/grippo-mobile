package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Repeat: ImageVector
    get() {
        if (_Repeat != null) {
            return _Repeat!!
        }
        _Repeat = ImageVector.Builder(
            name = "Repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f
            ) {
                moveTo(14f, 11f)
                lineTo(10f, 15f)
                lineTo(14f, 19f)
            }
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.206f, 12.75f)
                curveTo(3.215f, 11.892f, 2.818f, 10.895f, 3.077f, 9.913f)
                curveTo(3.336f, 8.931f, 4.235f, 8.018f, 5.636f, 7.318f)
                curveTo(7.037f, 6.618f, 8.861f, 6.168f, 10.825f, 6.038f)
                curveTo(12.79f, 5.909f, 14.784f, 6.108f, 16.5f, 6.603f)
                curveTo(18.216f, 7.098f, 19.557f, 7.863f, 20.315f, 8.778f)
                curveTo(21.073f, 9.693f, 21.206f, 10.708f, 20.693f, 11.665f)
                curveTo(20.181f, 12.622f, 19.051f, 13.467f, 17.479f, 14.07f)
                curveTo(15.907f, 14.673f, 13.981f, 15f, 12f, 15f)
            }
        }.build()

        return _Repeat!!
    }

@Suppress("ObjectPropertyName")
private var _Repeat: ImageVector? = null
