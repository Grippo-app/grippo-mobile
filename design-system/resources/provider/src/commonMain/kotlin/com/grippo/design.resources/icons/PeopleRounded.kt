package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PeopleRounded: ImageVector
    get() {
        if (_PeopleRounded != null) {
            return _PeopleRounded!!
        }
        _PeopleRounded = ImageVector.Builder(
            name = "PeopleRounded",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 16f)
                verticalLineTo(8f)
                curveTo(3f, 5.239f, 5.239f, 3f, 8f, 3f)
                horizontalLineTo(16f)
                curveTo(18.761f, 3f, 21f, 5.239f, 21f, 8f)
                verticalLineTo(16f)
                curveTo(21f, 18.761f, 18.761f, 21f, 16f, 21f)
                horizontalLineTo(8f)
                curveTo(5.239f, 21f, 3f, 18.761f, 3f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 14.5f)
                curveTo(16.5f, 14.5f, 15f, 16.5f, 12f, 16.5f)
                curveTo(9f, 16.5f, 7.5f, 14.5f, 7.5f, 14.5f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 10f)
                curveTo(8.224f, 10f, 8f, 9.776f, 8f, 9.5f)
                curveTo(8f, 9.224f, 8.224f, 9f, 8.5f, 9f)
                curveTo(8.776f, 9f, 9f, 9.224f, 9f, 9.5f)
                curveTo(9f, 9.776f, 8.776f, 10f, 8.5f, 10f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 10f)
                curveTo(15.224f, 10f, 15f, 9.776f, 15f, 9.5f)
                curveTo(15f, 9.224f, 15.224f, 9f, 15.5f, 9f)
                curveTo(15.776f, 9f, 16f, 9.224f, 16f, 9.5f)
                curveTo(16f, 9.776f, 15.776f, 10f, 15.5f, 10f)
                close()
            }
        }.build()

        return _PeopleRounded!!
    }

@Suppress("ObjectPropertyName")
private var _PeopleRounded: ImageVector? = null
