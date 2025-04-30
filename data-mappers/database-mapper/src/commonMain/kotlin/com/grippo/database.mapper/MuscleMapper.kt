package com.grippo.database.mapper

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleLoadEnum
import com.grippo.data.features.api.muscle.models.MuscleStatusEnum
import com.grippo.database.entity.MuscleEntity

public fun List<MuscleEntity>.toDomain(): List<Muscle> {
    return mapNotNull { it.toDomain() }
}

public fun MuscleEntity.toDomain(): Muscle {
    return Muscle(
        id = id,
        name = name,
        type = MuscleEnum.of(type),
        status = MuscleStatusEnum.of(status),
        load = MuscleLoadEnum.of(load)
    )
}