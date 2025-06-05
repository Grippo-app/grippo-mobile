package com.grippo.profile.equipments

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileEquipmentsScreen(
    state: ProfileEquipmentsState,
    loaders: ImmutableSet<ProfileEquipmentsLoader>,
    contract: ProfileEquipmentsContract
) = BaseComposeScreen {
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileEquipmentsScreen(
            state = ProfileEquipmentsState,
            loaders = persistentSetOf(),
            contract = ProfileEquipmentsContract.Empty
        )
    }
}