package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.database.mapper.equipment.toDomain
import com.grippo.database.models.ExerciseEquipmentWithEquipment

public fun List<ExerciseEquipmentWithEquipment>.toDomain(): List<Equipment> {
    return map { it.toDomain() }
}

public fun ExerciseEquipmentWithEquipment.toDomain(): Equipment {
    return equipment.toDomain()
}