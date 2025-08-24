package com.grippo.authorization.registration.missing.equipments

import com.grippo.core.models.BaseDirection

internal sealed interface MissingEquipmentsDirection : BaseDirection {
    data class Completed(val missingEquipmentIds: List<String>) : MissingEquipmentsDirection
    data object Back : MissingEquipmentsDirection
}