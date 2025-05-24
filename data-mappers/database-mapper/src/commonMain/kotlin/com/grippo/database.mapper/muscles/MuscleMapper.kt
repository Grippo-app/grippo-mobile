package com.grippo.database.mapper.muscles

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.database.entity.MuscleEntity

public fun List<MuscleEntity>.toDomain(): List<Muscle> {
    return mapNotNull { it.toDomain() }
}

public fun MuscleEntity.toDomain(): Muscle {
    return Muscle(
        id = id,
        name = name,
        type = MuscleEnum.of(type),
    )
}