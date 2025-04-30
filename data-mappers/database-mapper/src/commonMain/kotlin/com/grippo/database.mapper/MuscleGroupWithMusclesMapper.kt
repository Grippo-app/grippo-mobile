package com.grippo.database.mapper

import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.database.models.MuscleGroupWithMuscles

public fun List<MuscleGroupWithMuscles>.toDomain(): List<MuscleGroup> {
    return mapNotNull { it.daoToDomain() }
}

public fun MuscleGroupWithMuscles.daoToDomain(): MuscleGroup {
    return MuscleGroup(
        id = group.id,
        name = group.name,
        muscles = muscles.toDomain(),
        type = MuscleGroupEnum.of(group.type)
    )
}
