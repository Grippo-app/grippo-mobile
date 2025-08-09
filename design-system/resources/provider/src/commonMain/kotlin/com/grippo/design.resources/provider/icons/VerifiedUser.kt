package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.VerifiedUser: ImageVector
    get() {
        if (_VerifiedUser != null) {
            return _VerifiedUser!!
        }
        _VerifiedUser = ImageVector.Builder(
            name = "VerifiedUser",
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
                moveTo(2f, 20f)
                verticalLineTo(19f)
                curveTo(2f, 15.134f, 5.134f, 12f, 9f, 12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(15.804f, 12.313f)
                curveTo(16.445f, 11.609f, 17.554f, 11.609f, 18.196f, 12.313f)
                curveTo(18.521f, 12.67f, 18.987f, 12.863f, 19.468f, 12.84f)
                curveTo(20.42f, 12.796f, 21.204f, 13.58f, 21.16f, 14.532f)
                curveTo(21.137f, 15.013f, 21.33f, 15.479f, 21.686f, 15.804f)
                curveTo(22.391f, 16.445f, 22.391f, 17.554f, 21.686f, 18.196f)
                curveTo(21.33f, 18.521f, 21.137f, 18.987f, 21.16f, 19.468f)
                curveTo(21.204f, 20.42f, 20.42f, 21.204f, 19.468f, 21.16f)
                curveTo(18.987f, 21.137f, 18.521f, 21.33f, 18.196f, 21.686f)
                curveTo(17.554f, 22.391f, 16.445f, 22.391f, 15.804f, 21.686f)
                curveTo(15.479f, 21.33f, 15.013f, 21.137f, 14.532f, 21.16f)
                curveTo(13.58f, 21.204f, 12.796f, 20.42f, 12.84f, 19.468f)
                curveTo(12.863f, 18.987f, 12.67f, 18.521f, 12.313f, 18.196f)
                curveTo(11.609f, 17.554f, 11.609f, 16.445f, 12.313f, 15.804f)
                curveTo(12.67f, 15.479f, 12.863f, 15.013f, 12.84f, 14.532f)
                curveTo(12.796f, 13.58f, 13.58f, 12.796f, 14.532f, 12.84f)
                curveTo(15.013f, 12.863f, 15.479f, 12.67f, 15.804f, 12.313f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.363f, 17f)
                lineTo(16.455f, 18.091f)
                lineTo(18.636f, 15.909f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 12f)
                curveTo(11.209f, 12f, 13f, 10.209f, 13f, 8f)
                curveTo(13f, 5.791f, 11.209f, 4f, 9f, 4f)
                curveTo(6.791f, 4f, 5f, 5.791f, 5f, 8f)
                curveTo(5f, 10.209f, 6.791f, 12f, 9f, 12f)
                close()
            }
        }.build()

        return _VerifiedUser!!
    }

@Suppress("ObjectPropertyName")
private var _VerifiedUser: ImageVector? = null
