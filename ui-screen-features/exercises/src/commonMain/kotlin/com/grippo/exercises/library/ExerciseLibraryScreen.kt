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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExample
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
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.exercises.library.internal.Header
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged

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
            ) {
                ExerciseExampleCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = ExerciseExampleCardStyle.Small(value = target),
                )
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

    HorizontalDivider(color = AppTokens.colors.divider.default)

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
                    horizontal = AppTokens.dp.screen.horizontalPadding,
                    vertical = AppTokens.dp.contentPadding.content
                )

                BottomOverlayContainer(
                    modifier = Modifier
                        .fillMaxSize(),
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
                            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                            contentPadding = resolvedPadding
                        ) {
                            items(
                                items = state.exerciseExamples,
                                key = { k -> k.value.id },
                            ) { item ->
                                val selectClickProvider = remember(item.value.id) {
                                    { contract.onExerciseExampleSelectClick(item.value.id) }
                                }

                                ExerciseExampleCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scalableClick(onClick = selectClickProvider),
                                    style = ExerciseExampleCardStyle.Large(value = item),
                                )
                            }
                        }
                    },
                    bottom = {
                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
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
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample(),
                ),
            ),
            contract = ExerciseLibraryContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewReplace() {
    val targetExample = stubExerciseExample().value
    PreviewContainer {
        ExerciseLibraryScreen(
            state = ExerciseLibraryState(
                mode = ExerciseLibraryMode.SimilarTo(
                    targetExerciseExampleId = targetExample.id,
                    target = targetExample,
                ),
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample(),
                ),
                queries = Queries(filter = QueryFilter.Suggestions),
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
