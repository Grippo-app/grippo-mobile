package com.grippo.database.domain.exercise.equipment

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.database.models.ExerciseEquipmentWithEquipment
import com.grippo.database.domain.equipment.toDomain
import com.grippo.logger.AppLogger

public fun List<ExerciseEquipmentWithEquipment>.toDomain(): List<Equipment> {
    return mapNotNull { it.toDomain() }
}

public fun ExerciseEquipmentWithEquipment.toDomain(): Equipment? {
    val equipment = AppLogger.Mapping.log(this@toDomain.equipment.toDomain()) {
        "ExerciseEquipmentWithEquipment: equipment mapping failed for equipmentId=${this@toDomain.equipment.id}"
    }

    return equipment
}