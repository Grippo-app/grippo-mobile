package com.grippo.design.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.cards.internal.SelectableCardMedium
import com.grippo.design.components.cards.internal.SelectableCardSmall

@Immutable
public sealed class SelectableCardStyle(
    public open val title: String
) {
    @Immutable
    public data class Small(
        override val title: String
    ) : SelectableCardStyle(title)

    @Immutable
    public data class Medium(
        override val title: String,
        val description: String,
        val icon: ImageVector,
    ) : SelectableCardStyle(title)
}

@Composable
public fun SelectableCard(
    modifier: Modifier = Modifier,
    style: SelectableCardStyle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    when (style) {
        is SelectableCardStyle.Small -> SelectableCardSmall(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )

        is SelectableCardStyle.Medium -> SelectableCardMedium(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )
    }
}