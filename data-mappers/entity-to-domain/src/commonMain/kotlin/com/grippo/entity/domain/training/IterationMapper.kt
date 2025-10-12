package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.database.entity.DraftIterationEntity
import com.grippo.database.entity.IterationEntity

public fun List<IterationEntity>.toDomain(): List<Iteration> {
    return map { it.toDomain() }
}

public fun IterationEntity.toDomain(): Iteration {
    return Iteration(
        id = id,
        volume = volume,
        repetitions = repetitions
    )
}

public fun List<DraftIterationEntity>.toSetDomain(): List<SetIteration> {
    return map { it.toSetDomain() }
}

public fun DraftIterationEntity.toSetDomain(): SetIteration {
    return SetIteration(
        volume = volume,
        repetitions = repetitions
    )
}