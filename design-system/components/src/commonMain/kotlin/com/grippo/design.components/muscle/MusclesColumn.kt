package com.grippo.design.components.muscle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.muscles.stubMuscles
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun MusclesColumn(
    modifier: Modifier = Modifier,
    item: MuscleGroupState<MuscleRepresentationState.Plain>,
    selectedIds: ImmutableList<String>,
    onSelect: (id: String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = item.name,
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

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

@AppPreview
@Composable
private fun MusclesColumnPreview() {
    PreviewContainer {
        val group = stubMuscles().random()

        MusclesColumn(
            item = group,
            selectedIds = persistentListOf(group.muscles.random().value.id),
            onSelect = {}
        )
    }
}