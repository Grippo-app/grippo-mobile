package com.grippo.design.components.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.design.components.cards.selectable.ToggleSelectableCard
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Stable
public sealed interface EquipmentCardStyle {
    @Immutable
    public data object Inline : EquipmentCardStyle

    @Immutable
    public data object Detail : EquipmentCardStyle

    @Stable
    public data class Selection(
        public val isSelected: Boolean,
        public val onSelect: () -> Unit,
    ) : EquipmentCardStyle
}

@Composable
public fun EquipmentCard(
    modifier: Modifier = Modifier,
    value: EquipmentState,
    style: EquipmentCardStyle,
) {
    when (style) {
        EquipmentCardStyle.Inline -> EquipmentCardInline(modifier = modifier, value = value)
        EquipmentCardStyle.Detail -> EquipmentCardDetail(modifier = modifier, value = value)
        is EquipmentCardStyle.Selection -> EquipmentCardSelection(
            modifier = modifier,
            value = value,
            style = style,
        )
    }
}

@Composable
private fun EquipmentCardInline(
    modifier: Modifier,
    value: EquipmentState,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.equipmentCard.inlineIcon),
            imageVector = value.type.icon(),
            contentDescription = null,
        )

        Text(
            text = value.type.title().text(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun EquipmentCardDetail(
    modifier: Modifier,
    value: EquipmentState,
) {
    Column(
        modifier = modifier.width(AppTokens.dp.equipmentCard.width),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.equipmentCard.icon),
            imageVector = value.type.icon(),
            contentDescription = null,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.type.title().text(),
            textAlign = TextAlign.Center,
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@Composable
private fun EquipmentCardSelection(
    modifier: Modifier,
    value: EquipmentState,
    style: EquipmentCardStyle.Selection,
) {
    val icon = value.type.icon()

    ToggleSelectableCard(
        modifier = modifier.fillMaxWidth(),
        style = ToggleSelectableCardStyle.Medium(
            title = value.type.title().text(),
            icon = icon,
        ),
        isSelected = style.isSelected,
        onSelect = style.onSelect,
        leading = {
            Image(
                modifier = Modifier.size(AppTokens.dp.toggleSelectableCard.medium.icon),
                imageVector = icon,
                contentDescription = null,
            )
        },
    )
}

@AppPreview
@Composable
private fun EquipmentCardInlinePreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.BARBELL,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.ROPE,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Inline,
            )
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardDetailPreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.BARBELL,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.ROPE,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Detail,
            )
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardSelectedPreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.ROPE,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Selection(isSelected = true, onSelect = {}),
            )
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardUnselectedPreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.ROPE,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Selection(isSelected = false, onSelect = {}),
            )
        }
    }
}
