package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Tune: ImageVector
    get() {
        if (_Tune != null) {
            return _Tune!!
        }
        _Tune = ImageVector.Builder(
            name = "Tune",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top rail with knob (knob on right) — pill rail + knob unioned visually,
            // hollow inside the knob via EvenOdd to keep "thumb-on-rail" reading.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Pill rail
                moveTo(4.25f, 5.25f)
                horizontalLineTo(19.75f)
                curveTo(20.44f, 5.25f, 21f, 5.81f, 21f, 6.5f)
                curveTo(21f, 7.19f, 20.44f, 7.75f, 19.75f, 7.75f)
                horizontalLineTo(4.25f)
                curveTo(3.56f, 7.75f, 3f, 7.19f, 3f, 6.5f)
                curveTo(3f, 5.81f, 3.56f, 5.25f, 4.25f, 5.25f)
                close()
                // Knob ring (cutout inside)
                moveTo(16f, 5f)
                curveTo(15.172f, 5f, 14.5f, 5.672f, 14.5f, 6.5f)
                curveTo(14.5f, 7.328f, 15.172f, 8f, 16f, 8f)
                curveTo(16.828f, 8f, 17.5f, 7.328f, 17.5f, 6.5f)
                curveTo(17.5f, 5.672f, 16.828f, 5f, 16f, 5f)
                close()
            }
            // Top knob solid disc
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(16f, 4f)
                curveTo(17.381f, 4f, 18.5f, 5.119f, 18.5f, 6.5f)
                curveTo(18.5f, 7.881f, 17.381f, 9f, 16f, 9f)
                curveTo(14.619f, 9f, 13.5f, 7.881f, 13.5f, 6.5f)
                curveTo(13.5f, 5.119f, 14.619f, 4f, 16f, 4f)
                close()
            }
            // Middle rail (knob on left)
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(4.25f, 10.75f)
                horizontalLineTo(19.75f)
                curveTo(20.44f, 10.75f, 21f, 11.31f, 21f, 12f)
                curveTo(21f, 12.69f, 20.44f, 13.25f, 19.75f, 13.25f)
                horizontalLineTo(4.25f)
                curveTo(3.56f, 13.25f, 3f, 12.69f, 3f, 12f)
                curveTo(3f, 11.31f, 3.56f, 10.75f, 4.25f, 10.75f)
                close()
                moveTo(8f, 10.5f)
                curveTo(7.172f, 10.5f, 6.5f, 11.172f, 6.5f, 12f)
                curveTo(6.5f, 12.828f, 7.172f, 13.5f, 8f, 13.5f)
                curveTo(8.828f, 13.5f, 9.5f, 12.828f, 9.5f, 12f)
                curveTo(9.5f, 11.172f, 8.828f, 10.5f, 8f, 10.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(8f, 9.5f)
                curveTo(9.381f, 9.5f, 10.5f, 10.619f, 10.5f, 12f)
                curveTo(10.5f, 13.381f, 9.381f, 14.5f, 8f, 14.5f)
                curveTo(6.619f, 14.5f, 5.5f, 13.381f, 5.5f, 12f)
                curveTo(5.5f, 10.619f, 6.619f, 9.5f, 8f, 9.5f)
                close()
            }
            // Bottom rail (knob centre-right)
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(4.25f, 16.25f)
                horizontalLineTo(19.75f)
                curveTo(20.44f, 16.25f, 21f, 16.81f, 21f, 17.5f)
                curveTo(21f, 18.19f, 20.44f, 18.75f, 19.75f, 18.75f)
                horizontalLineTo(4.25f)
                curveTo(3.56f, 18.75f, 3f, 18.19f, 3f, 17.5f)
                curveTo(3f, 16.81f, 3.56f, 16.25f, 4.25f, 16.25f)
                close()
                moveTo(14f, 16f)
                curveTo(13.172f, 16f, 12.5f, 16.672f, 12.5f, 17.5f)
                curveTo(12.5f, 18.328f, 13.172f, 19f, 14f, 19f)
                curveTo(14.828f, 19f, 15.5f, 18.328f, 15.5f, 17.5f)
                curveTo(15.5f, 16.672f, 14.828f, 16f, 14f, 16f)
                close()
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(14f, 15f)
                curveTo(15.381f, 15f, 16.5f, 16.119f, 16.5f, 17.5f)
                curveTo(16.5f, 18.881f, 15.381f, 20f, 14f, 20f)
                curveTo(12.619f, 20f, 11.5f, 18.881f, 11.5f, 17.5f)
                curveTo(11.5f, 16.119f, 12.619f, 15f, 14f, 15f)
                close()
            }
        }.build()

        return _Tune!!
    }

@Suppress("ObjectPropertyName")
private var _Tune: ImageVector? = null
