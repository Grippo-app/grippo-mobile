package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

/**
 * Intermediate experience tier — lightning bolt.
 * Symbolises "energy, momentum, you've got the spark" — positive impulse,
 * not "middle of the pack".
 */
public val AppIcon.ExperienceIntermediate: ImageVector
    get() {
        if (_ExperienceIntermediate != null) {
            return _ExperienceIntermediate!!
        }
        _ExperienceIntermediate = ImageVector.Builder(
            name = "ExperienceIntermediate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Z-shape lightning bolt — clean classic silhouette.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(14f, 2f)
                lineTo(13f, 9f)
                horizontalLineTo(18f)
                curveTo(18.842f, 9f, 19.314f, 9.969f, 18.8f, 10.633f)
                lineTo(9.5f, 22.5f)
                curveTo(8.92f, 23.245f, 7.747f, 22.685f, 7.969f, 21.78f)
                lineTo(10f, 14f)
                horizontalLineTo(6f)
                curveTo(5.158f, 14f, 4.686f, 13.031f, 5.2f, 12.367f)
                lineTo(13.214f, 2.378f)
                curveTo(13.539f, 1.962f, 14.078f, 1.844f, 14f, 2f)
                close()
            }
        }.build()

        return _ExperienceIntermediate!!
    }

@Suppress("ObjectPropertyName")
private var _ExperienceIntermediate: ImageVector? = null
