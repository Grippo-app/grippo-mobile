package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MailOpened: ImageVector
    get() {
        if (_MailOpened != null) {
            return _MailOpened!!
        }
        _MailOpened = ImageVector.Builder(
            name = "MailOpened",
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
                moveTo(7f, 12f)
                lineTo(12f, 15.5f)
                lineTo(17f, 12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 20f)
                verticalLineTo(9.132f)
                curveTo(2f, 8.43f, 2.369f, 7.779f, 2.971f, 7.417f)
                lineTo(10.971f, 2.617f)
                curveTo(11.604f, 2.237f, 12.396f, 2.237f, 13.029f, 2.617f)
                lineTo(21.029f, 7.417f)
                curveTo(21.631f, 7.779f, 22f, 8.43f, 22f, 9.132f)
                verticalLineTo(20f)
                curveTo(22f, 21.104f, 21.105f, 22f, 20f, 22f)
                horizontalLineTo(4f)
                curveTo(2.895f, 22f, 2f, 21.104f, 2f, 20f)
                close()
            }
        }.build()

        return _MailOpened!!
    }

@Suppress("ObjectPropertyName")
private var _MailOpened: ImageVector? = null
