package com.grippo.exercises.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.empty.IllustratedEmptyState
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.frames.FocusFrame
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_exercise_examples
import com.grippo.design.resources.provider.exercise_library
import com.grippo.design.resources.provider.exercise_library_replace_target
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.exercises.library.internal.Header
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDateTime

@Composable
internal fun ExerciseLibraryScreen(
    state: ExerciseLibraryState,
    contract: ExerciseLibraryContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.exercise_library),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    val replaceMode = state.mode as? ExerciseLibraryMode.SimilarTo

    var cachedCardTarget by remember {
        mutableStateOf<ExerciseExampleValueState?>(null)
    }

    LaunchedEffect(replaceMode?.target) {
        if (replaceMode?.target != null) cachedCardTarget = replaceMode.target
    }

    AnimatedVisibility(
        modifier = Modifier.fillMaxWidth(),
        visible = replaceMode?.target != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val target = replaceMode?.target ?: cachedCardTarget ?: return@AnimatedVisibility

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

            FocusFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                accent = AppTokens.colors.border.default,
                background = AppTokens.colors.background.card,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                ) {
                    Text(
                        text = AppTokens.strings.res(Res.string.exercise_library_replace_target),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                    ExerciseExampleCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = ExerciseExampleCardStyle.Small(value = target),
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Header(
        modifier = Modifier.fillMaxWidth(),
        value = state.queries,
        canShowSuggestions = state.canShowSuggestions,
        contract = contract,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

    ContentDivider()

    val isEmpty = state.exerciseExamples.isEmpty()

    AnimatedContent(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .imePadding(),
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        },
        targetState = isEmpty
    ) {
        when (it) {
            true -> IllustratedEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
                contentPadding = PaddingValues(AppTokens.dp.screen.horizontalPadding),
                text = AppTokens.strings.res(Res.string.empty_exercise_examples)
            )

            false -> {
                val basePadding = PaddingValues(
                    start = AppTokens.dp.screen.horizontalPadding,
                    end = AppTokens.dp.screen.horizontalPadding,
                    top = AppTokens.dp.contentPadding.content,
                    bottom = AppTokens.dp.screen.verticalPadding,
                )

                BottomOverlayContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = basePadding,
                    overlay = AppTokens.colors.background.screen,
                    content = { containerModifier, resolvedPadding ->
                        val listState = rememberLazyListState()
                        val canLoadMore = !state.pagination.isEndReached &&
                                !state.pagination.isLoadingNextPage

                        LaunchedEffect(listState, canLoadMore) {
                            if (!canLoadMore) return@LaunchedEffect
                            snapshotFlow {
                                val layoutInfo = listState.layoutInfo
                                val totalItems = layoutInfo.totalItemsCount
                                if (totalItems == 0) return@snapshotFlow false
                                val lastItemIndex = layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: return@snapshotFlow false
                                lastItemIndex >= totalItems - 1
                            }
                                .distinctUntilChanged()
                                .collect { isAtEnd -> if (isAtEnd) contract.onLoadNextPage() }
                        }

                        LazyColumn(
                            modifier = containerModifier
                                .fillMaxSize(),
                            state = listState,
                            contentPadding = resolvedPadding
                        ) {
                            itemsIndexed(
                                items = state.exerciseExamples,
                                key = { _, item -> item.value.id },
                            ) { index, item ->
                                ExerciseExampleCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scalableClick(role = Role.Button) {
                                            contract.onExerciseExampleSelectClick(item.value.id)
                                        },
                                    style = ExerciseExampleCardStyle.Large(value = item),
                                )
                                if (index < state.exerciseExamples.lastIndex) {
                                    ContentDivider(
                                        modifier = Modifier.padding(
                                            start = AppTokens.dp.exerciseExampleImage.medium.size +
                                                    AppTokens.dp.contentPadding.content,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreviewDefault() {
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.Default(preselectedMuscleGroupId = null),
                exerciseExamples = exerciseLibraryPreviewExamples(),
                queries = Queries(muscleGroups = stubMuscleGroup()),
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewReplace() {
    val examples = exerciseLibraryPreviewExamples()
    val targetExample = examples.first().value
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.SimilarTo(
                    targetExerciseExampleId = targetExample.id,
                    target = targetExample,
                ),
                exerciseExamples = examples,
                queries = Queries(
                    filter = QueryFilter.Suggestions,
                    muscleGroups = stubMuscleGroup(),
                ),
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.Default(preselectedMuscleGroupId = null),
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewReplacementFallback() {
    val examples = exerciseLibraryPreviewExamples()
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.SimilarTo(
                    targetExerciseExampleId = examples.first().value.id,
                    target = examples.first().value,
                ),
                exerciseExamples = examples,
                hasSuggestionsResults = false,
                queries = Queries(muscleGroups = stubMuscleGroup()),
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewReplacementWithoutTarget() {
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.SimilarTo(targetExerciseExampleId = "library-target"),
                exerciseExamples = exerciseLibraryPreviewExamples(),
                queries = Queries(
                    filter = QueryFilter.Suggestions,
                    muscleGroups = stubMuscleGroup(),
                ),
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewReplacementNoResults() {
    val example = exerciseLibraryPreviewExamples().first()
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.SimilarTo(
                    targetExerciseExampleId = example.value.id,
                    target = example.value,
                ),
                queries = Queries(
                    name = "No matching exercise",
                    muscleGroups = stubMuscleGroup(),
                ),
                hasSuggestionsResults = false,
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewMuscleFilter() {
    val muscleGroups = stubMuscleGroup()
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.Default(preselectedMuscleGroupId = muscleGroups.last().id),
                exerciseExamples = exerciseLibraryPreviewExamples(),
                queries = Queries(
                    filter = QueryFilter.Group(muscleGroups.last().id),
                    muscleGroups = muscleGroups,
                ),
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

private fun exerciseLibraryPreviewExamples() = persistentListOf(
    exerciseLibraryPreviewExample("library-incline", "Жим гантелей лежачи на похилій лаві нейтральним хватом"),
    exerciseLibraryPreviewExample("library-bench", "Dumbbell bench press").let { example ->
        example.copy(
            value = example.value.copy(
                usageCount = 0,
                lastUsed = DateTimeFormatState.Empty(DateFormat.DateOnly.DateDdMmm),
            ),
        )
    },
)

private fun exerciseLibraryPreviewExample(id: String, name: String): ExerciseExampleState {
    val example = stubExerciseExample()
    return example.copy(
        value = example.value.copy(
            id = id,
            name = name,
            lastUsed = DateTimeFormatState.of(
                value = LocalDateTime(2026, 10, 14, 12, 0),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateDdMmm,
            ),
        ),
        bundles = persistentListOf(),
        equipments = persistentListOf(
            EquipmentState(id = "library-dumbbells", type = EquipmentEnumState.DUMBBELLS),
            EquipmentState(id = "library-bench", type = EquipmentEnumState.ADJUSTABLE_BENCH),
        ),
    )
}
