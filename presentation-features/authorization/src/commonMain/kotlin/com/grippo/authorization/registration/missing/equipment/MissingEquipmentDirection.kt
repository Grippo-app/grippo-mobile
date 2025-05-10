package com.grippo.authorization.registration.missing.equipment

import com.grippo.core.models.BaseDirection

internal sealed interface MissingEquipmentDirection : BaseDirection {
    data class Completed(
        val equipmentIds: List<String>
    ) : MissingEquipmentDirection
}