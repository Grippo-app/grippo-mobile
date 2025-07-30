package com.grippo.design.components.cards.selectable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.cards.selectable.internal.MultiSelectableCardMedium
import com.grippo.design.components.cards.selectable.internal.MultiSelectableCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed class MultiSelectableCardStyle(
    public open val title: String
) {
    @Immutable
    public data class Small(
        override val title: String
    ) : MultiSelectableCardStyle(title)

    @Immutable
    public data class Medium(
        override val title: String,
        val icon: ImageVector,
    ) : MultiSelectableCardStyle(title)
}

@Composable
public fun MultiSelectableCard(
    modifier: Modifier = Modifier,
    style: MultiSelectableCardStyle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    when (style) {
        is MultiSelectableCardStyle.Small -> MultiSelectableCardSmall(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )

        is MultiSelectableCardStyle.Medium -> MultiSelectableCardMedium(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )
    }
}

@AppPreview
@Composable
private fun MultiSelectableCardMediumPreview() {
    PreviewContainer {
        MultiSelectableCardVariants(
            MultiSelectableCardStyle.Medium(
                title = "Test Title",
                icon = Icons.Filled.Done
            )
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

@Composable
internal fun MultiSelectableCardVariants(style: MultiSelectableCardStyle) {
    MultiSelectableCard(
        style = style,
        isSelected = true,
        onSelect = {}
    )
    MultiSelectableCard(
        style = style,
        isSelected = false,
        onSelect = {}
    )
}