package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.SettingsProfiles: ImageVector
    get() {
        if (_SettingsProfiles != null) {
            return _SettingsProfiles!!
        }
        _SettingsProfiles = ImageVector.Builder(
            name = "SettingsProfiles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(11.606f, 2.342f)
                curveTo(11.832f, 2.146f, 12.168f, 2.146f, 12.394f, 2.342f)
                lineTo(14.342f, 4.034f)
                curveTo(14.465f, 4.141f, 14.625f, 4.193f, 14.787f, 4.179f)
                lineTo(17.359f, 3.955f)
                curveTo(17.656f, 3.929f, 17.928f, 4.126f, 17.995f, 4.418f)
                lineTo(18.577f, 6.932f)
                curveTo(18.613f, 7.091f, 18.713f, 7.228f, 18.852f, 7.311f)
                lineTo(21.064f, 8.641f)
                curveTo(21.32f, 8.795f, 21.424f, 9.115f, 21.307f, 9.39f)
                lineTo(20.299f, 11.766f)
                curveTo(20.236f, 11.915f, 20.236f, 12.085f, 20.299f, 12.234f)
                lineTo(21.307f, 14.61f)
                curveTo(21.424f, 14.885f, 21.32f, 15.205f, 21.064f, 15.359f)
                lineTo(18.852f, 16.689f)
                curveTo(18.713f, 16.772f, 18.613f, 16.909f, 18.577f, 17.068f)
                lineTo(17.995f, 19.582f)
                curveTo(17.928f, 19.874f, 17.656f, 20.071f, 17.359f, 20.045f)
                lineTo(14.787f, 19.821f)
                curveTo(14.625f, 19.807f, 14.465f, 19.859f, 14.342f, 19.965f)
                lineTo(12.394f, 21.658f)
                curveTo(12.168f, 21.854f, 11.832f, 21.854f, 11.606f, 21.658f)
                lineTo(9.658f, 19.965f)
                curveTo(9.535f, 19.859f, 9.375f, 19.807f, 9.213f, 19.821f)
                lineTo(6.641f, 20.045f)
                curveTo(6.344f, 20.071f, 6.072f, 19.874f, 6.005f, 19.582f)
                lineTo(5.423f, 17.068f)
                curveTo(5.387f, 16.909f, 5.287f, 16.772f, 5.148f, 16.689f)
                lineTo(2.936f, 15.359f)
                curveTo(2.68f, 15.205f, 2.576f, 14.885f, 2.693f, 14.61f)
                lineTo(3.701f, 12.234f)
                curveTo(3.764f, 12.085f, 3.764f, 11.915f, 3.701f, 11.766f)
                lineTo(2.693f, 9.39f)
                curveTo(2.576f, 9.115f, 2.68f, 8.795f, 2.936f, 8.641f)
                lineTo(5.148f, 7.311f)
                curveTo(5.287f, 7.228f, 5.387f, 7.091f, 5.423f, 6.932f)
                lineTo(6.005f, 4.418f)
                curveTo(6.072f, 4.126f, 6.344f, 3.929f, 6.641f, 3.955f)
                lineTo(9.213f, 4.179f)
                curveTo(9.375f, 4.193f, 9.535f, 4.141f, 9.658f, 4.034f)
                lineTo(11.606f, 2.342f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 13f)
                lineTo(11f, 15f)
                lineTo(16f, 10f)
            }
        }.build()

        return _SettingsProfiles!!
    }

@Suppress("ObjectPropertyName")
private var _SettingsProfiles: ImageVector? = null
