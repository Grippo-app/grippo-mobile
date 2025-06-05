package com.grippo.profile.equipments

import com.grippo.core.models.BaseDirection

internal sealed interface ProfileEquipmentsDirection : BaseDirection {
    data object Back : ProfileEquipmentsDirection
}