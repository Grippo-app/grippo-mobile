package com.grippo.design.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppDp

@Composable
public fun Badge(
    modifier: Modifier = Modifier,
    value: Int,
) {
    if (value <= 0) return

    val label = if (value > 99) "99+" else value.toString()
    val shape = CircleShape

    val offset = AppDp.badge.size / 3

    Box(
        modifier = modifier
            .offset(x = offset, y = -offset)
            .defaultMinSize(minWidth = AppDp.badge.size, minHeight = AppDp.badge.size)
            .background(AppTokens.colors.brand.color2, shape)
            .padding(AppTokens.dp.contentPadding.text),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTokens.typography.b10Med(),
            color = AppTokens.colors.static.white,
            maxLines = 1
        )
    }
}

@AppPreview
@Composable
private fun BadgePreview() {
    PreviewContainer {
        Badge(
            value = 3
        )

        Badge(
            value = 88
        )

        Badge(
            value = 123
        )
    }
}