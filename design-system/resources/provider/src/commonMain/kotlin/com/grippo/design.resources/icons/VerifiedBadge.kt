package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.VerifiedBadge: ImageVector
    get() {
        if (_VerifiedBadge != null) {
            return _VerifiedBadge!!
        }
        _VerifiedBadge = ImageVector.Builder(
            name = "VerifiedBadge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(11.528f, 1.6f)
                curveTo(11.769f, 1.294f, 12.231f, 1.294f, 12.472f, 1.6f)
                lineTo(14.281f, 3.901f)
                curveTo(14.431f, 4.092f, 14.682f, 4.173f, 14.916f, 4.107f)
                lineTo(17.731f, 3.309f)
                curveTo(18.105f, 3.203f, 18.48f, 3.475f, 18.495f, 3.863f)
                lineTo(18.606f, 6.788f)
                curveTo(18.615f, 7.031f, 18.77f, 7.244f, 18.998f, 7.328f)
                lineTo(21.745f, 8.338f)
                curveTo(22.11f, 8.472f, 22.253f, 8.912f, 22.037f, 9.235f)
                lineTo(20.408f, 11.666f)
                curveTo(20.272f, 11.868f, 20.272f, 12.132f, 20.408f, 12.334f)
                lineTo(22.037f, 14.765f)
                curveTo(22.253f, 15.088f, 22.11f, 15.528f, 21.745f, 15.663f)
                lineTo(18.998f, 16.672f)
                curveTo(18.77f, 16.756f, 18.615f, 16.969f, 18.606f, 17.212f)
                lineTo(18.495f, 20.137f)
                curveTo(18.48f, 20.525f, 18.105f, 20.797f, 17.731f, 20.691f)
                lineTo(14.916f, 19.893f)
                curveTo(14.682f, 19.827f, 14.431f, 19.908f, 14.281f, 20.1f)
                lineTo(12.472f, 22.4f)
                curveTo(12.231f, 22.706f, 11.769f, 22.706f, 11.528f, 22.4f)
                lineTo(9.719f, 20.1f)
                curveTo(9.569f, 19.908f, 9.318f, 19.827f, 9.084f, 19.893f)
                lineTo(6.269f, 20.691f)
                curveTo(5.895f, 20.797f, 5.52f, 20.525f, 5.505f, 20.137f)
                lineTo(5.394f, 17.212f)
                curveTo(5.385f, 16.969f, 5.23f, 16.756f, 5.002f, 16.672f)
                lineTo(2.255f, 15.663f)
                curveTo(1.89f, 15.528f, 1.747f, 15.088f, 1.963f, 14.765f)
                lineTo(3.592f, 12.334f)
                curveTo(3.728f, 12.132f, 3.728f, 11.868f, 3.592f, 11.666f)
                lineTo(1.963f, 9.235f)
                curveTo(1.747f, 8.912f, 1.89f, 8.472f, 2.255f, 8.338f)
                lineTo(5.002f, 7.328f)
                curveTo(5.23f, 7.244f, 5.385f, 7.031f, 5.394f, 6.788f)
                lineTo(5.505f, 3.863f)
                curveTo(5.52f, 3.475f, 5.895f, 3.203f, 6.269f, 3.309f)
                lineTo(9.084f, 4.107f)
                curveTo(9.318f, 4.173f, 9.569f, 4.092f, 9.719f, 3.901f)
                lineTo(11.528f, 1.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15f, 10f)
            }
        }.build()

        return _VerifiedBadge!!
    }

@Suppress("ObjectPropertyName")
private var _VerifiedBadge: ImageVector? = null
