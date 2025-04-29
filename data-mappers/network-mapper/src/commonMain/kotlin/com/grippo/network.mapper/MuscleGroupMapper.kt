package com.grippo.network.mapper

import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.network.dto.MuscleGroupDto

public fun MuscleGroupDto.toMuscles(): List<MuscleEntity> {
    return muscles.orEmpty().mapNotNull { it.toEntityOrNull() }
}

public fun MuscleGroupDto.toEntityOrNull(): MuscleGroupEntity? {
    return MuscleGroupEntity(
        id = id ?: return null,
        name = name ?: return null,
        type = type ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}