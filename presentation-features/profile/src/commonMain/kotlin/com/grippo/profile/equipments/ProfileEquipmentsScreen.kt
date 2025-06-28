package com.grippo.profile.equipments

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.equipment.EquipmentRow
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.apply_btn
import com.grippo.design.resources.equipments
import com.grippo.presentation.api.equipment.models.stubEquipments
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ProfileEquipmentsScreen(
    state: ProfileEquipmentsState,
    loaders: ImmutableSet<ProfileEquipmentsLoader>,
    contract: ProfileEquipmentsContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {

    val segmentItems = remember(state.suggestions) {
        state.suggestions.map { it.id to UiText.Str(it.name) }.toPersistentList()
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.equipments),
        onBack = contract::back,
        content = {
            Segment(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = segmentItems,
                selected = state.selectedGroupId,
                onSelect = contract::selectGroup,
                segmentWidth = SegmentWidth.Unspecified,
            )

        }
    )

    val equipments = remember(state.selectedGroupId, state.suggestions) {
        state.suggestions.find { it.id == state.selectedGroupId }?.equipments.orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.content,
            start = AppTokens.dp.screen.horizontalPadding,
            end = AppTokens.dp.screen.horizontalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        items(equipments, key = { it.id }) { equipment ->
            EquipmentRow(
                equipment = equipment,
                selectedEquipmentIds = state.selectedEquipmentIds,
                selectEquipment = contract::selectEquipment,
            )
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val buttonState = remember(loaders) {
        when {
            loaders.contains(ProfileEquipmentsLoader.ApplyButton) -> ButtonState.Loading
            else -> ButtonState.Enabled
        }
    }

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.apply_btn),
        style = ButtonStyle.Primary,
        state = buttonState,
        onClick = contract::apply
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileEquipmentsScreen(
            state = ProfileEquipmentsState(
                suggestions = stubEquipments()
            ),
            loaders = persistentSetOf(),
            contract = ProfileEquipmentsContract.Empty
        )
    }
}