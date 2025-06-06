package com.grippo.profile.equipments

import com.grippo.core.models.BaseLoader

internal sealed interface ProfileEquipmentsLoader : BaseLoader {
    data object ApplyButton : ProfileEquipmentsLoader
}