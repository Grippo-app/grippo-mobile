package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
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
    val colors = AppTokens.colors.selection
    val backgroundColor by animateColorAsState(
        if (isSelected) colors.background else AppTokens.colors.background.card,
        label = "backgroundColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) colors.accent else AppTokens.colors.border.default,
        label = "borderColor"
    )

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(backgroundColor, shape)
            .border(AppTokens.dp.toggleSelectableCard.borderWidth, borderColor, shape)
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
                false -> ColorFilter.tint(color = AppTokens.colors.icon.secondary)
            },
        )

        Text(
            modifier = Modifier.weight(1f),
            text = style.title,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        Toggle(
            checked = isSelected,
            onCheckedChange = onClick,
            contentDescription = style.title,
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
