package com.grippo.design.components.cards.selectable.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.MultiSelectableCardStyle
import com.grippo.design.components.cards.selectable.MultiSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun MultiSelectableCardSmall(
    modifier: Modifier,
    style: MultiSelectableCardStyle.Small,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.multiSelectableCard.small.radius)

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.secondary, shape)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(horizontal = AppTokens.dp.multiSelectableCard.small.horizontalPadding)
            .height(AppTokens.dp.multiSelectableCard.small.height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
private fun MultiSelectableCardSmallPreview() {
    PreviewContainer {
        MultiSelectableCardVariants(
            MultiSelectableCardStyle.Small(
                title = "Test Title"
            )
        )
    }
}