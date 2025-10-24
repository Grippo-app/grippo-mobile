package com.grippo.design.components.cards.selectable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.cards.selectable.internal.ToggleSelectableCardMedium
import com.grippo.design.components.cards.selectable.internal.ToggleSelectableCardSmall
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.SystemRestart

@Immutable
public sealed class ToggleSelectableCardStyle(
    public open val title: String
) {
    @Immutable
    public data class Small(
        override val title: String
    ) : ToggleSelectableCardStyle(title)

    @Immutable
    public data class Medium(
        override val title: String,
        val icon: ImageVector,
    ) : ToggleSelectableCardStyle(title)
}

@Composable
public fun ToggleSelectableCard(
    modifier: Modifier = Modifier,
    style: ToggleSelectableCardStyle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    when (style) {
        is ToggleSelectableCardStyle.Small -> ToggleSelectableCardSmall(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )

        is ToggleSelectableCardStyle.Medium -> ToggleSelectableCardMedium(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
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
                icon = AppTokens.icons.SystemRestart
            )
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

@Composable
internal fun ToggleSelectableCardVariants(style: ToggleSelectableCardStyle) {
    ToggleSelectableCard(
        style = style,
        isSelected = true,
        onSelect = {}
    )
    ToggleSelectableCard(
        style = style,
        isSelected = false,
        onSelect = {}
    )
}