package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.QuestionCircle: ImageVector
    get() {
        if (_QuestionCircle != null) {
            return _QuestionCircle!!
        }
        _QuestionCircle = ImageVector.Builder(
            name = "QuestionCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Filled circle with question mark cut out via EvenOdd
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer circle
                moveTo(12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                close()
                // Question mark hook (top loop) — cut-out
                moveTo(12f, 6f)
                curveTo(9.791f, 6f, 8f, 7.791f, 8f, 10f)
                horizontalLineTo(10f)
                curveTo(10f, 8.895f, 10.895f, 8f, 12f, 8f)
                curveTo(13.105f, 8f, 14f, 8.895f, 14f, 10f)
                curveTo(14f, 10.5f, 13.7f, 10.9f, 13.2f, 11.3f)
                curveTo(12.4f, 11.9f, 11f, 12.7f, 11f, 14.5f)
                horizontalLineTo(13f)
                curveTo(13f, 13.7f, 13.6f, 13.4f, 14.4f, 12.7f)
                curveTo(15.4f, 12f, 16f, 11.1f, 16f, 10f)
                curveTo(16f, 7.791f, 14.209f, 6f, 12f, 6f)
                close()
                // Question mark dot — cut-out
                moveTo(12f, 16f)
                curveTo(11.31f, 16f, 10.75f, 16.56f, 10.75f, 17.25f)
                curveTo(10.75f, 17.94f, 11.31f, 18.5f, 12f, 18.5f)
                curveTo(12.69f, 18.5f, 13.25f, 17.94f, 13.25f, 17.25f)
                curveTo(13.25f, 16.56f, 12.69f, 16f, 12f, 16f)
                close()
            }
        }.build()

        return _QuestionCircle!!
    }

@Suppress("ObjectPropertyName")
private var _QuestionCircle: ImageVector? = null
