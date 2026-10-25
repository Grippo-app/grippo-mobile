package com.grippo.design.components.muscle

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.design.components.cards.selectable.ToggleSelectableCard
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun MuscleSelection(
    modifier: Modifier = Modifier,
    groups: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>>,
    selectedGroupId: String?,
    selectedMuscleIds: ImmutableList<String>,
    onGroupSelect: (String) -> Unit,
    onMuscleSelect: (String) -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    val selectedGroup = remember(selectedGroupId, groups) {
        groups.find { it.id == selectedGroupId }
    }
    val segmentItems = remember(groups) {
        groups.map { it.id to it.type.title() }.toPersistentList()
    }
    val selectedIds = remember(selectedMuscleIds) { selectedMuscleIds.toSet() }

    OverlayContainer(
        modifier = modifier,
        overlay = AppTokens.colors.background.screen,
        bottom = bottom,
        content = { containerModifier, resolvedPadding ->
            BoxWithConstraints(
                modifier = containerModifier.fillMaxSize()
            ) {
                val availableHeight = (maxHeight - resolvedPadding.calculateBottomPadding())
                    .coerceAtLeast(0.dp)
                val dividerHeight = if (groups.isNotEmpty()) {
                    AppTokens.dp.divider.thickness
                } else {
                    0.dp
                }
                val availableHeroHeight = (
                    availableHeight - AppTokens.dp.segment.outline.height -
                        AppTokens.dp.contentPadding.content * 3 -
                        dividerHeight - AppTokens.dp.muscleSelection.listMinHeight
                    ).coerceAtLeast(AppTokens.dp.muscleSelection.heroMinHeight)
                val heroSize = minOf(
                    maxWidth,
                    availableHeight * AppTokens.dp.muscleSelection.heroHeightFraction,
                    AppTokens.dp.muscleSelection.heroMaxHeight,
                    availableHeroHeight
                )

                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedGroup != null) {
                        MusclesImage(
                            modifier = Modifier.size(heroSize),
                            item = selectedGroup,
                            preset = selectedGroup.colorPreset(selectedIds)
                        )
                        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
                    }

                    Segment(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                        items = segmentItems,
                        selected = selectedGroupId,
                        onSelect = onGroupSelect,
                        segmentWidth = SegmentWidth.Unspecified,
                        style = SegmentStyle.Outline
                    )

                    if (groups.isNotEmpty()) {
                        ContentDivider()
                    }

                    key(selectedGroupId) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            state = rememberLazyListState(),
                            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                            contentPadding = PaddingValues(
                                start = AppTokens.dp.screen.horizontalPadding,
                                end = AppTokens.dp.screen.horizontalPadding,
                                top = AppTokens.dp.contentPadding.content,
                                bottom = resolvedPadding.calculateBottomPadding()
                            )
                        ) {
                            items(
                                items = selectedGroup?.muscles.orEmpty(),
                                key = { it.value.id },
                                contentType = { "muscle" }
                            ) { muscle ->
                                ToggleSelectableCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    style = ToggleSelectableCardStyle.Small(muscle.value.type.title().text()),
                                    isSelected = muscle.value.id in selectedIds,
                                    onSelect = { onMuscleSelect(muscle.value.id) },
                                    leading = { MuscleMark(muscle = muscle.value.type) },
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@AppPreview
@Composable
private fun MuscleSelectionPreview() {
    PreviewContainer {
        val groups = stubMuscleGroup()
        MuscleSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = groups,
            selectedGroupId = groups.first().id,
            selectedMuscleIds = groups.first().muscles.map { it.value.id }.toPersistentList(),
            onGroupSelect = {},
            onMuscleSelect = {},
            bottom = {}
        )
    }
}

@AppPreview
@Composable
private fun MuscleSelectionEmptyPreview() {
    PreviewContainer {
        MuscleSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = persistentListOf(),
            selectedGroupId = null,
            selectedMuscleIds = persistentListOf(),
            onGroupSelect = {},
            onMuscleSelect = {},
            bottom = {}
        )
    }
}

@AppPreview
@Composable
private fun MuscleSelectionUnselectedPreview() {
    PreviewContainer {
        val groups = stubMuscleGroup()
        MuscleSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = groups,
            selectedGroupId = groups.first().id,
            selectedMuscleIds = persistentListOf(),
            onGroupSelect = {},
            onMuscleSelect = {},
            bottom = {},
        )
    }
}

@AppPreview
@Composable
private fun MuscleSelectionPartialPreview() {
    PreviewContainer {
        val groups = stubMuscleGroup()
        MuscleSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = groups,
            selectedGroupId = groups.first().id,
            selectedMuscleIds = persistentListOf(groups.first().muscles.first().value.id),
            onGroupSelect = {},
            onMuscleSelect = {},
            bottom = {},
        )
    }
}
