package com.grippo.design.components.cards

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.cards.internal.SelectableCardLarge
import com.grippo.design.components.cards.internal.SelectableCardLargeSkeleton
import com.grippo.design.components.cards.internal.SelectableCardMedium
import com.grippo.design.components.cards.internal.SelectableCardMediumSkeleton
import com.grippo.design.components.cards.internal.SelectableCardSmall
import com.grippo.design.components.cards.internal.SelectableCardSmallSkeleton
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

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

    @Immutable
    public data class Large(
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

        is SelectableCardStyle.Large -> SelectableCardLarge(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )
    }
}

@Composable
public fun SelectableCardSkeleton(
    modifier: Modifier = Modifier,
    style: SelectableCardStyle,
) {
    when (style) {
        is SelectableCardStyle.Small -> SelectableCardSmallSkeleton(
            modifier = modifier,
        )

        is SelectableCardStyle.Large -> SelectableCardLargeSkeleton(
            modifier = modifier,
        )

        is SelectableCardStyle.Medium -> SelectableCardMediumSkeleton(
            modifier = modifier
        )
    }
}

@AppPreview
@Composable
private fun SelectableCardLargePreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Large(
                title = "Test Title",
                description = "Test Description",
                icon = Icons.Filled.Done
            )
        )
    }
}

@AppPreview
@Composable
private fun SelectableCardMediumPreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Medium(
                title = "Test Title",
                description = "Test Description",
                icon = Icons.Filled.Done
            )
        )
    }
}

@AppPreview
@Composable
private fun SelectableCardSmallPreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Small(
                title = "Test Title"
            )
        )
    }
}

@Composable
private fun ColumnScope.SelectableCardVariants(style: SelectableCardStyle) {
    SelectableCard(
        style = style,
        isSelected = true,
        onSelect = {}
    )
    SelectableCard(
        style = style,
        isSelected = false,
        onSelect = {}
    )
}