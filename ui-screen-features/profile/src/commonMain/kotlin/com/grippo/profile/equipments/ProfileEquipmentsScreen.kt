package com.grippo.profile.equipments

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileEquipmentsScreen(
    state: ProfileEquipmentsState,
    loaders: ImmutableSet<ProfileEquipmentsLoader>,
    contract: ProfileEquipmentsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

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
