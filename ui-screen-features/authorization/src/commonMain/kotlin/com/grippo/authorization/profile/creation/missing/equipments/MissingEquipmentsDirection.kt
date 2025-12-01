package com.grippo.authorization.profile.creation.missing.equipments

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface MissingEquipmentsDirection : BaseDirection {
    data class Completed(val missingEquipmentIds: List<String>) : MissingEquipmentsDirection
    data object Back : MissingEquipmentsDirection
}