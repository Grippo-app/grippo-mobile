package com.grippo.profile.equipments

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileEquipmentsDirection : BaseDirection {
    data object Back : ProfileEquipmentsDirection
}