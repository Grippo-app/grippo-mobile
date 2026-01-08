package com.grippo.entity.domain.equipment

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.database.models.ExerciseEquipmentWithEquipment>.toDomain(): List<Equipment> {
    return mapNotNull { it.toDomain() }
}

public fun com.grippo.services.database.models.ExerciseEquipmentWithEquipment.toDomain(): Equipment? {
    val equipment = AppLogger.Mapping.log(this@toDomain.equipment.toDomain()) {
        "ExerciseEquipmentWithEquipment: equipment mapping failed for equipmentId=${this@toDomain.equipment.id}"
    }

    return equipment
}