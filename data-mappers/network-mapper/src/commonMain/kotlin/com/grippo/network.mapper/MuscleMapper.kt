package com.grippo.network.mapper

import com.grippo.database.entity.MuscleEntity
import com.grippo.network.dto.MuscleDto

public fun List<MuscleDto>.toEntities(): List<MuscleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleDto.toEntityOrNull(): MuscleEntity? {
    return MuscleEntity(
        id = id ?: return null,
        muscleGroupId = muscleGroupId ?: return null,
        name = name ?: return null,
        type = type ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}