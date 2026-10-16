package com.grippo.design.components.cards.selectable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.cards.selectable.internal.ToggleSelectableCardMedium
import com.grippo.design.components.cards.selectable.internal.ToggleSelectableCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.equipment.Barbell
import com.grippo.design.resources.provider.equipment.LegExtensionMachine
import com.grippo.design.resources.provider.equipment.SmithMachine

@Immutable
public sealed class ToggleSelectableCardStyle(
    public open val title: String
) {
    @Immutable
    public data class Small(
        public override val title: String
    ) : ToggleSelectableCardStyle(title)

    @Immutable
    public data class Medium(
        public override val title: String,
        public val icon: ImageVector,
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
                title = "Barbell",
                icon = Barbell
            )
        )
        ToggleSelectableCardVariants(
            ToggleSelectableCardStyle.Medium(
                title = "Smith machine",
                icon = SmithMachine
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
                title = "Quadriceps"
            )
        )
    }
}

@AppPreview
@Composable
private fun ToggleSelectableCardLongContentPreview() {
    PreviewContainer {
        ToggleSelectableCardVariants(
            ToggleSelectableCardStyle.Small(
                title = "Розгиначі хребта та м'язи попереку"
            )
        )
        ToggleSelectableCardVariants(
            ToggleSelectableCardStyle.Medium(
                title = "Тренажер для розгинання ніг сидячи",
                icon = LegExtensionMachine
            )
        )
    }
}

@Composable
internal fun ToggleSelectableCardVariants(style: ToggleSelectableCardStyle) {
    ToggleSelectableCard(
        modifier = Modifier.fillMaxWidth(),
        style = style,
        isSelected = true,
        onSelect = {}
    )
    ToggleSelectableCard(
        modifier = Modifier.fillMaxWidth(),
        style = style,
        isSelected = false,
        onSelect = {}
    )
}
