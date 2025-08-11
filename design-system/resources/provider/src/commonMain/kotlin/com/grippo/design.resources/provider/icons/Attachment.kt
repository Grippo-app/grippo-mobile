package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Attachment: ImageVector
    get() {
        if (_Attachment != null) {
            return _Attachment!!
        }
        _Attachment = ImageVector.Builder(
            name = "Attachment",
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
                moveTo(21.438f, 11.662f)
                lineTo(12.248f, 20.852f)
                curveTo(11.123f, 21.978f, 9.596f, 22.611f, 8.003f, 22.611f)
                curveTo(6.411f, 22.611f, 4.884f, 21.978f, 3.758f, 20.852f)
                curveTo(2.632f, 19.726f, 2f, 18.199f, 2f, 16.607f)
                curveTo(2f, 15.015f, 2.632f, 13.488f, 3.758f, 12.362f)
                lineTo(12.948f, 3.172f)
                curveTo(13.699f, 2.422f, 14.717f, 2f, 15.778f, 2f)
                curveTo(16.84f, 2f, 17.858f, 2.422f, 18.608f, 3.172f)
                curveTo(19.359f, 3.923f, 19.781f, 4.941f, 19.781f, 6.002f)
                curveTo(19.781f, 7.064f, 19.359f, 8.082f, 18.608f, 8.832f)
                lineTo(9.408f, 18.022f)
                curveTo(9.033f, 18.397f, 8.524f, 18.608f, 7.993f, 18.608f)
                curveTo(7.463f, 18.608f, 6.954f, 18.397f, 6.578f, 18.022f)
                curveTo(6.203f, 17.647f, 5.992f, 17.138f, 5.992f, 16.607f)
                curveTo(5.992f, 16.076f, 6.203f, 15.568f, 6.578f, 15.192f)
                lineTo(15.068f, 6.712f)
            }
        }.build()

        return _Attachment!!
    }

@Suppress("ObjectPropertyName")
private var _Attachment: ImageVector? = null
