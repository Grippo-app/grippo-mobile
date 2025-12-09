package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Height: ImageVector
    get() {
        if (_Height != null) {
            return _Height!!
        }
        _Height = ImageVector.Builder(
            name = "Height",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF222222))) {
                moveTo(10.247f, 17.002f)
                lineTo(11.008f, 16.567f)
                curveTo(11.492f, 16.29f, 11.734f, 16.152f, 12f, 16.152f)
                curveTo(12.266f, 16.152f, 12.508f, 16.29f, 12.992f, 16.567f)
                lineTo(12.992f, 16.567f)
                lineTo(13.753f, 17.002f)
                curveTo(15.92f, 18.24f, 17.004f, 18.859f, 17.541f, 18.376f)
                curveTo(18.079f, 17.894f, 17.578f, 16.75f, 16.578f, 14.464f)
                lineTo(13.832f, 8.188f)
                curveTo(13.047f, 6.393f, 12.654f, 5.495f, 12f, 5.495f)
                curveTo(11.346f, 5.495f, 10.953f, 6.393f, 10.168f, 8.188f)
                lineTo(7.422f, 14.464f)
                curveTo(6.422f, 16.75f, 5.922f, 17.894f, 6.459f, 18.376f)
                curveTo(6.996f, 18.859f, 8.08f, 18.24f, 10.247f, 17.002f)
                close()
            }
        }.build()

        return _Height!!
    }

@Suppress("ObjectPropertyName")
private var _Height: ImageVector? = null

