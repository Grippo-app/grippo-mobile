package com.grippo.design.components.cards.selectable.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.components.cards.selectable.ToggleSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Spinner

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
            .heightIn(min = AppTokens.dp.toggleSelectableCard.medium.height)
            .padding(
                horizontal = AppTokens.dp.toggleSelectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.toggleSelectableCard.medium.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
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
            style = AppTokens.typography.h5(),
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
                icon = AppTokens.icons.Spinner
            )
        )
    }
}
