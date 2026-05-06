package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.Iteration
import com.grippo.services.database.entity.IterationEntity

public fun List<IterationEntity>.toDomain(): List<Iteration> {
    return map { it.toDomain() }
}

public fun IterationEntity.toDomain(): Iteration {
    return Iteration(
        id = id,
        externalWeight = externalWeight,
        extraWeight = extraWeight,
        assistWeight = assistWeight,
        bodyWeight = bodyWeight,
        repetitions = repetitions,
        bodyMultiplier = bodyMultiplier
    )
}