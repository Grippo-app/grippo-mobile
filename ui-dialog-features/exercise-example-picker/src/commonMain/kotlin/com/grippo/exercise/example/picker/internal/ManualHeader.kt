package com.grippo.exercise.example.picker.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.components.badge.Badge
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.inputs.InputSearch
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Filter
import com.grippo.design.resources.provider.icons.Magic
import com.grippo.exercise.example.picker.ExerciseExamplePickerContract
import com.grippo.exercise.example.picker.ExerciseExamplePickerLoader
import com.grippo.exercise.example.picker.ManualQueries
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ManualHeader(
    modifier: Modifier = Modifier,
    value: ManualQueries,
    contract: ExerciseExamplePickerContract,
    loaders: ImmutableSet<ExerciseExamplePickerLoader>
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            InputSearch(
                modifier = Modifier.weight(1f),
                value = value.name,
                onValueChange = contract::onQueryChange
            )
            Box {
                Button(
                    content = ButtonContent.Icon(
                        icon = AppTokens.icons.Filter
                    ),
                    style = ButtonStyle.Tertiary,
                    onClick = contract::onFiltersClick
                )

                val count = remember(value.filters) {
                    value.filters.count { it.isSelected() }
                }

                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    value = count
                )
            }

            val buttonState = remember(loaders) {
                when {
                    loaders.contains(ExerciseExamplePickerLoader.SuggestExample) -> ButtonState.Loading
                    else -> ButtonState.Enabled
                }
            }

            Button(
                content = ButtonContent.Icon(
                    icon = AppTokens.icons.Magic
                ),
                state = buttonState,
                style = ButtonStyle.Magic,
                onClick = contract::onSuggestClick
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val groupsListState = rememberLazyListState()

        LaunchedEffect(value.muscleGroups) {
            val index = value.muscleGroups
                .map { it.id }
                .indexOf(value.selectedMuscleGroupId)
                .takeIf { it >= 0 } ?: return@LaunchedEffect
            groupsListState.scrollToItem(index)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
            state = groupsListState
        ) {
            items(
                items = value.muscleGroups,
                key = { it.id },
            ) { item ->
                val clickProvider = remember(item.id) {
                    { contract.onMuscleGroupClick(item.id) }
                }

                SelectableCard(
                    style = CheckSelectableCardStyle.Small(
                        title = item.type.title().text()
                    ),
                    isSelected = value.selectedMuscleGroupId == item.id,
                    onSelect = clickProvider
                )
            }
        }
    }
}

@Composable
@AppPreview
private fun ManualHeaderPreview() {
    PreviewContainer {
        ManualHeader(
            value = ManualQueries(
                name = "Bench",
                selectedMuscleGroupId = null,
            ),
            contract = ExerciseExamplePickerContract.Empty,
            loaders = persistentSetOf()
        )
    }
}