package com.grippo.entity.domain.muscles

import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.services.database.models.MuscleGroupWithMuscles
import com.grippo.toolkit.logger.AppLogger

public fun List<MuscleGroupWithMuscles>.toDomain(): List<MuscleGroup> {
    return mapNotNull { it.toDomain() }
}

public fun MuscleGroupWithMuscles.toDomain(): MuscleGroup? {
    val mappedType = AppLogger.Mapping.log(MuscleGroupEnum.of(group.type)) {
        "MuscleGroupWithMuscles ${group.id} has unrecognized type: ${group.type}"
    } ?: return null

    return MuscleGroup(
        id = group.id,
        name = group.name,
        muscles = muscles.toDomain(),
        type = mappedType
    )
}
