package com.grippo.database.mapper.muscles

import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.database.models.MuscleGroupWithMuscles

public fun List<MuscleGroupWithMuscles>.toDomain(): List<MuscleGroup> {
    return mapNotNull { it.toDomain() }
}

public fun MuscleGroupWithMuscles.toDomain(): MuscleGroup {
    return MuscleGroup(
        id = group.id,
        name = group.name,
        muscles = muscles.toDomain(),
        type = MuscleGroupEnum.of(group.type)
    )
}
