package com.grippo.exercise.example.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.badge.Badge
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.inputs.InputSearch
import com.grippo.design.components.placeholder.ScreenPlaceholder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Filter
import com.grippo.design.resources.provider.not_found
import com.grippo.design.resources.provider.select_exercise
import com.grippo.state.exercise.examples.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExamplePickerScreen(
    state: ExerciseExamplePickerState,
    loaders: ImmutableSet<ExerciseExamplePickerLoader>,
    contract: ExerciseExamplePickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.select_exercise),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            InputSearch(
                modifier = Modifier.weight(1f),
                value = state.query,
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

                val count = remember(state.filters) {
                    state.filters.count { it.isSelected() }
                }

                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    value = count
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val groupsListState = rememberLazyListState()

        LaunchedEffect(state.muscleGroups, state.selectedMuscleGroupId) {
            val index = state.muscleGroups
                .map { it.id }
                .indexOf(state.selectedMuscleGroupId)
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
                items = state.muscleGroups,
                key = { it.id },
            ) { item ->
                val clickProvider = remember(item.id) {
                    { contract.onMuscleGroupClick(item.id) }
                }

                SelectableCard(
                    style = CheckSelectableCardStyle.Small(
                        title = item.type.title().text()
                    ),
                    isSelected = state.selectedMuscleGroupId == item.id,
                    onSelect = clickProvider
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        if (state.exerciseExamples.isEmpty()) {
            ScreenPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = AppTokens.strings.res(Res.string.not_found),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
            ) {
                items(
                    items = state.exerciseExamples,
                    key = { it.value.id },
                ) { item ->
                    val selectClickProvider = remember(item.value.id) {
                        { contract.onExerciseExampleSelectClick(item.value.id) }
                    }

                    ExerciseExampleCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = item,
                        style = ExerciseExampleCardStyle.Medium(
                            onCardClick = selectClickProvider,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                selectedMuscleGroupId = null,
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample()
                ),
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExamplePickerContract.Empty
        )

        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                exerciseExamples = persistentListOf(),
                selectedMuscleGroupId = null
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExamplePickerContract.Empty
        )
    }
}
