package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

/**
 * Advanced experience tier — loaded barbell.
 * Symbolises "you've moved past bodyweight, lifting real weight, technique is solid"
 * — concrete fitness escalation toward Pro.
 *
 * Composed of: central bar + 2 inner collars + 2 outer weight plates per side.
 */
public val AppIcon.ExperienceAdvanced: ImageVector
    get() {
        if (_ExperienceAdvanced != null) {
            return _ExperienceAdvanced!!
        }
        _ExperienceAdvanced = ImageVector.Builder(
            name = "ExperienceAdvanced",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Central bar — pill shape across the middle.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(8f, 11f)
                horizontalLineTo(16f)
                curveTo(16.552f, 11f, 17f, 11.448f, 17f, 12f)
                curveTo(17f, 12.552f, 16.552f, 13f, 16f, 13f)
                horizontalLineTo(8f)
                curveTo(7.448f, 13f, 7f, 12.552f, 7f, 12f)
                curveTo(7f, 11.448f, 7.448f, 11f, 8f, 11f)
                close()
            }
            // Left inner collar (small weight near bar end).
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(5f, 9.5f)
                horizontalLineTo(7f)
                curveTo(7.276f, 9.5f, 7.5f, 9.724f, 7.5f, 10f)
                verticalLineTo(14f)
                curveTo(7.5f, 14.276f, 7.276f, 14.5f, 7f, 14.5f)
                horizontalLineTo(5f)
                curveTo(4.724f, 14.5f, 4.5f, 14.276f, 4.5f, 14f)
                verticalLineTo(10f)
                curveTo(4.5f, 9.724f, 4.724f, 9.5f, 5f, 9.5f)
                close()
            }
            // Left outer weight plate (taller, rounded rectangle).
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(2f, 7.5f)
                horizontalLineTo(4f)
                curveTo(4.552f, 7.5f, 5f, 7.948f, 5f, 8.5f)
                verticalLineTo(15.5f)
                curveTo(5f, 16.052f, 4.552f, 16.5f, 4f, 16.5f)
                horizontalLineTo(2f)
                curveTo(1.448f, 16.5f, 1f, 16.052f, 1f, 15.5f)
                verticalLineTo(8.5f)
                curveTo(1f, 7.948f, 1.448f, 7.5f, 2f, 7.5f)
                close()
            }
            // Right inner collar.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(17f, 9.5f)
                horizontalLineTo(19f)
                curveTo(19.276f, 9.5f, 19.5f, 9.724f, 19.5f, 10f)
                verticalLineTo(14f)
                curveTo(19.5f, 14.276f, 19.276f, 14.5f, 19f, 14.5f)
                horizontalLineTo(17f)
                curveTo(16.724f, 14.5f, 16.5f, 14.276f, 16.5f, 14f)
                verticalLineTo(10f)
                curveTo(16.5f, 9.724f, 16.724f, 9.5f, 17f, 9.5f)
                close()
            }
            // Right outer weight plate.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(20f, 7.5f)
                horizontalLineTo(22f)
                curveTo(22.552f, 7.5f, 23f, 7.948f, 23f, 8.5f)
                verticalLineTo(15.5f)
                curveTo(23f, 16.052f, 22.552f, 16.5f, 22f, 16.5f)
                horizontalLineTo(20f)
                curveTo(19.448f, 16.5f, 19f, 16.052f, 19f, 15.5f)
                verticalLineTo(8.5f)
                curveTo(19f, 7.948f, 19.448f, 7.5f, 20f, 7.5f)
                close()
            }
        }.build()

        return _ExperienceAdvanced!!
    }

@Suppress("ObjectPropertyName")
private var _ExperienceAdvanced: ImageVector? = null
