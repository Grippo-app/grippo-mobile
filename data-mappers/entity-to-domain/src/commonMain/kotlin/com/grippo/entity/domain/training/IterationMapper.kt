package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.SetIteration

public fun List<com.grippo.services.database.entity.IterationEntity>.toDomain(): List<Iteration> {
    return map { it.toDomain() }
}

public fun com.grippo.services.database.entity.IterationEntity.toDomain(): Iteration {
    return Iteration(
        id = id,
        volume = volume,
        repetitions = repetitions
    )
}

public fun List<com.grippo.services.database.entity.DraftIterationEntity>.toSetDomain(): List<SetIteration> {
    return map { it.toSetDomain() }
}

public fun com.grippo.services.database.entity.DraftIterationEntity.toSetDomain(): SetIteration {
    return SetIteration(
        volume = volume,
        repetitions = repetitions
    )
}