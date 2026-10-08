package com.grippo.design.components.cards.selectable.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.components.cards.selectable.ToggleSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ToggleSelectableCardSmall(
    modifier: Modifier,
    style: ToggleSelectableCardStyle.Small,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.toggleSelectableCard.small.radius)

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .heightIn(min = AppTokens.dp.toggleSelectableCard.small.height)
            .padding(
                horizontal = AppTokens.dp.toggleSelectableCard.small.horizontalPadding,
                vertical = AppTokens.dp.toggleSelectableCard.small.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = style.title,
            style = AppTokens.typography.h6(),
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
private fun ToggleSelectableCardSmallPreview() {
    PreviewContainer {
        ToggleSelectableCardVariants(
            ToggleSelectableCardStyle.Small(
                title = "Test Title"
            )
        )
    }
}