package com.grippo.authorization.registration.excluded.muscles.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.SelectableCard
import com.grippo.design.components.cards.SelectableCardStyle
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import com.grippo.presentation.api.muscles.models.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MusclesColumn(
    modifier: Modifier = Modifier,
    item: MuscleGroupState<MuscleRepresentationState.Plain>,
    selectedIds: ImmutableList<String>,
    onSelect: (id: String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item.muscles.forEach { muscle ->
            val selectProvider = remember(muscle.value.id) {
                { onSelect.invoke(muscle.value.id) }
            }

            val isSelected = remember(muscle.value.id, selectedIds) {
                selectedIds.contains(muscle.value.id)
            }

            SelectableCard(
                modifier = Modifier.fillMaxWidth(),
                style = SelectableCardStyle.Small(
                    title = muscle.value.name
                ),
                isSelected = isSelected,
                onSelect = selectProvider
            )
        }
    }
}