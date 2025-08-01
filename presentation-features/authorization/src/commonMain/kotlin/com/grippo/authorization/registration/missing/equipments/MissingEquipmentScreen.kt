package com.grippo.authorization.registration.missing.equipments

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.equipment.EquipmentRow
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_equipment_description
import com.grippo.design.resources.registration_equipment_title
import com.grippo.state.equipments.stubEquipments
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MissingEquipmentsScreen(
    state: MissingEquipmentsState,
    loaders: ImmutableSet<MissingEquipmentsLoader>,
    contract: MissingEquipmentsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        onBack = contract::onBack,
        style = ToolbarStyle.Transparent
    )

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = AppTokens.dp.contentPadding.content)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_equipment_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_equipment_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {

            val segmentItems = remember(state.suggestions) {
                state.suggestions.map { it.id to UiText.Str(it.name) }.toPersistentList()
            }

            Segment(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = segmentItems,
                selected = state.selectedGroupId,
                onSelect = contract::onGroupClick,
                segmentWidth = SegmentWidth.Unspecified,
            )

            val equipments = remember(state.selectedGroupId, state.suggestions) {
                state.suggestions.find { it.id == state.selectedGroupId }?.equipments.orEmpty()
            }

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    top = AppTokens.dp.contentPadding.content
                ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                items(
                    items = equipments,
                    key = { it.id },
                    contentType = { it::class }) { equipment ->
                    EquipmentRow(
                        equipment = equipment,
                        selectedEquipmentIds = state.selectedEquipmentIds,
                        selectEquipment = contract::onEquipmentClick,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            style = ButtonStyle.Primary,
            onClick = contract::onNextClick
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewSelected() {
    PreviewContainer {
        MissingEquipmentsScreen(
            state = MissingEquipmentsState(
                suggestions = stubEquipments(),
                selectedEquipmentIds = stubEquipments()
                    .map { it.equipments.map { it.id } }
                    .flatten()
                    .take(3).toPersistentList(),
                selectedGroupId = stubEquipments().firstOrNull()?.id
            ),
            loaders = persistentSetOf(),
            contract = MissingEquipmentsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewUnselected() {
    PreviewContainer {
        MissingEquipmentsScreen(
            state = MissingEquipmentsState(
                suggestions = stubEquipments(),
                selectedEquipmentIds = persistentListOf(),
                selectedGroupId = stubEquipments().firstOrNull()?.id
            ),
            loaders = persistentSetOf(),
            contract = MissingEquipmentsContract.Empty
        )
    }
}