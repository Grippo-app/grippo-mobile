package com.grippo.authorization.profile.creation.missing.equipments

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MissingEquipmentsScreen(
    state: MissingEquipmentsState,
    loaders: ImmutableSet<MissingEquipmentsLoader>,
    contract: MissingEquipmentsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

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
