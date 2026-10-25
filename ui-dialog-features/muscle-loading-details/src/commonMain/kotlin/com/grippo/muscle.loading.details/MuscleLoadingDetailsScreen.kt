package com.grippo.muscle.loading.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryEmpty
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummarySingleGroup
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryUnknown
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.datetime.DialogRangeHeader
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCard
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingImagesMode
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingImagesRow
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingStyle
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowTop
import com.grippo.design.resources.provider.muscle_load_excluded_sets
import com.grippo.design.resources.provider.muscle_load_unassigned_sets
import com.grippo.design.resources.provider.muscle_loading_details_method_body
import com.grippo.design.resources.provider.muscle_loading_details_method_collapsed
import com.grippo.design.resources.provider.muscle_loading_details_method_expanded
import com.grippo.design.resources.provider.muscle_loading_details_method_limits
import com.grippo.design.resources.provider.muscle_loading_details_method_title
import com.grippo.design.resources.provider.muscle_loading_details_no_sets_message
import com.grippo.design.resources.provider.muscle_loading_details_no_sets_title
import com.grippo.design.resources.provider.muscle_loading_details_no_workouts_message
import com.grippo.design.resources.provider.muscle_loading_details_no_workouts_title
import com.grippo.design.resources.provider.muscle_loading_details_unknown_message
import com.grippo.design.resources.provider.muscle_loading_details_unknown_title
import com.grippo.muscle.loading.details.components.MuscleLoadingDetailsLoadingContent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

private const val MethodAnimationDurationMillis = 200

@Composable
internal fun MuscleLoadingDetailsScreen(
    state: MuscleLoadingDetailsState,
    loaders: ImmutableSet<MuscleLoadingDetailsLoader>,
    contract: MuscleLoadingDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    var isMethodExpanded by rememberSaveable(state.range) { mutableStateOf(false) }

    MuscleLoadingDetailsContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        isMethodExpanded = isMethodExpanded,
        onToggleMethod = { isMethodExpanded = !isMethodExpanded },
        onSelectMode = contract::onSelectMode,
    )
}

@Composable
private fun MuscleLoadingDetailsContent(
    modifier: Modifier = Modifier,
    state: MuscleLoadingDetailsState,
    isMethodExpanded: Boolean,
    onToggleMethod: () -> Unit,
    onSelectMode: (MuscleLoadingDetailsShowingMode) -> Unit,
) {
    val segmentItems = remember {
        MuscleLoadingDetailsShowingMode.entries.map { it to it.text }.toPersistentList()
    }
    val listState = rememberAnchoredLazyListState(behavior = AnchorScrollBehavior.Animated)

    BottomOverlayContainer(
        modifier = modifier,
        overlay = AppTokens.colors.background.dialog,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier.fillMaxSize(),
                state = listState,
                contentPadding = resolvedPadding,
            ) {
                item(key = "range") {
                    DialogRangeHeader(text = state.range.short())
                }

                item(key = "distribution") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    ) {
                        val summary = state.summary
                        if (summary == null) {
                            MuscleLoadingDetailsLoadingContent(modifier = Modifier.fillMaxWidth())
                        } else {
                            if (summary.meta.trainingsCount > 0) {
                                MuscleLoadingCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    summary = summary,
                                    style = MuscleLoadingCardStyle.Summary,
                                )
                            }

                            if (summary.perGroup.entries.isEmpty()) {
                                MuscleLoadingMissingContent(summary = summary)
                            } else {
                                Segment(
                                    modifier = Modifier.fillMaxWidth(),
                                    items = segmentItems,
                                    selected = state.mode,
                                    onSelect = onSelectMode,
                                    segmentWidth = SegmentWidth.EqualFill,
                                    style = SegmentStyle.Outline,
                                )

                                MuscleLoadingImagesRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    summary = summary,
                                    mode = when (state.mode) {
                                        MuscleLoadingDetailsShowingMode.PerGroup -> MuscleLoadingImagesMode.PerGroup
                                        MuscleLoadingDetailsShowingMode.PerMuscle -> MuscleLoadingImagesMode.PerMuscle
                                    },
                                )

                                MuscleLoading(
                                    modifier = Modifier.fillMaxWidth(),
                                    summary = summary,
                                    mode = when (state.mode) {
                                        MuscleLoadingDetailsShowingMode.PerGroup -> MuscleLoadingMode.PerGroup
                                        MuscleLoadingDetailsShowingMode.PerMuscle -> MuscleLoadingMode.PerMuscle
                                    },
                                    style = MuscleLoadingStyle.Breakdown,
                                )
                            }

                            if (summary.meta.trainingsCount > 0) {
                                MuscleLoadingMethodDisclosure(
                                    modifier = Modifier.fillMaxWidth(),
                                    summary = summary,
                                    isExpanded = isMethodExpanded,
                                    onToggle = onToggleMethod,
                                )
                            }
                        }
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@Composable
private fun MuscleLoadingMissingContent(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
) {
    val title = when {
        summary.meta.trainingsCount == 0 -> Res.string.muscle_loading_details_no_workouts_title
        summary.meta.totalSets == 0 -> Res.string.muscle_loading_details_no_sets_title
        else -> Res.string.muscle_loading_details_unknown_title
    }
    val message = when {
        summary.meta.trainingsCount == 0 -> Res.string.muscle_loading_details_no_workouts_message
        summary.meta.totalSets == 0 -> Res.string.muscle_loading_details_no_sets_message
        else -> Res.string.muscle_loading_details_unknown_message
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = AppTokens.strings.res(title),
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            text = AppTokens.strings.res(message),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun MuscleLoadingMethodDisclosure(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val expansionDescription = AppTokens.strings.res(
        if (isExpanded) Res.string.muscle_loading_details_method_expanded
        else Res.string.muscle_loading_details_method_collapsed,
    )

    Column(modifier = modifier) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = expansionDescription },
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.muscle_loading_details_method_title),
                endIcon = ButtonIcon.Icon(
                    value = if (isExpanded) AppTokens.icons.ArrowTop else AppTokens.icons.ArrowDown,
                ),
            ),
            style = ButtonStyle.Tertiary,
            onClick = onToggle,
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(MethodAnimationDurationMillis),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(MethodAnimationDurationMillis)),
            exit = shrinkVertically(
                animationSpec = tween(MethodAnimationDurationMillis),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(MethodAnimationDurationMillis)),
        ) {
            Column(
                modifier = Modifier.padding(top = AppTokens.dp.contentPadding.content),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Text(
                    text = AppTokens.strings.res(Res.string.muscle_loading_details_method_body),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.muscle_loading_details_method_limits),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
                if (summary.meta.unassignedSets > 0) {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.muscle_load_unassigned_sets,
                            summary.meta.unassignedSets,
                        ),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
                if (summary.meta.excludedSets > 0) {
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.muscle_load_excluded_sets,
                            summary.meta.excludedSets,
                        ),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
    }
}

