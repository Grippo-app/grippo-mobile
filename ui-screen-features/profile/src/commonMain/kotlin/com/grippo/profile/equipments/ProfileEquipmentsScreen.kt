package com.grippo.profile.equipments

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.equipment.EquipmentSelection
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.apply_btn
import com.grippo.design.resources.provider.equipments
import com.grippo.design.resources.provider.registration_equipment_description
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
        value = AppTokens.colors.background.screen
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.equipments),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
        content = {
            Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

            Text(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.registration_equipment_description),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))
        }
    )

    EquipmentSelection(
        modifier = Modifier.fillMaxWidth().weight(1f),
        groups = state.suggestions,
        selectedGroupId = state.selectedGroupId,
        selectedEquipmentIds = state.selectedEquipmentIds,
        onGroupSelect = contract::onSelectGroup,
        onEquipmentSelect = contract::onSelectEquipment,
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            val buttonState = remember(loaders, state.suggestions) {
                when {
                    loaders.contains(ProfileEquipmentsLoader.ApplyButton) -> ButtonState.Loading
                    state.suggestions.all { it.equipments.isEmpty() } -> ButtonState.Disabled
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
        val groups = stubEquipments()
        ProfileEquipmentsScreen(
            state = ProfileEquipmentsState(
                suggestions = groups,
                selectedGroupId = groups.first().id,
                selectedEquipmentIds = groups.first().equipments.take(3).map { it.id }.toPersistentList(),
            ),
            loaders = persistentSetOf(),
            contract = ProfileEquipmentsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewLoading() {
    PreviewContainer {
        val groups = stubEquipments()
        ProfileEquipmentsScreen(
            state = ProfileEquipmentsState(
                suggestions = groups,
                selectedGroupId = groups.first().id,
            ),
            loaders = persistentSetOf(ProfileEquipmentsLoader.ApplyButton),
            contract = ProfileEquipmentsContract.Empty
        )
    }
}
