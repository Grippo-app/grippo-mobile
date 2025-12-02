package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Google: ImageVector
    get() {
        if (_Google != null) {
            return _Google!!
        }
        _Google = ImageVector.Builder(
            name = "GoogleIconLogoSvgrepoCom",
            defaultWidth = 800.dp,
            defaultHeight = 800.dp,
            viewportWidth = 262f,
            viewportHeight = 262f
        ).apply {
            path(fill = SolidColor(Color(0xFF4285F4))) {
                moveTo(258.88f, 133.45f)
                curveToRelative(0f, -10.73f, -0.87f, -18.57f, -2.76f, -26.69f)
                lineTo(133.55f, 106.76f)
                verticalLineToRelative(48.45f)
                horizontalLineToRelative(71.95f)
                curveToRelative(-1.45f, 12.04f, -9.28f, 30.17f, -26.69f, 42.36f)
                lineToRelative(-0.24f, 1.62f)
                lineToRelative(38.76f, 30.02f)
                lineToRelative(2.68f, 0.27f)
                curveToRelative(24.66f, -22.77f, 38.88f, -56.28f, 38.88f, -96.03f)
            }
            path(fill = SolidColor(Color(0xFF34A853))) {
                moveTo(133.55f, 261.1f)
                curveToRelative(35.25f, 0f, 64.84f, -11.6f, 86.45f, -31.62f)
                lineToRelative(-41.2f, -31.91f)
                curveToRelative(-11.02f, 7.69f, -25.82f, 13.06f, -45.26f, 13.06f)
                curveToRelative(-34.52f, 0f, -63.82f, -22.77f, -74.27f, -54.25f)
                lineToRelative(-1.53f, 0.13f)
                lineToRelative(-40.3f, 31.19f)
                lineToRelative(-0.53f, 1.47f)
                curveTo(38.39f, 231.8f, 82.49f, 261.1f, 133.55f, 261.1f)
            }
            path(fill = SolidColor(Color(0xFFFBBC05))) {
                moveTo(59.28f, 156.37f)
                curveToRelative(-2.76f, -8.12f, -4.35f, -16.83f, -4.35f, -25.82f)
                curveToRelative(0f, -8.99f, 1.6f, -17.7f, 4.21f, -25.82f)
                lineToRelative(-0.07f, -1.73f)
                lineTo(18.26f, 71.31f)
                lineToRelative(-1.34f, 0.63f)
                curveTo(8.08f, 89.64f, 3f, 109.52f, 3f, 130.55f)
                reflectiveCurveToRelative(5.08f, 40.9f, 13.93f, 58.6f)
                lineToRelative(42.36f, -32.78f)
            }
            path(fill = SolidColor(Color(0xFFEB4335))) {
                moveTo(133.55f, 50.48f)
                curveToRelative(24.51f, 0f, 41.05f, 10.59f, 50.48f, 19.44f)
                lineToRelative(36.84f, -35.97f)
                curveTo(198.24f, 12.91f, 168.8f, 0f, 133.55f, 0f)
                curveTo(82.49f, 0f, 38.39f, 29.3f, 16.92f, 71.95f)
                lineToRelative(42.21f, 32.78f)
                curveToRelative(10.59f, -31.48f, 39.89f, -54.25f, 74.41f, -54.25f)
            }
        }.build()

        return _Google!!
    }

@Suppress("ObjectPropertyName")
private var _Google: ImageVector? = null
