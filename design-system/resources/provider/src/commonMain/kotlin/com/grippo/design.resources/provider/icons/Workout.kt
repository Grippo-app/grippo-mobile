/*
 * Adapted from the Lucide dumbbell icon: https://github.com/lucide-icons/lucide
 *
 * ISC License
 *
 * Copyright (c) 2026 Lucide Icons and Contributors
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Workout: ImageVector
    get() {
        if (_Workout != null) {
            return _Workout!!
        }
        _Workout = ImageVector.Builder(
            name = "Workout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(17.596f, 12.768f)
                arcToRelative(2f, 2f, 0f, true, false, 2.829f, -2.829f)
                lineToRelative(-1.768f, -1.767f)
                arcToRelative(2f, 2f, 0f, false, false, 2.828f, -2.829f)
                lineToRelative(-2.828f, -2.828f)
                arcToRelative(2f, 2f, 0f, false, false, -2.829f, 2.828f)
                lineToRelative(-1.767f, -1.768f)
                arcToRelative(2f, 2f, 0f, true, false, -2.829f, 2.829f)
                close()
                moveTo(2.5f, 21.5f)
                lineToRelative(1.4f, -1.4f)
                moveTo(20.1f, 3.9f)
                lineToRelative(1.4f, -1.4f)
                moveTo(5.343f, 21.485f)
                arcToRelative(2f, 2f, 0f, true, false, 2.829f, -2.828f)
                lineToRelative(1.767f, 1.768f)
                arcToRelative(2f, 2f, 0f, true, false, 2.829f, -2.829f)
                lineToRelative(-6.364f, -6.364f)
                arcToRelative(2f, 2f, 0f, true, false, -2.829f, 2.829f)
                lineToRelative(1.768f, 1.767f)
                arcToRelative(2f, 2f, 0f, false, false, -2.828f, 2.829f)
                close()
                moveTo(9.6f, 14.4f)
                lineToRelative(4.8f, -4.8f)
            }
        }.build()
        return _Workout!!
    }

@Suppress("ObjectPropertyName")
private var _Workout: ImageVector? = null
