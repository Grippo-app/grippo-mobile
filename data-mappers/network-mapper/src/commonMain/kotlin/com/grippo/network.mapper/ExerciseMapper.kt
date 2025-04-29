package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseEntity
import com.grippo.database.entity.IterationEntity
import com.grippo.network.dto.ExerciseDto

public fun ExerciseDto.toIterations(): List<IterationEntity> {
    return iterations.mapNotNull { it.toEntityOrNull() }
}

public fun List<ExerciseDto>.toEntities(): List<ExerciseEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseDto.toEntityOrNull(): ExerciseEntity? {
    return ExerciseEntity(
        id = id ?: return null,
        trainingId = trainingId ?: return null,
        exerciseExampleId = exerciseExampleId,
        name = name ?: return null,
        volume = volume?.toFloat() ?: return null,
        repetitions = repetitions ?: return null,
        intensity = intensity?.toFloat() ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}
