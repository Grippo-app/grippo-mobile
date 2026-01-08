package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetIteration
import kotlin.uuid.Uuid

public fun SetIteration.toEntity(exerciseId: String): com.grippo.services.database.entity.DraftIterationEntity {
    return _root_ide_package_.com.grippo.services.database.entity.DraftIterationEntity(
        id = Uuid.random().toString(),
        exerciseId = exerciseId,
        volume = volume,
        repetitions = repetitions
    )
}