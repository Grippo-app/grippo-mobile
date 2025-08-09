package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BounceRight: ImageVector
    get() {
        if (_BounceRight != null) {
            return _BounceRight!!
        }
        _BounceRight = ImageVector.Builder(
            name = "BounceRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 7f)
                curveTo(17.895f, 7f, 17f, 6.105f, 17f, 5f)
                curveTo(17f, 3.895f, 17.895f, 3f, 19f, 3f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                curveTo(21f, 6.105f, 20.105f, 7f, 19f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 15.5f)
                curveTo(7f, 14.5f, 9.5f, 15f, 12f, 20f)
                curveTo(12.5f, 17f, 14f, 12.5f, 15.5f, 10f)
            }
        }.build()

        return _BounceRight!!
    }

@Suppress("ObjectPropertyName")
private var _BounceRight: ImageVector? = null
