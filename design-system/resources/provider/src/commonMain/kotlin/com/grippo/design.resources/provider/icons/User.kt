package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.User: ImageVector
    get() {
        if (_User != null) {
            return _User!!
        }
        _User = ImageVector.Builder(
            name = "User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head — solid filled circle (radius 4 at center 12, 7).
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 3f)
                curveTo(14.209f, 3f, 16f, 4.791f, 16f, 7f)
                curveTo(16f, 9.209f, 14.209f, 11f, 12f, 11f)
                curveTo(9.791f, 11f, 8f, 9.209f, 8f, 7f)
                curveTo(8f, 4.791f, 9.791f, 3f, 12f, 3f)
                close()
            }
            // Shoulders / torso — rounded "wing" silhouette underneath the head.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(11.643f, 13f)
                horizontalLineTo(12.357f)
                curveTo(15.69f, 13f, 19.014f, 14.829f, 19.768f, 18.045f)
                curveTo(19.926f, 18.717f, 20.052f, 19.422f, 20.122f, 20.135f)
                curveTo(20.214f, 21.066f, 19.471f, 21.825f, 18.535f, 21.825f)
                horizontalLineTo(5.465f)
                curveTo(4.529f, 21.825f, 3.786f, 21.066f, 3.878f, 20.135f)
                curveTo(3.948f, 19.422f, 4.074f, 18.717f, 4.232f, 18.045f)
                curveTo(4.986f, 14.829f, 8.31f, 13f, 11.643f, 13f)
                close()
            }
        }.build()

        return _User!!
    }

@Suppress("ObjectPropertyName")
private var _User: ImageVector? = null
