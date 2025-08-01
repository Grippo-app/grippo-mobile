package com.grippo.design.components.cards.selectable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.internal.SelectableCardLarge
import com.grippo.design.components.cards.selectable.internal.SelectableCardMedium
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed class SelectableCardStyle(
    public open val title: String
) {
    @Immutable
    public data class Medium(
        override val title: String,
        val description: String,
        val subContent: (@Composable ColumnScope.() -> Unit)?,
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
        is SelectableCardStyle.Large -> SelectableCardLarge(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )

        is SelectableCardStyle.Medium -> SelectableCardMedium(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect,
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
                description = "from - to",
                subContent = null
            )
        )
        SelectableCardVariants(
            SelectableCardStyle.Medium(
                title = "Test Title",
                description = "from - to",
                subContent = {
                    Box(modifier = Modifier.size(40.dp).background(Color.Green))
                }
            )
        )
    }
}

@Composable
internal fun SelectableCardVariants(style: SelectableCardStyle) {
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