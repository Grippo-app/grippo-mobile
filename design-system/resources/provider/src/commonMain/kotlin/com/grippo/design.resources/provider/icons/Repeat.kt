package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Repeat: ImageVector
    get() {
        if (_Repeat != null) {
            return _Repeat!!
        }
        _Repeat = ImageVector.Builder(
            name = "Repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // 270° open arc — refresh-style. Starts at 12 o'clock, sweeps clockwise
            // through 3 → 6 → 9 o'clock. Open at the top so the arrow can sit in the gap.
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 4f)
                curveTo(16.418f, 4f, 20f, 7.582f, 20f, 12f)
                curveTo(20f, 16.418f, 16.418f, 20f, 12f, 20f)
                curveTo(7.582f, 20f, 4f, 16.418f, 4f, 12f)
            }
            // Arrow head — triangle pointing LEFT, with all three corners softly rounded.
            // Back-edge sits at the arc's start (12, 4) so they merge cleanly.
            path(fill = SolidColor(Color(0xFF33363F))) {
                // Top-back corner (rounded)
                moveTo(13f, 1.5f)
                curveTo(13.55f, 1.5f, 14f, 1.95f, 14f, 2.5f)
                // Back side going down
                verticalLineTo(5.5f)
                // Bottom-back corner (rounded)
                curveTo(14f, 6.05f, 13.55f, 6.5f, 13f, 6.5f)
                // Diagonal down-left to tip
                lineTo(8.5f, 4.3f)
                // Tip corner (rounded — small arc)
                curveTo(8f, 4.05f, 8f, 3.95f, 8.5f, 3.7f)
                // Diagonal up-left back to top-back
                close()
            }
        }.build()

        return _Repeat!!
    }

@Suppress("ObjectPropertyName")
private var _Repeat: ImageVector? = null
