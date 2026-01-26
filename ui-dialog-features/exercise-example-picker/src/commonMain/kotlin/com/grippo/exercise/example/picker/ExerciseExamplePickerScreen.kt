package com.grippo.exercise.example.picker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.select_exercise
import com.grippo.exercise.example.picker.internal.Header
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun ExerciseExamplePickerScreen(
    state: ExerciseExamplePickerState,
    loaders: ImmutableSet<ExerciseExamplePickerLoader>,
    contract: ExerciseExamplePickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.select_exercise),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Header(
            modifier = Modifier.fillMaxWidth(),
            value = state.queries,
            contract = contract,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        AnimatedContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            targetState = state.exerciseExamples.isEmpty(),
        ) {
            when (it) {
                true -> EmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                false -> {
                    val basePadding = PaddingValues(
                        horizontal = AppTokens.dp.screen.horizontalPadding,
                        vertical = AppTokens.dp.contentPadding.content
                    )

                    BottomOverlayContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = basePadding,
                        overlay = AppTokens.colors.background.dialog,
                        content = { containerModifier, resolvedPadding ->
                            val listState = rememberLazyListState()
                            val canLoadMore =
                                !state.pagination.isEndReached && !state.pagination.isLoadingNextPage

                            LaunchedEffect(listState, canLoadMore, state.exerciseExamples.size) {
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
                                    .fillMaxWidth()
                                    .weight(1f),
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
                            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample()
                ),
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExamplePickerContract.Empty
        )
    }
}