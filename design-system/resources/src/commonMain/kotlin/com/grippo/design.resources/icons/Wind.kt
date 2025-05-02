package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Wind: ImageVector
    get() {
        if (_Wind != null) {
            return _Wind!!
        }
        _Wind = ImageVector.Builder(
            name = "Wind",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.73f, 7.73f)
                curveTo(18.021f, 7.44f, 18.379f, 7.226f, 18.772f, 7.107f)
                curveTo(19.165f, 6.988f, 19.582f, 6.968f, 19.985f, 7.048f)
                curveTo(20.387f, 7.129f, 20.764f, 7.307f, 21.082f, 7.568f)
                curveTo(21.399f, 7.828f, 21.648f, 8.163f, 21.805f, 8.543f)
                curveTo(21.962f, 8.922f, 22.024f, 9.335f, 21.984f, 9.743f)
                curveTo(21.944f, 10.152f, 21.803f, 10.545f, 21.576f, 10.887f)
                curveTo(21.348f, 11.229f, 21.039f, 11.509f, 20.677f, 11.703f)
                curveTo(20.315f, 11.897f, 19.911f, 11.999f, 19.5f, 12f)
                horizontalLineTo(2f)
                moveTo(9.59f, 4.59f)
                curveTo(9.822f, 4.356f, 10.109f, 4.184f, 10.424f, 4.087f)
                curveTo(10.739f, 3.991f, 11.073f, 3.974f, 11.396f, 4.038f)
                curveTo(11.719f, 4.102f, 12.022f, 4.245f, 12.277f, 4.454f)
                curveTo(12.531f, 4.663f, 12.731f, 4.931f, 12.857f, 5.236f)
                curveTo(12.983f, 5.54f, 13.031f, 5.871f, 12.998f, 6.199f)
                curveTo(12.966f, 6.527f, 12.852f, 6.842f, 12.669f, 7.115f)
                curveTo(12.485f, 7.389f, 12.236f, 7.613f, 11.945f, 7.767f)
                curveTo(11.654f, 7.921f, 11.33f, 8.001f, 11f, 8f)
                horizontalLineTo(2f)
                lineTo(9.59f, 4.59f)
                close()
                moveTo(12.59f, 19.41f)
                curveTo(12.822f, 19.644f, 13.109f, 19.816f, 13.424f, 19.913f)
                curveTo(13.739f, 20.009f, 14.073f, 20.026f, 14.396f, 19.962f)
                curveTo(14.719f, 19.898f, 15.022f, 19.755f, 15.277f, 19.546f)
                curveTo(15.531f, 19.337f, 15.731f, 19.069f, 15.857f, 18.764f)
                curveTo(15.983f, 18.46f, 16.031f, 18.129f, 15.998f, 17.801f)
                curveTo(15.966f, 17.473f, 15.852f, 17.158f, 15.669f, 16.885f)
                curveTo(15.485f, 16.611f, 15.236f, 16.387f, 14.945f, 16.233f)
                curveTo(14.654f, 16.079f, 14.33f, 15.999f, 14f, 16f)
                horizontalLineTo(2f)
                lineTo(12.59f, 19.41f)
                close()
            }
        }.build()

        return _Wind!!
    }

@Suppress("ObjectPropertyName")
private var _Wind: ImageVector? = null
