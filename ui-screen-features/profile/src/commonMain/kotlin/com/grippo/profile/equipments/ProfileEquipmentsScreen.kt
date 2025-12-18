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
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.equipment.EquipmentRow
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.apply_btn
import com.grippo.design.resources.provider.equipments
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ProfileEquipmentsScreen(
    state: ProfileEquipmentsState,
    loaders: ImmutableSet<ProfileEquipmentsLoader>,
    contract: ProfileEquipmentsContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    )
) {
    val segmentItems = remember(state.suggestions) {
        state.suggestions.map { it.id to it.type.title() }.toPersistentList()
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.equipments),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
        content = {
            Segment(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = segmentItems,
                selected = state.selectedGroupId,
                onSelect = contract::onSelectGroup,
                segmentWidth = SegmentWidth.Unspecified,
                style = SegmentStyle.Outline
            )
        }
    )

    val equipments = remember(state.selectedGroupId, state.suggestions) {
        state.suggestions.find { it.id == state.selectedGroupId }?.equipments.orEmpty()
    }

    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content
    )

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                contentPadding = resolvedPadding
            ) {
                items(items = equipments, key = { it.id }) { equipment ->
                    EquipmentRow(
                        equipment = equipment,
                        selectedEquipmentIds = state.selectedEquipmentIds,
                        selectEquipment = contract::onSelectEquipment,
                    )
                }
            }
        },
        bottom = {
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
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.apply_btn),
                ),
                style = ButtonStyle.Primary,
                state = buttonState,
                onClick = contract::onApply
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
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
