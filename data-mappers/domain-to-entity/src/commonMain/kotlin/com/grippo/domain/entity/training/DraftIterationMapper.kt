package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.services.database.entity.DraftIterationEntity
import kotlin.uuid.Uuid

public fun SetIteration.toEntity(exerciseId: String): DraftIterationEntity {
    return DraftIterationEntity(
        id = Uuid.random().toString(),
        exerciseId = exerciseId,
        externalWeight = externalWeight,
        extraWeight = extraWeight,
        assistWeight = assistWeight,
        bodyWeight = bodyWeight,
        bodyMultiplier = bodyMultiplier,
        repetitions = repetitions
    )
}