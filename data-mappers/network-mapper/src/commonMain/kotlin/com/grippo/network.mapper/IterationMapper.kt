package com.grippo.network.mapper

import com.grippo.database.entity.IterationEntity
import com.grippo.network.dto.IterationDto

public fun List<IterationDto>.toEntities(): List<IterationEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun IterationDto.toEntityOrNull(): IterationEntity? {
    return IterationEntity(
        id = id ?: return null,
        exerciseId = exerciseId ?: return null,
        weight = weight?.toFloat() ?: return null,
        repetitions = repetitions ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}