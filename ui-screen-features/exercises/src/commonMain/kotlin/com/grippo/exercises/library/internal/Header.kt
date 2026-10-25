package com.grippo.exercises.library.internal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.inputs.InputSearch
import com.grippo.design.components.muscle.MuscleMark
import com.grippo.design.components.muscle.MuscleMarkStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_library_suggestions_chip
import com.grippo.design.resources.provider.icons.Sparkle
import com.grippo.exercises.library.ExerciseLibraryContract
import com.grippo.exercises.library.Queries
import com.grippo.exercises.library.QueryFilter
import kotlinx.coroutines.CompletableDeferred

@Composable
internal fun Header(
    modifier: Modifier = Modifier,
    value: Queries,
    canShowSuggestions: Boolean,
    contract: ExerciseLibraryContract,
) {
    val groupsScrollState = rememberScrollState()

    Column(modifier = modifier) {
        InputSearch(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            value = value.name,
            onValueChange = contract::onQueryChange
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(groupsScrollState)
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            if (canShowSuggestions) {
                LibraryFilterChip(
                    title = AppTokens.strings.res(Res.string.exercise_library_suggestions_chip),
                    isSelected = value.filter is QueryFilter.Suggestions,
                    onSelect = contract::onSuggestionsClick,
                    leading = { SuggestionsMark() },
                )
            }

            value.muscleGroups.forEach { item ->
                key(item.id) {
                    LibraryFilterChip(
                        title = item.type.title().text(),
                        isSelected = value.filter is QueryFilter.Group && value.filter.id == item.id,
                        onSelect = { contract.onMuscleGroupClick(item.id) },
                        leading = { MuscleMark(group = item.type, style = MuscleMarkStyle.Inline) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryFilterChip(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    // Initial selection can arrive before the chip has been placed.
    val firstPlacement = remember { CompletableDeferred<Unit>() }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            firstPlacement.await()
            bringIntoViewRequester.bringIntoView()
        }
    }

    CheckSelectableCard(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onPlaced { firstPlacement.complete(Unit) },
        style = CheckSelectableCardStyle.Small(title = title),
        isSelected = isSelected,
        onSelect = onSelect,
        leading = leading,
    )
}

@Composable
private fun SuggestionsMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(AppTokens.dp.muscle.inlineMarkSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.checkSelectableCard.small.leadingIcon),
            imageVector = AppTokens.icons.Sparkle,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null,
        )
    }
}

@Composable
@AppPreview
private fun HeaderPreview() {
    PreviewContainer {
        Header(
            value = Queries(
                name = "Bench",
                filter = QueryFilter.Group(stubMuscleGroup().first().id),
                muscleGroups = stubMuscleGroup(),
            ),
            canShowSuggestions = true,
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@Composable
@AppPreview
private fun HeaderAllPreview() {
    PreviewContainer {
        Header(
            value = Queries(muscleGroups = stubMuscleGroup()),
            canShowSuggestions = false,
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@Composable
@AppPreview
private fun HeaderSuggestionsPreview() {
    PreviewContainer {
        Header(
            value = Queries(
                filter = QueryFilter.Suggestions,
                muscleGroups = stubMuscleGroup(),
            ),
            canShowSuggestions = true,
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@Composable
@AppPreview
private fun HeaderLastGroupPreview() {
    val groups = stubMuscleGroup()
    PreviewContainer {
        Header(
            value = Queries(
                filter = QueryFilter.Group(groups.last().id),
                muscleGroups = groups,
            ),
            canShowSuggestions = false,
            contract = ExerciseLibraryContract.Empty,
        )
    }
}
