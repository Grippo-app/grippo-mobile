package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Gauge: ImageVector
    get() {
        if (_Gauge != null) {
            return _Gauge!!
        }
        _Gauge = ImageVector.Builder(
            name = "Gauge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Speedometer half-ring (outer arc + inner cut → ~3 unit band).
            path(
                fill = SolidColor(Color(0xFF33363F)),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outer half-disc — flat baseline at y = 18
                moveTo(2.5f, 18f)
                curveTo(2.5f, 12.753f, 6.753f, 8.5f, 12f, 8.5f)
                curveTo(17.247f, 8.5f, 21.5f, 12.753f, 21.5f, 18f)
                lineTo(2.5f, 18f)
                close()
                // Inner half-disc cut-out
                moveTo(5.5f, 18f)
                curveTo(5.5f, 14.41f, 8.41f, 11.5f, 12f, 11.5f)
                curveTo(15.59f, 11.5f, 18.5f, 14.41f, 18.5f, 18f)
                lineTo(5.5f, 18f)
                close()
            }
            // Filled tapered needle — clean triangle from pivot to upper-right.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(16.7f, 12.3f)
                lineTo(11.3f, 17f)
                lineTo(12.7f, 18.7f)
                lineTo(17.7f, 13.3f)
                close()
            }
            // Pivot dot at base — solid circle anchoring the needle.
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 16f)
                curveTo(13.105f, 16f, 14f, 16.895f, 14f, 18f)
                curveTo(14f, 19.105f, 13.105f, 20f, 12f, 20f)
                curveTo(10.895f, 20f, 10f, 19.105f, 10f, 18f)
                curveTo(10f, 16.895f, 10.895f, 16f, 12f, 16f)
                close()
            }
        }.build()

        return _Gauge!!
    }

@Suppress("ObjectPropertyName")
private var _Gauge: ImageVector? = null
