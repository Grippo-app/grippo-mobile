package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Edit2: ImageVector
    get() {
        if (_Edit2 != null) {
            return _Edit2!!
        }
        _Edit2 = ImageVector.Builder(
            name = "Edit2",
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
                moveTo(17f, 3f)
                curveTo(17.263f, 2.737f, 17.574f, 2.529f, 17.918f, 2.387f)
                curveTo(18.261f, 2.245f, 18.629f, 2.172f, 19f, 2.172f)
                curveTo(19.371f, 2.172f, 19.739f, 2.245f, 20.082f, 2.387f)
                curveTo(20.426f, 2.529f, 20.737f, 2.737f, 21f, 3f)
                curveTo(21.263f, 3.263f, 21.471f, 3.574f, 21.613f, 3.918f)
                curveTo(21.755f, 4.261f, 21.828f, 4.629f, 21.828f, 5f)
                curveTo(21.828f, 5.371f, 21.755f, 5.739f, 21.613f, 6.082f)
                curveTo(21.471f, 6.426f, 21.263f, 6.737f, 21f, 7f)
                lineTo(7.5f, 20.5f)
                lineTo(2f, 22f)
                lineTo(3.5f, 16.5f)
                lineTo(17f, 3f)
                close()
            }
        }.build()

        return _Edit2!!
    }

@Suppress("ObjectPropertyName")
private var _Edit2: ImageVector? = null
