package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Apple: ImageVector
    get() {
        if (_Apple != null) {
            return _Apple!!
        }
        _Apple = ImageVector.Builder(
            name = "Apple",
            defaultWidth = 800.dp,
            defaultHeight = 800.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(248.64f, 123.48f)
                curveToRelative(-5.45f, -29.71f, 8.6f, -60.28f, 25.52f, -80.89f)
                curveToRelative(18.65f, -22.74f, 50.64f, -40.17f, 77.99f, -42.09f)
                curveToRelative(4.62f, 31.15f, -8.09f, 61.5f, -24.83f, 82.96f)
                curveTo(309.37f, 106.53f, 278.51f, 124.41f, 248.64f, 123.48f)
                close()
                moveTo(409.03f, 231.13f)
                curveToRelative(8.46f, -23.61f, 25.22f, -44.85f, 51.23f, -59.17f)
                curveToRelative(-26.28f, -32.79f, -63.17f, -51.83f, -97.99f, -51.83f)
                curveToRelative(-46.06f, 0f, -65.54f, 21.95f, -97.54f, 21.95f)
                curveToRelative(-32.96f, 0f, -57.97f, -21.95f, -97.87f, -21.95f)
                curveToRelative(-39.13f, 0f, -80.78f, 23.85f, -107.19f, 64.58f)
                curveToRelative(-9.71f, 15.06f, -16.29f, 33.76f, -19.88f, 54.59f)
                curveToRelative(-9.96f, 58.44f, 4.92f, 134.56f, 49.28f, 202.14f)
                curveToRelative(21.57f, 32.8f, 50.32f, 69.74f, 87.88f, 70.06f)
                curveToRelative(33.46f, 0.33f, 42.95f, -21.39f, 88.25f, -21.62f)
                curveToRelative(45.36f, -0.26f, 53.96f, 21.84f, 87.37f, 21.52f)
                curveToRelative(37.57f, -0.32f, 67.91f, -41.2f, 89.48f, -73.99f)
                curveToRelative(15.36f, -23.53f, 21.17f, -35.42f, 33.11f, -62.02f)
                curveTo(414.43f, 352.49f, 389.46f, 285.57f, 409.03f, 231.13f)
                close()
            }
        }.build()

        return _Apple!!
    }

@Suppress("ObjectPropertyName")
private var _Apple: ImageVector? = null