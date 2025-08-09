package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PackageLock: ImageVector
    get() {
        if (_PackageLock != null) {
            return _PackageLock!!
        }
        _PackageLock = ImageVector.Builder(
            name = "PackageLock",
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
                moveTo(14f, 20f)
                horizontalLineTo(6f)
                curveTo(4.895f, 20f, 4f, 19.105f, 4f, 18f)
                verticalLineTo(6f)
                curveTo(4f, 4.895f, 4.895f, 4f, 6f, 4f)
                horizontalLineTo(18f)
                curveTo(19.104f, 4f, 20f, 4.895f, 20f, 6f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                verticalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.167f, 18.5f)
                horizontalLineTo(17.833f)
                moveTo(21.167f, 18.5f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 18.5f, 22f, 18.769f, 22f, 19.1f)
                verticalLineTo(21.4f)
                curveTo(22f, 21.731f, 21.731f, 22f, 21.4f, 22f)
                horizontalLineTo(17.6f)
                curveTo(17.269f, 22f, 17f, 21.731f, 17f, 21.4f)
                verticalLineTo(19.1f)
                curveTo(17f, 18.769f, 17.269f, 18.5f, 17.6f, 18.5f)
                horizontalLineTo(17.833f)
                horizontalLineTo(21.167f)
                close()
                moveTo(21.167f, 18.5f)
                verticalLineTo(16.75f)
                curveTo(21.167f, 16.167f, 20.833f, 15f, 19.5f, 15f)
                curveTo(18.167f, 15f, 17.833f, 16.167f, 17.833f, 16.75f)
                verticalLineTo(18.5f)
                horizontalLineTo(21.167f)
                close()
            }
        }.build()

        return _PackageLock!!
    }

@Suppress("ObjectPropertyName")
private var _PackageLock: ImageVector? = null
