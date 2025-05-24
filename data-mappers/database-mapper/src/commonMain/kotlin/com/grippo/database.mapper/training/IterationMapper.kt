package com.grippo.database.mapper.training

import com.grippo.data.features.api.training.models.Iteration
import com.grippo.database.entity.IterationEntity

public fun List<IterationEntity>.toDomain(): List<Iteration> {
    return map { it.toDomain() }
}

public fun IterationEntity.toDomain(): Iteration {
    return Iteration(
        id = id,
        weight = weight,
        repetitions = repetitions
    )
}