package com.grippo.domain.database.settings.training

import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.database.entity.DraftIterationEntity
import kotlin.uuid.Uuid

public fun SetIteration.toEntity(exerciseId: String): DraftIterationEntity {
    return DraftIterationEntity(
        id = Uuid.random().toString(),
        exerciseId = exerciseId,
        volume = volume,
        repetitions = repetitions
    )
}