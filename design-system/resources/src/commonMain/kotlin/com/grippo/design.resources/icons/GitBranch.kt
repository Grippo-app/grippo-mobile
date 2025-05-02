package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.GitBranch: ImageVector
    get() {
        if (_GitBranch != null) {
            return _GitBranch!!
        }
        _GitBranch = ImageVector.Builder(
            name = "GitBranch",
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
                moveTo(6f, 3f)
                verticalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 9f)
                curveTo(19.657f, 9f, 21f, 7.657f, 21f, 6f)
                curveTo(21f, 4.343f, 19.657f, 3f, 18f, 3f)
                curveTo(16.343f, 3f, 15f, 4.343f, 15f, 6f)
                curveTo(15f, 7.657f, 16.343f, 9f, 18f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 21f)
                curveTo(7.657f, 21f, 9f, 19.657f, 9f, 18f)
                curveTo(9f, 16.343f, 7.657f, 15f, 6f, 15f)
                curveTo(4.343f, 15f, 3f, 16.343f, 3f, 18f)
                curveTo(3f, 19.657f, 4.343f, 21f, 6f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 9f)
                curveTo(18f, 11.387f, 17.052f, 13.676f, 15.364f, 15.364f)
                curveTo(13.676f, 17.052f, 11.387f, 18f, 9f, 18f)
            }
        }.build()

        return _GitBranch!!
    }

@Suppress("ObjectPropertyName")
private var _GitBranch: ImageVector? = null
