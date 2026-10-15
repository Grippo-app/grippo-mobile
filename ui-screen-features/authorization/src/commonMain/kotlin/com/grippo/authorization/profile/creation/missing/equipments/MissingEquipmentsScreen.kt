package com.grippo.authorization.profile.creation.missing.equipments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.design.components.brand.BrandHeader
import com.grippo.design.components.brand.BrandHeaderStyle
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.equipment.EquipmentSelection
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.registration_equipment_description
import com.grippo.design.resources.provider.registration_equipment_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MissingEquipmentsScreen(
    state: MissingEquipmentsState,
    loaders: ImmutableSet<MissingEquipmentsLoader>,
    contract: MissingEquipmentsContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        leading = Leading.Back(contract::onBack),
        style = ToolbarStyle.Transparent
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandHeader(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.registration_equipment_title),
            description = AppTokens.strings.res(Res.string.registration_equipment_description),
            style = BrandHeaderStyle.Compact,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        EquipmentSelection(
            modifier = Modifier.fillMaxWidth().weight(1f),
            groups = state.suggestions,
            selectedGroupId = state.selectedGroupId,
            selectedEquipmentIds = state.selectedEquipmentIds,
            onGroupSelect = contract::onGroupClick,
            onEquipmentSelect = contract::onEquipmentClick,
            bottom = {
                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                Button(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                        .fillMaxWidth(),
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.continue_btn),
                    ),
                    style = ButtonStyle.Primary,
                    onClick = contract::onNextClick
                )

                Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

                Spacer(Modifier.navigationBarsPadding())
            }
        )
    }
}

@AppPreview
@Composable
private fun MissingEquipmentsSelectedPreview() {
    PreviewContainer {
        val groups = stubEquipments()
        MissingEquipmentsScreen(
            state = MissingEquipmentsState(
                suggestions = groups,
                selectedEquipmentIds = groups.first().equipments.take(3).map { it.id }.toPersistentList(),
                selectedGroupId = groups.first().id
            ),
            loaders = persistentSetOf(),
            contract = MissingEquipmentsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun MissingEquipmentsUnselectedPreview() {
    PreviewContainer {
        val groups = stubEquipments()
        MissingEquipmentsScreen(
            state = MissingEquipmentsState(
                suggestions = groups,
                selectedEquipmentIds = persistentListOf(),
                selectedGroupId = groups.first().id
            ),
            loaders = persistentSetOf(),
            contract = MissingEquipmentsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun MissingEquipmentsEmptyCatalogPreview() {
    PreviewContainer {
        MissingEquipmentsScreen(
            state = MissingEquipmentsState(),
            loaders = persistentSetOf(),
            contract = MissingEquipmentsContract.Empty,
        )
    }
}
