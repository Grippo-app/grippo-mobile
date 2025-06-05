package com.grippo.profile.equipment

import com.grippo.core.models.BaseDirection

internal sealed interface ProfileEquipmentDirection : BaseDirection {
    data object Back : ProfileEquipmentDirection
}