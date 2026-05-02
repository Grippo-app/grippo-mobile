package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Mail: ImageVector
    get() {
        if (_Mail != null) {
            return _Mail!!
        }
        _Mail = ImageVector.Builder(
            name = "Mail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Envelope body with thin V-flap stripe inset from edges.
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer envelope rectangle
                moveTo(4f, 5f)
                horizontalLineTo(20f)
                curveTo(21.105f, 5f, 22f, 5.895f, 22f, 7f)
                verticalLineTo(17f)
                curveTo(22f, 18.105f, 21.105f, 19f, 20f, 19f)
                horizontalLineTo(4f)
                curveTo(2.895f, 19f, 2f, 18.105f, 2f, 17f)
                verticalLineTo(7f)
                curveTo(2f, 5.895f, 2.895f, 5f, 4f, 5f)
                close()
                // Thin V-flap stripe — recessed from the envelope edges, ~0.6 unit thick.
                moveTo(5f, 8.4f)
                curveTo(5.2f, 8.05f, 5.65f, 7.94f, 6f, 8.14f)
                lineTo(12f, 11.55f)
                lineTo(18f, 8.14f)
                curveTo(18.35f, 7.94f, 18.8f, 8.05f, 19f, 8.4f)
                curveTo(19.2f, 8.75f, 19.08f, 9.2f, 18.73f, 9.4f)
                lineTo(12.37f, 13.02f)
                curveTo(12.14f, 13.15f, 11.86f, 13.15f, 11.63f, 13.02f)
                lineTo(5.27f, 9.4f)
                curveTo(4.92f, 9.2f, 4.8f, 8.75f, 5f, 8.4f)
                close()
            }
        }.build()

        return _Mail!!
    }

@Suppress("ObjectPropertyName")
private var _Mail: ImageVector? = null
