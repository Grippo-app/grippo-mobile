package com.grippo.design.components.cards.selectable.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCardVariants
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun SelectableCardMedium(
    modifier: Modifier,
    style: SelectableCardStyle.Medium,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.selectableCard.medium.radius)

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            )
            .background(AppTokens.colors.background.secondary, shape)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(
                horizontal = AppTokens.dp.selectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.selectableCard.medium.verticalPadding,
            )
            .height(AppTokens.dp.selectableCard.medium.height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            imageVector = style.icon,
            contentDescription = null,
            modifier = Modifier.size(AppTokens.dp.selectableCard.medium.icon),
            colorFilter = when (isSelected) {
                true -> null
                false -> ColorFilter.tint(color = AppTokens.colors.icon.disabled)
            },
        )

        Text(
            modifier = Modifier.weight(1f),
            text = style.title,
            style = AppTokens.typography.b14Bold(),
            maxLines = 2,
            color = AppTokens.colors.text.primary,
            overflow = TextOverflow.Ellipsis
        )

        Toggle(
            checked = isSelected,
            onCheckedChange = onClick
        )
    }
}

@Composable
internal fun SelectableCardMediumSkeleton(modifier: Modifier) {
    val radius = AppTokens.dp.selectableCard.medium.radius

    Box(
        modifier = modifier
            .shimmerAnimation(visible = true, radius = radius)
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = RoundedCornerShape(radius),
                color = AppTokens.colors.overlay.defaultShadow
            )
            .background(AppTokens.colors.background.secondary, RoundedCornerShape(radius))
            .border(1.dp, AppTokens.colors.border.defaultPrimary, RoundedCornerShape(radius))
            .padding(
                horizontal = AppTokens.dp.selectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.selectableCard.medium.verticalPadding,
            )
            .height(AppTokens.dp.selectableCard.medium.height),
    )
}

@AppPreview
@Composable
private fun SelectableCardMediumPreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Medium(
                title = "Test Title",
                icon = Icons.Filled.Done
            )
        )

        SelectableCardMediumSkeleton(
            modifier = Modifier.fillMaxWidth()
        )
    }
}