package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.AddUser: ImageVector
    get() {
        if (_AddUser != null) {
            return _AddUser!!
        }
        _AddUser = ImageVector.Builder(
            name = "AddUser",
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
                moveTo(20f, 10f)
                verticalLineTo(13f)
                moveTo(17f, 10f)
                horizontalLineTo(20f)
                horizontalLineTo(17f)
                close()
                moveTo(23f, 10f)
                horizontalLineTo(20f)
                horizontalLineTo(23f)
                close()
                moveTo(20f, 10f)
                verticalLineTo(7f)
                verticalLineTo(10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 20f)
                verticalLineTo(19f)
                curveTo(1f, 15.134f, 4.134f, 12f, 8f, 12f)
                curveTo(11.866f, 12f, 15f, 15.134f, 15f, 19f)
                verticalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 12f)
                curveTo(10.209f, 12f, 12f, 10.209f, 12f, 8f)
                curveTo(12f, 5.791f, 10.209f, 4f, 8f, 4f)
                curveTo(5.791f, 4f, 4f, 5.791f, 4f, 8f)
                curveTo(4f, 10.209f, 5.791f, 12f, 8f, 12f)
                close()
            }
        }.build()

        return _AddUser!!
    }

@Suppress("ObjectPropertyName")
private var _AddUser: ImageVector? = null
