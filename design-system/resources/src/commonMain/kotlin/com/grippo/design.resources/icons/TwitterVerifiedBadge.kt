package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.TwitterVerifiedBadge: ImageVector
    get() {
        if (_TwitterVerifiedBadge != null) {
            return _TwitterVerifiedBadge!!
        }
        _TwitterVerifiedBadge = ImageVector.Builder(
            name = "TwitterVerifiedBadge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(10.521f, 2.624f)
                curveTo(11.315f, 1.752f, 12.685f, 1.752f, 13.479f, 2.624f)
                lineTo(14.499f, 3.744f)
                curveTo(14.9f, 4.184f, 15.476f, 4.423f, 16.071f, 4.395f)
                lineTo(17.584f, 4.324f)
                curveTo(18.761f, 4.269f, 19.731f, 5.238f, 19.676f, 6.415f)
                lineTo(19.605f, 7.929f)
                curveTo(19.577f, 8.524f, 19.816f, 9.1f, 20.256f, 9.501f)
                lineTo(21.376f, 10.521f)
                curveTo(22.247f, 11.315f, 22.247f, 12.685f, 21.376f, 13.479f)
                lineTo(20.256f, 14.499f)
                curveTo(19.816f, 14.9f, 19.577f, 15.476f, 19.605f, 16.071f)
                lineTo(19.676f, 17.584f)
                curveTo(19.731f, 18.761f, 18.761f, 19.731f, 17.584f, 19.676f)
                lineTo(16.071f, 19.605f)
                curveTo(15.476f, 19.577f, 14.9f, 19.816f, 14.499f, 20.256f)
                lineTo(13.479f, 21.376f)
                curveTo(12.685f, 22.247f, 11.315f, 22.247f, 10.521f, 21.376f)
                lineTo(9.501f, 20.256f)
                curveTo(9.1f, 19.816f, 8.524f, 19.577f, 7.929f, 19.605f)
                lineTo(6.415f, 19.676f)
                curveTo(5.238f, 19.731f, 4.269f, 18.761f, 4.324f, 17.584f)
                lineTo(4.395f, 16.071f)
                curveTo(4.423f, 15.476f, 4.184f, 14.9f, 3.744f, 14.499f)
                lineTo(2.624f, 13.479f)
                curveTo(1.752f, 12.685f, 1.752f, 11.315f, 2.624f, 10.521f)
                lineTo(3.744f, 9.501f)
                curveTo(4.184f, 9.1f, 4.423f, 8.524f, 4.395f, 7.929f)
                lineTo(4.324f, 6.415f)
                curveTo(4.269f, 5.238f, 5.238f, 4.269f, 6.415f, 4.324f)
                lineTo(7.929f, 4.395f)
                curveTo(8.524f, 4.423f, 9.1f, 4.184f, 9.501f, 3.744f)
                lineTo(10.521f, 2.624f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15f, 10f)
            }
        }.build()

        return _TwitterVerifiedBadge!!
    }

@Suppress("ObjectPropertyName")
private var _TwitterVerifiedBadge: ImageVector? = null
