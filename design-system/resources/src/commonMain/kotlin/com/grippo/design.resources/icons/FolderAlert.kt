package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.FolderAlert: ImageVector
    get() {
        if (_FolderAlert != null) {
            return _FolderAlert!!
        }
        _FolderAlert = ImageVector.Builder(
            name = "FolderAlert",
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
                moveTo(18f, 3f)
                verticalLineTo(7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 11.01f)
                lineTo(18.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 7f)
                verticalLineTo(11f)
                verticalLineTo(19.4f)
                curveTo(22f, 19.731f, 21.731f, 20f, 21.4f, 20f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 20f, 2f, 19.731f, 2f, 19.4f)
                verticalLineTo(11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 7f)
                horizontalLineTo(12.722f)
                curveTo(12.579f, 7f, 12.44f, 6.949f, 12.332f, 6.856f)
                lineTo(9.169f, 4.144f)
                curveTo(9.06f, 4.051f, 8.921f, 4f, 8.778f, 4f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 4f, 2f, 4.269f, 2f, 4.6f)
                verticalLineTo(11f)
                horizontalLineTo(14f)
            }
        }.build()

        return _FolderAlert!!
    }

@Suppress("ObjectPropertyName")
private var _FolderAlert: ImageVector? = null
