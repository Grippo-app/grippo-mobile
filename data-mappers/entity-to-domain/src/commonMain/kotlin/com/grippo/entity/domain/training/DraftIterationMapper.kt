package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.services.database.entity.DraftIterationEntity

public fun List<DraftIterationEntity>.toSetDomain(): List<SetIteration> {
    return map { it.toSetDomain() }
}

public fun DraftIterationEntity.toSetDomain(): SetIteration {
    return SetIteration(
        externalWeight = externalWeight,
        extraWeight = extraWeight,
        assistWeight = assistWeight,
        bodyWeight = bodyWeight,
        repetitions = repetitions,
        bodyMultiplier = bodyMultiplier,
    )
}