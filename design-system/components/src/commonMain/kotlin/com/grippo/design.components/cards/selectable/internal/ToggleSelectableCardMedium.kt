package com.grippo.design.components.cards.selectable.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.components.cards.selectable.ToggleSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ToggleSelectableCardMedium(
    modifier: Modifier,
    style: ToggleSelectableCardStyle.Medium,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.toggleSelectableCard.medium.radius)

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.toggleSelectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.toggleSelectableCard.medium.verticalPadding,
            )
            .height(AppTokens.dp.toggleSelectableCard.medium.height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            imageVector = style.icon,
            contentDescription = null,
            modifier = Modifier.size(AppTokens.dp.toggleSelectableCard.medium.icon),
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

@AppPreview
@Composable
private fun ToggleSelectableCardMediumPreview() {
    PreviewContainer {
        ToggleSelectableCardVariants(
            ToggleSelectableCardStyle.Medium(
                title = "Test Title",
                icon = Icons.Filled.Done
            )
        )
    }
}