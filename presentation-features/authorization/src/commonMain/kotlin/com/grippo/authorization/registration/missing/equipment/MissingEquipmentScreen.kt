package com.grippo.authorization.registration.missing.equipment

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.authorization.registration.missing.equipment.internal.EquipmentRow
import com.grippo.authorization.registration.missing.equipment.internal.EquipmentsSkeleton
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.segment.ThumbPosition
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_equipment_description
import com.grippo.design.resources.registration_equipment_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MissingEquipmentScreen(
    state: MissingEquipmentState,
    loaders: ImmutableSet<MissingEquipmentLoader>,
    contract: MissingEquipmentContract
) = BaseComposeScreen {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = AppTokens.dp.paddings.screenHorizontal,
                vertical = AppTokens.dp.paddings.screenVertical
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(60.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_equipment_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_equipment_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(20.dp))

        Crossfade(
            targetState = state.suggestions.isEmpty() && loaders.contains(MissingEquipmentLoader.EquipmentList),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { loading ->
            when (loading) {
                true -> {
                    EquipmentsSkeleton(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }

                false -> {
                    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {

                        val segmentItems = remember(state.suggestions) {
                            state.suggestions.map { it.id to it.name }.toPersistentList()
                        }

                        Segment(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            items = segmentItems,
                            selected = state.selectedGroupId,
                            onSelect = contract::selectGroup,
                            segmentWidth = SegmentWidth.Unspecified,
                            thumbPosition = ThumbPosition.Bottom
                        )

                        Spacer(modifier = Modifier.size(10.dp))

                        val equipments = remember(state.selectedGroupId, state.suggestions) {
                            state.suggestions.find { it.id == state.selectedGroupId }?.equipments.orEmpty()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                        ) {
                            items(equipments, key = { it.id }) { equipment ->
                                EquipmentRow(
                                    equipment = equipment,
                                    selectedEquipmentIds = state.selectedEquipmentIds,
                                    selectEquipment = contract::selectEquipment,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            style = ButtonStyle.Primary,
            onClick = contract::next
        )
    }
}