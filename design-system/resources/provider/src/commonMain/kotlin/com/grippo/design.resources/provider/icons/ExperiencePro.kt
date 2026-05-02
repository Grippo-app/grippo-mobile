package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

/**
 * Pro experience tier — trophy cup with handles.
 * Symbolises "mastery, top tier" — positive culmination of the experience scale.
 */
public val AppIcon.ExperiencePro: ImageVector
    get() {
        if (_ExperiencePro != null) {
            return _ExperiencePro!!
        }
        _ExperiencePro = ImageVector.Builder(
            name = "ExperiencePro",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Cup body with rounded rim, soft cup, and handle holes via EvenOdd.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Top rim
                moveTo(6f, 3f)
                horizontalLineTo(18f)
                curveTo(18.552f, 3f, 19f, 3.448f, 19f, 4f)
                verticalLineTo(5f)
                horizontalLineTo(20f)
                curveTo(21.105f, 5f, 22f, 5.895f, 22f, 7f)
                verticalLineTo(9f)
                curveTo(22f, 11.209f, 20.209f, 13f, 18f, 13f)
                horizontalLineTo(17.4f)
                curveTo(16.5f, 14.95f, 14.7f, 16.4f, 13f, 16.85f)
                verticalLineTo(18.5f)
                curveTo(13f, 18.776f, 13.224f, 19f, 13.5f, 19f)
                horizontalLineTo(15.5f)
                curveTo(16.052f, 19f, 16.5f, 19.448f, 16.5f, 20f)
                curveTo(16.5f, 20.552f, 16.052f, 21f, 15.5f, 21f)
                horizontalLineTo(8.5f)
                curveTo(7.948f, 21f, 7.5f, 20.552f, 7.5f, 20f)
                curveTo(7.5f, 19.448f, 7.948f, 19f, 8.5f, 19f)
                horizontalLineTo(10.5f)
                curveTo(10.776f, 19f, 11f, 18.776f, 11f, 18.5f)
                verticalLineTo(16.85f)
                curveTo(9.3f, 16.4f, 7.5f, 14.95f, 6.6f, 13f)
                horizontalLineTo(6f)
                curveTo(3.791f, 13f, 2f, 11.209f, 2f, 9f)
                verticalLineTo(7f)
                curveTo(2f, 5.895f, 2.895f, 5f, 4f, 5f)
                horizontalLineTo(5f)
                verticalLineTo(4f)
                curveTo(5f, 3.448f, 5.448f, 3f, 6f, 3f)
                close()
                // Left handle hole
                moveTo(4.5f, 7f)
                verticalLineTo(9f)
                curveTo(4.5f, 10.105f, 5.395f, 11f, 6.5f, 11f)
                verticalLineTo(7f)
                horizontalLineTo(4.5f)
                close()
                // Right handle hole
                moveTo(19.5f, 7f)
                verticalLineTo(9f)
                curveTo(19.5f, 10.105f, 18.605f, 11f, 17.5f, 11f)
                verticalLineTo(7f)
                horizontalLineTo(19.5f)
                close()
            }
        }.build()

        return _ExperiencePro!!
    }

@Suppress("ObjectPropertyName")
private var _ExperiencePro: ImageVector? = null
