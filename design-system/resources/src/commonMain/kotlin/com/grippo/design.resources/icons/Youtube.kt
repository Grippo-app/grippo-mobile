package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Youtube: ImageVector
    get() {
        if (_Youtube != null) {
            return _Youtube!!
        }
        _Youtube = ImageVector.Builder(
            name = "Youtube",
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
                moveTo(22.54f, 6.42f)
                curveTo(22.421f, 5.945f, 22.179f, 5.511f, 21.839f, 5.159f)
                curveTo(21.498f, 4.808f, 21.071f, 4.553f, 20.6f, 4.42f)
                curveTo(18.88f, 4f, 12f, 4f, 12f, 4f)
                curveTo(12f, 4f, 5.12f, 4f, 3.4f, 4.46f)
                curveTo(2.929f, 4.593f, 2.502f, 4.848f, 2.161f, 5.199f)
                curveTo(1.821f, 5.551f, 1.579f, 5.985f, 1.46f, 6.46f)
                curveTo(1.145f, 8.206f, 0.991f, 9.976f, 1f, 11.75f)
                curveTo(0.989f, 13.537f, 1.143f, 15.321f, 1.46f, 17.08f)
                curveTo(1.591f, 17.54f, 1.838f, 17.958f, 2.178f, 18.295f)
                curveTo(2.518f, 18.631f, 2.939f, 18.874f, 3.4f, 19f)
                curveTo(5.12f, 19.46f, 12f, 19.46f, 12f, 19.46f)
                curveTo(12f, 19.46f, 18.88f, 19.46f, 20.6f, 19f)
                curveTo(21.071f, 18.867f, 21.498f, 18.612f, 21.839f, 18.261f)
                curveTo(22.179f, 17.909f, 22.421f, 17.475f, 22.54f, 17f)
                curveTo(22.852f, 15.268f, 23.006f, 13.51f, 23f, 11.75f)
                curveTo(23.011f, 9.963f, 22.857f, 8.179f, 22.54f, 6.42f)
                verticalLineTo(6.42f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.75f, 15.02f)
                lineTo(15.5f, 11.75f)
                lineTo(9.75f, 8.48f)
                verticalLineTo(15.02f)
                close()
            }
        }.build()

        return _Youtube!!
    }

@Suppress("ObjectPropertyName")
private var _Youtube: ImageVector? = null
