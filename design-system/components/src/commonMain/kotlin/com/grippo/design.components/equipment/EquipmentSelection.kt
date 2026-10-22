package com.grippo.design.components.equipment

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentGroupEnumState
import com.grippo.core.state.equipments.EquipmentGroupState
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.frames.BottomOverlayContainer
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
public fun EquipmentSelection(
    modifier: Modifier = Modifier,
    groups: ImmutableList<EquipmentGroupState>,
    selectedGroupId: String?,
    selectedEquipmentIds: ImmutableList<String>,
    onGroupSelect: (String) -> Unit,
    onEquipmentSelect: (String) -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    val segmentItems = remember(groups) {
        groups.map { it.id to it.type.title() }.toPersistentList()
    }
    val equipments = remember(selectedGroupId, groups) {
        groups.find { it.id == selectedGroupId }?.equipments.orEmpty()
    }
    val selectedIds = remember(selectedEquipmentIds) { selectedEquipmentIds.toSet() }

    Column(modifier = modifier) {
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

        BottomOverlayContainer(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                start = AppTokens.dp.screen.horizontalPadding,
                end = AppTokens.dp.screen.horizontalPadding,
                top = AppTokens.dp.contentPadding.content
            ),
            overlay = AppTokens.colors.background.screen,
            content = { containerModifier, resolvedPadding ->
                key(selectedGroupId) {
                    LazyColumn(
                        modifier = containerModifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                        contentPadding = resolvedPadding
                    ) {
                        items(
                            items = equipments,
                            key = { it.id },
                            contentType = { "equipment" }
                        ) { equipment ->
                            EquipmentCard(
                                value = equipment,
                                style = EquipmentCardStyle.Selection(
                                    isSelected = equipment.id in selectedIds,
                                    onSelect = { onEquipmentSelect(equipment.id) },
                                ),
                            )
                        }
                    }
                }
            },
            bottom = bottom,
        )
    }
}

@AppPreview
@Composable
private fun EquipmentSelectionPreview() {
    PreviewContainer {
        val groups = stubEquipments()
        val freeWeight = groups.first { it.type == EquipmentGroupEnumState.FREE_WEIGHT }
        val kettlebells = freeWeight.equipments.first { it.type == EquipmentEnumState.KETTLEBELLS }
        EquipmentSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = groups,
            selectedGroupId = freeWeight.id,
            selectedEquipmentIds = persistentListOf(kettlebells.id),
            onGroupSelect = {},
            onEquipmentSelect = {},
            bottom = {},
        )
    }
}

@AppPreview
@Composable
private fun EquipmentSelectionMachinesPreview() {
    PreviewContainer {
        val groups = stubEquipments()
        val machines = groups.first { it.type == EquipmentGroupEnumState.MACHINES }
        val forearm = machines.equipments.first { it.type == EquipmentEnumState.BUTTERFLY_FOREARM }
        val assisted = machines.equipments.first { it.type == EquipmentEnumState.ASSISTED_PULL_UP_DIP }
        val row = machines.equipments.first { it.type == EquipmentEnumState.PLATE_LOADED_ROW }
        val beltSquat = machines.equipments.first { it.type == EquipmentEnumState.BELT_SQUAT }
        val seatedDip = machines.equipments.first {
            it.type == EquipmentEnumState.PLATE_LOADED_SEATED_DIP
        }
        EquipmentSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = groups,
            selectedGroupId = machines.id,
            selectedEquipmentIds = persistentListOf(
                seatedDip.id, beltSquat.id, row.id, assisted.id, forearm.id
            ),
            onGroupSelect = {},
            onEquipmentSelect = {},
            bottom = {},
        )
    }
}

@AppPreview
@Composable
private fun EquipmentSelectionEmptyGroupPreview() {
    PreviewContainer {
        val catalog = stubEquipments()
        val emptyGroup = catalog.first().copy(equipments = persistentListOf())
        val groups = catalog.toPersistentList().replacingAt(index = 0, element = emptyGroup)
        EquipmentSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = groups,
            selectedGroupId = emptyGroup.id,
            selectedEquipmentIds = persistentListOf(),
            onGroupSelect = {},
            onEquipmentSelect = {},
            bottom = {},
        )
    }
}

@AppPreview
@Composable
private fun EquipmentSelectionEmptyPreview() {
    PreviewContainer {
        EquipmentSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = persistentListOf(),
            selectedGroupId = null,
            selectedEquipmentIds = persistentListOf(),
            onGroupSelect = {},
            onEquipmentSelect = {},
            bottom = {},
        )
    }
}
