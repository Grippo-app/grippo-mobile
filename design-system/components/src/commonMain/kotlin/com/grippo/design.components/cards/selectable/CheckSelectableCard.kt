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
import com.grippo.design.components.cards.selectable.internal.CheckSelectableCardLarge
import com.grippo.design.components.cards.selectable.internal.CheckSelectableCardMedium
import com.grippo.design.components.cards.selectable.internal.CheckSelectableCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer


@Immutable
public sealed class CheckSelectableCardStyle(
    public open val title: String
) {
    @Immutable
    public data class Small(
        override val title: String,
    ) : CheckSelectableCardStyle(title)

    @Immutable
    public data class Medium(
        override val title: String,
        val description: String?,
    ) : CheckSelectableCardStyle(title)

    @Immutable
    public data class Large(
        override val title: String,
        val description: String,
        val icon: ImageVector,
        val subContent: (@Composable ColumnScope.() -> Unit)?,
    ) : CheckSelectableCardStyle(title)
}

@Composable
public fun SelectableCard(
    modifier: Modifier = Modifier,
    style: CheckSelectableCardStyle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    when (style) {
        is CheckSelectableCardStyle.Large -> CheckSelectableCardLarge(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )

        is CheckSelectableCardStyle.Medium -> CheckSelectableCardMedium(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )

        is CheckSelectableCardStyle.Small -> CheckSelectableCardSmall(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardLargePreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Large(
                title = "Test Title",
                description = "Test Description",
                icon = Icons.Filled.Done,
                subContent = {
                    Box(modifier = Modifier.size(40.dp).background(Color.Green))
                }
            )
        )

        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Large(
                title = "Test Title",
                description = "Test Description",
                icon = Icons.Filled.Done,
                subContent = null
            )
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardMediumPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Medium(
                title = "Test Title",
                description = "Test Description"
            )
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardSmallPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Small(
                title = "Test Title",
            )
        )
    }
}

@Composable
internal fun CheckSelectableCardVariants(style: CheckSelectableCardStyle) {
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