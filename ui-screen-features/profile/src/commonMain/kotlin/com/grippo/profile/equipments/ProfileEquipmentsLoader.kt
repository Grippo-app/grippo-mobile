package com.grippo.profile.equipments

import androidx.compose.runtime.Immutable
import com.grippo.core.models.BaseLoader

@Immutable
internal sealed interface ProfileEquipmentsLoader : BaseLoader {
    @Immutable
    data object ApplyButton : ProfileEquipmentsLoader
}