package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

/**
 * Flexed bicep / arm muscle silhouette.
 * Designer-supplied "BodyPartMuscle" outline (118x118 viewport), closed and
 * filled — the inner crease line was dropped, since the outer silhouette alone
 * reads cleanly as a flexed arm at icon sizes.
 */
public val AppIcon.Muscle: ImageVector
    get() {
        if (_Muscle != null) {
            return _Muscle!!
        }
        _Muscle = ImageVector.Builder(
            name = "Muscle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 118f,
            viewportHeight = 118f
        ).apply {
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(9.92f, 99.83f)
                curveTo(15.47f, 107.77f, 39.62f, 114f, 51.03f, 99.14f)
                curveTo(63.37f, 105.04f, 83.72f, 103.21f, 100.29f, 93.97f)
                curveTo(102.6f, 92.69f, 104.78f, 91.07f, 106.11f, 88.79f)
                curveTo(109.13f, 83.65f, 109.2f, 76.52f, 103.7f, 66.01f)
                curveTo(94.53f, 43.12f, 78.04f, 23.03f, 71.39f, 14.96f)
                curveTo(70.02f, 13.71f, 61.3f, 11.94f, 55.98f, 10.24f)
                curveTo(53.64f, 9.51f, 49.26f, 9.03f, 44.03f, 15.92f)
                curveTo(41.55f, 19.19f, 30.29f, 27.22f, 44.58f, 32.61f)
                curveTo(46.79f, 33.18f, 48.42f, 34.22f, 58.52f, 32.37f)
                curveTo(59.84f, 32.14f, 63.13f, 32.37f, 65.44f, 36.43f)
                lineTo(70.27f, 43.35f)
                curveTo(70.72f, 43.99f, 71.02f, 44.73f, 71.11f, 45.51f)
                curveTo(71.95f, 52.88f, 71.93f, 62.11f, 76.03f, 66.78f)
                curveTo(69.69f, 62.19f, 53.1f, 56.74f, 40.61f, 72.25f)
                close()
            }
        }.build()

        return _Muscle!!
    }

@Suppress("ObjectPropertyName")
private var _Muscle: ImageVector? = null
