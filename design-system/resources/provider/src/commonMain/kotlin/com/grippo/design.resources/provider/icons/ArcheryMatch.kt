package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ArcheryMatch: ImageVector
    get() {
        if (_ArcheryMatch != null) {
            return _ArcheryMatch!!
        }
        _ArcheryMatch = ImageVector.Builder(
            name = "ArcheryMatch",
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
                moveTo(20.632f, 3.868f)
                verticalLineTo(6.697f)
                moveTo(8.611f, 15.889f)
                lineTo(20.632f, 3.868f)
                lineTo(8.611f, 15.889f)
                close()
                moveTo(8.611f, 15.889f)
                horizontalLineTo(5.783f)
                lineTo(2.954f, 18.718f)
                horizontalLineTo(5.783f)
                verticalLineTo(21.546f)
                lineTo(8.611f, 18.718f)
                verticalLineTo(15.889f)
                close()
                moveTo(20.632f, 3.868f)
                horizontalLineTo(17.803f)
                horizontalLineTo(20.632f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.368f, 3.868f)
                verticalLineTo(6.697f)
                moveTo(15.389f, 15.889f)
                lineTo(3.368f, 3.868f)
                lineTo(15.389f, 15.889f)
                close()
                moveTo(15.389f, 15.889f)
                horizontalLineTo(18.218f)
                lineTo(21.046f, 18.718f)
                horizontalLineTo(18.218f)
                verticalLineTo(21.546f)
                lineTo(15.389f, 18.718f)
                verticalLineTo(15.889f)
                close()
                moveTo(3.368f, 3.868f)
                horizontalLineTo(6.197f)
                horizontalLineTo(3.368f)
                close()
            }
        }.build()

        return _ArcheryMatch!!
    }

@Suppress("ObjectPropertyName")
private var _ArcheryMatch: ImageVector? = null
