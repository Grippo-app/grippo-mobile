package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Dumbbell: ImageVector
    get() {
        if (_Dumbbell != null) {
            return _Dumbbell!!
        }
        _Dumbbell = ImageVector.Builder(
            name = "Dumbbell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Left big weight (rounded)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(2.5f, 10f)
                curveTo(2.5f, 9.172f, 3.172f, 8.5f, 4f, 8.5f)
                curveTo(4.828f, 8.5f, 5.5f, 9.172f, 5.5f, 10f)
                verticalLineTo(14f)
                curveTo(5.5f, 14.828f, 4.828f, 15.5f, 4f, 15.5f)
                curveTo(3.172f, 15.5f, 2.5f, 14.828f, 2.5f, 14f)
                close()
            }
            // Left small weight (rounded)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(6f, 8f)
                curveTo(6f, 7.172f, 6.672f, 6.5f, 7.5f, 6.5f)
                curveTo(8.328f, 6.5f, 9f, 7.172f, 9f, 8f)
                verticalLineTo(16f)
                curveTo(9f, 16.828f, 8.328f, 17.5f, 7.5f, 17.5f)
                curveTo(6.672f, 17.5f, 6f, 16.828f, 6f, 16f)
                close()
            }
            // Bar (rounded)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(9f, 11.25f)
                curveTo(9f, 10.836f, 9.336f, 10.5f, 9.75f, 10.5f)
                horizontalLineTo(14.25f)
                curveTo(14.664f, 10.5f, 15f, 10.836f, 15f, 11.25f)
                verticalLineTo(12.75f)
                curveTo(15f, 13.164f, 14.664f, 13.5f, 14.25f, 13.5f)
                horizontalLineTo(9.75f)
                curveTo(9.336f, 13.5f, 9f, 13.164f, 9f, 12.75f)
                close()
            }
            // Right small weight (rounded)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(15f, 8f)
                curveTo(15f, 7.172f, 15.672f, 6.5f, 16.5f, 6.5f)
                curveTo(17.328f, 6.5f, 18f, 7.172f, 18f, 8f)
                verticalLineTo(16f)
                curveTo(18f, 16.828f, 17.328f, 17.5f, 16.5f, 17.5f)
                curveTo(15.672f, 17.5f, 15f, 16.828f, 15f, 16f)
                close()
            }
            // Right big weight (rounded)
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(18.5f, 10f)
                curveTo(18.5f, 9.172f, 19.172f, 8.5f, 20f, 8.5f)
                curveTo(20.828f, 8.5f, 21.5f, 9.172f, 21.5f, 10f)
                verticalLineTo(14f)
                curveTo(21.5f, 14.828f, 20.828f, 15.5f, 20f, 15.5f)
                curveTo(19.172f, 15.5f, 18.5f, 14.828f, 18.5f, 14f)
                close()
            }
        }.build()

        return _Dumbbell!!
    }

@Suppress("ObjectPropertyName")
private var _Dumbbell: ImageVector? = null