private fun previewState(
    summary: MuscleLoadSummaryState? = stubMuscleLoadSummary(),
    mode: MuscleLoadingDetailsShowingMode = MuscleLoadingDetailsShowingMode.PerGroup,
): MuscleLoadingDetailsState = MuscleLoadingDetailsState(
    range = DateRangeFormatState.of(
        from = LocalDateTime(2026, 10, 11, 0, 0),
        to = LocalDateTime(2026, 10, 17, 23, 59),
    ),
    summary = summary,
    mode = mode,
)

@AppPreview
@Composable
private fun ScreenPerGroupPreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPerMusclePreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(mode = MuscleLoadingDetailsShowingMode.PerMuscle),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun UnknownMuscleDataPreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(summary = stubMuscleLoadSummaryUnknown()),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun LoadingSummaryPreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(summary = null),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun EmptyPeriodPreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(summary = stubMuscleLoadSummaryEmpty()),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun NoAvailableSetsPreview() {
    val summary = stubMuscleLoadSummaryEmpty()
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(
                summary = summary.copy(meta = summary.meta.copy(trainingsCount = 2, excludedSets = 3)),
            ),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun PartialMuscleDataPreview() {
    val summary = stubMuscleLoadSummary()
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(
                summary = summary.copy(
                    meta = summary.meta.copy(excludedSets = 2, unassignedSets = 3, unmeasuredVolumeSets = 4),
                ),
            ),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun SingleGroupPreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(summary = stubMuscleLoadSummarySingleGroup()),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TiedGroupsPreview() {
    val summary = stubMuscleLoadSummary()
    val groups = summary.perGroup.entries.take(2).map { it.copy(value = 50f) }.toPersistentList()
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = previewState(summary = summary.copy(perGroup = summary.perGroup.copy(entries = groups))),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ExpandedMethodPreview() {
    val summary = stubMuscleLoadSummarySingleGroup()
    PreviewContainer {
        BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
            MuscleLoadingDetailsContent(
                modifier = Modifier.fillMaxSize(),
                state = previewState(
                    summary = summary.copy(meta = summary.meta.copy(excludedSets = 2, unassignedSets = 1)),
                ),
                isMethodExpanded = true,
                onToggleMethod = {},
                onSelectMode = MuscleLoadingDetailsContract.Empty::onSelectMode,
            )
        }
    }
}
