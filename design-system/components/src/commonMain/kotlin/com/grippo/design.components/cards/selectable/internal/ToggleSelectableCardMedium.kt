package com.grippo.design.components.cards.selectable.internal

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.graphics.ColorFilter
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.components.cards.selectable.ToggleSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.equipment.Dumbbell

@Composable
internal fun ToggleSelectableCardMedium(
    modifier: Modifier,
    style: ToggleSelectableCardStyle.Medium,
    isSelected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(AppTokens.dp.toggleSelectableCard.medium.radius)

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                toggleableState = if (isSelected) {
                    ToggleableState.On
                } else {
                    ToggleableState.Off
                }
            }
            .scalableClick(role = Role.Switch, haptic = true, onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .border(
                AppTokens.dp.toggleSelectableCard.borderWidth,
                AppTokens.colors.border.default,
                shape,
            )
            .heightIn(min = AppTokens.dp.toggleSelectableCard.medium.height)
            .padding(
                horizontal = AppTokens.dp.toggleSelectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.toggleSelectableCard.medium.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        if (leading != null) {
            leading()
        } else {
            Image(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(AppTokens.dp.toggleSelectableCard.medium.icon),
                colorFilter = when (isSelected) {
                    true -> null
                    false -> ColorFilter.tint(color = AppTokens.colors.icon.secondary)
                },
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = style.title,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        Toggle(checked = isSelected)
    }
}

@AppPreview
@Composable
private fun ToggleSelectableCardMediumPreview() {
    PreviewContainer {
        ToggleSelectableCardVariants(
            ToggleSelectableCardStyle.Medium(
                title = "Dumbbells",
                icon = Dumbbell
            )
        )
    }
}
