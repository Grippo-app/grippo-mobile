package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

/**
 * Beginner experience tier — sprout / seedling.
 * Symbolises "new beginning, fresh start" without any negative connotation.
 */
public val AppIcon.ExperienceBeginner: ImageVector
    get() {
        if (_ExperienceBeginner != null) {
            return _ExperienceBeginner!!
        }
        _ExperienceBeginner = ImageVector.Builder(
            name = "ExperienceBeginner",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Stem — narrow vertical bar from base to centre.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(11.25f, 13f)
                horizontalLineTo(12.75f)
                curveTo(13.164f, 13f, 13.5f, 13.336f, 13.5f, 13.75f)
                verticalLineTo(21.25f)
                curveTo(13.5f, 21.664f, 13.164f, 22f, 12.75f, 22f)
                horizontalLineTo(11.25f)
                curveTo(10.836f, 22f, 10.5f, 21.664f, 10.5f, 21.25f)
                verticalLineTo(13.75f)
                curveTo(10.5f, 13.336f, 10.836f, 13f, 11.25f, 13f)
                close()
            }
            // Right leaf — large rounded teardrop curving up-right from stem.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 13f)
                curveTo(12f, 9f, 15f, 5f, 20f, 5f)
                curveTo(20.552f, 5f, 21f, 5.448f, 21f, 6f)
                curveTo(21f, 11f, 17f, 14f, 13f, 14f)
                curveTo(12.448f, 14f, 12f, 13.552f, 12f, 13f)
                close()
            }
            // Left leaf — smaller, lower, curving up-left.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 16f)
                curveTo(12f, 13f, 9.5f, 10f, 5f, 10f)
                curveTo(4.448f, 10f, 4f, 10.448f, 4f, 11f)
                curveTo(4f, 14.5f, 7.5f, 17f, 11f, 17f)
                curveTo(11.552f, 17f, 12f, 16.552f, 12f, 16f)
                close()
            }
        }.build()

        return _ExperienceBeginner!!
    }

@Suppress("ObjectPropertyName")
private var _ExperienceBeginner: ImageVector? = null
