package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.LeftRoundArrow: ImageVector
    get() {
        if (_LeftRoundArrow != null) {
            return _LeftRoundArrow!!
        }
        _LeftRoundArrow = ImageVector.Builder(
            name = "LeftRoundArrow",
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
                moveTo(6.75f, 12f)
                lineTo(9.5f, 9.25f)
                moveTo(16.75f, 12f)
                horizontalLineTo(6.75f)
                horizontalLineTo(16.75f)
                close()
                moveTo(6.75f, 12f)
                lineTo(9.5f, 14.75f)
                lineTo(6.75f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 15f)
                verticalLineTo(9f)
                curveTo(2f, 6.791f, 3.791f, 5f, 6f, 5f)
                horizontalLineTo(18f)
                curveTo(20.209f, 5f, 22f, 6.791f, 22f, 9f)
                verticalLineTo(15f)
                curveTo(22f, 17.209f, 20.209f, 19f, 18f, 19f)
                horizontalLineTo(6f)
                curveTo(3.791f, 19f, 2f, 17.209f, 2f, 15f)
                close()
            }
        }.build()

        return _LeftRoundArrow!!
    }

@Suppress("ObjectPropertyName")
private var _LeftRoundArrow: ImageVector? = null
