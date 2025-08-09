package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RemoveDatabaseScript: ImageVector
    get() {
        if (_RemoveDatabaseScript != null) {
            return _RemoveDatabaseScript!!
        }
        _RemoveDatabaseScript = ImageVector.Builder(
            name = "RemoveDatabaseScript",
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
                moveTo(6f, 13f)
                verticalLineTo(6f)
                curveTo(6f, 4.343f, 7.343f, 3f, 9f, 3f)
                horizontalLineTo(14f)
                moveTo(22f, 14f)
                verticalLineTo(8.5f)
                verticalLineTo(14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 4f)
                horizontalLineTo(22f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                horizontalLineTo(6f)
                curveTo(3.791f, 21f, 2f, 19.209f, 2f, 17f)
                curveTo(2f, 14.791f, 3.791f, 13f, 6f, 13f)
                horizontalLineTo(17f)
                horizontalLineTo(18f)
                curveTo(15.791f, 13f, 14f, 14.791f, 14f, 17f)
                curveTo(14f, 19.209f, 15.791f, 21f, 18f, 21f)
                curveTo(20.209f, 21f, 22f, 19.209f, 22f, 17f)
                verticalLineTo(14f)
            }
        }.build()

        return _RemoveDatabaseScript!!
    }

@Suppress("ObjectPropertyName")
private var _RemoveDatabaseScript: ImageVector? = null
