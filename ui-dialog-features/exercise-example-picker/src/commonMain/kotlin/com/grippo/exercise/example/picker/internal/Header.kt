package com.grippo.exercise.example.picker.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.inputs.InputSearch
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_example_picker_suggestions_chip
import com.grippo.exercise.example.picker.ExerciseExamplePickerContract
import com.grippo.exercise.example.picker.Queries
import com.grippo.exercise.example.picker.QueryFilter

@Composable
internal fun Header(
    modifier: Modifier = Modifier,
    value: Queries,
    canShowSuggestions: Boolean,
    contract: ExerciseExamplePickerContract,
) {
    Column(modifier = modifier) {
        InputSearch(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            value = value.name,
            onValueChange = contract::onQueryChange
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val groupsListState = rememberLazyListState()

        LaunchedEffect(value.muscleGroups) {
            val groupOffset = if (canShowSuggestions) 1 else 0
            val targetIndex = when (val filter = value.filter) {
                QueryFilter.Suggestions -> if (canShowSuggestions) 0 else null
                is QueryFilter.Group -> value.muscleGroups
                    .indexOfFirst { it.id == filter.id }
                    .takeIf { it >= 0 }
                    ?.plus(groupOffset)

                QueryFilter.All -> null
            } ?: return@LaunchedEffect
            groupsListState.scrollToItem(targetIndex)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
            state = groupsListState
        ) {
            if (canShowSuggestions) {
                item(key = "suggestions") {
                    val suggestionsClickProvider = remember {
                        { contract.onSuggestionsClick() }
                    }

                    CheckSelectableCard(
                        style = CheckSelectableCardStyle.Small(
                            title = AppTokens.strings.res(Res.string.exercise_example_picker_suggestions_chip)
                        ),
                        isSelected = value.filter is QueryFilter.Suggestions,
                        onSelect = suggestionsClickProvider
                    )
                }
            }

            items(
                items = value.muscleGroups,
                key = { it.id },
            ) { item ->
                val clickProvider = remember(item.id) {
                    { contract.onMuscleGroupClick(item.id) }
                }

                CheckSelectableCard(
                    style = CheckSelectableCardStyle.Small(
                        title = item.type.title().text()
                    ),
                    isSelected = value.filter is QueryFilter.Group && value.filter.id == item.id,
                    onSelect = clickProvider
                )
            }
        }
    }
}

@Composable
@AppPreview
private fun HeaderPreview() {
    PreviewContainer {
        Header(
            value = Queries(
                name = "Bench",
                filter = QueryFilter.Suggestions,
            ),
            canShowSuggestions = true,
            contract = ExerciseExamplePickerContract.Empty,
        )
    }
}
