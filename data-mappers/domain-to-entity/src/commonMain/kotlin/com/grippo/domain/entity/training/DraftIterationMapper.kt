package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.services.database.entity.DraftIterationEntity

public fun SetIteration.toEntity(id: String, exerciseId: String): DraftIterationEntity {
    return DraftIterationEntity(
        id = id,
        exerciseId = exerciseId,
        externalWeight = externalWeight,
        extraWeight = extraWeight,
        assistWeight = assistWeight,
        bodyWeight = bodyWeight,
        bodyMultiplier = bodyMultiplier,
        repetitions = repetitions,
    )
}