package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ReportColumns: ImageVector
    get() {
        if (_ReportColumns != null) {
            return _ReportColumns!!
        }
        _ReportColumns = ImageVector.Builder(
            name = "ReportColumns",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 7.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(9.4f)
                curveTo(9.731f, 3f, 10f, 3.269f, 10f, 3.6f)
                verticalLineTo(7.4f)
                curveTo(10f, 7.731f, 9.731f, 8f, 9.4f, 8f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 8f, 3f, 7.731f, 3f, 7.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(14f, 20.4f)
                verticalLineTo(16.6f)
                curveTo(14f, 16.269f, 14.269f, 16f, 14.6f, 16f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 16f, 21f, 16.269f, 21f, 16.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(14.6f)
                curveTo(14.269f, 21f, 14f, 20.731f, 14f, 20.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(14f, 12.4f)
                verticalLineTo(3.6f)
                curveTo(14f, 3.269f, 14.269f, 3f, 14.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(12.4f)
                curveTo(21f, 12.731f, 20.731f, 13f, 20.4f, 13f)
                horizontalLineTo(14.6f)
                curveTo(14.269f, 13f, 14f, 12.731f, 14f, 12.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 20.4f)
                verticalLineTo(11.6f)
                curveTo(3f, 11.269f, 3.269f, 11f, 3.6f, 11f)
                horizontalLineTo(9.4f)
                curveTo(9.731f, 11f, 10f, 11.269f, 10f, 11.6f)
                verticalLineTo(20.4f)
                curveTo(10f, 20.731f, 9.731f, 21f, 9.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                close()
            }
        }.build()

        return _ReportColumns!!
    }

@Suppress("ObjectPropertyName")
private var _ReportColumns: ImageVector? = null
