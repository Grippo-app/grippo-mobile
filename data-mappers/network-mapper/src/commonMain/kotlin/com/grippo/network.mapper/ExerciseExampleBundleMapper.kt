package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.network.dto.ExerciseExampleBundleDto

public fun List<ExerciseExampleBundleDto>.toEntities(): List<ExerciseExampleBundleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleBundleDto.toEntityOrNull(): ExerciseExampleBundleEntity? {
    return ExerciseExampleBundleEntity(
        id = id ?: return null,
        exerciseExampleId = exerciseExampleId ?: return null,
        muscleId = muscleId ?: return null,
        percentage = percentage ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}