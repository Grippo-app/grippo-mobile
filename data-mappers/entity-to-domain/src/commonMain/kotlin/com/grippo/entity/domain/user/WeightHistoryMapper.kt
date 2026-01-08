package com.grippo.entity.domain.user

import com.grippo.data.features.api.weight.history.models.WeightHistory

public fun List<com.grippo.services.database.entity.WeightHistoryEntity>.toDomain(): List<WeightHistory> {
    return mapNotNull { it.toDomain() }
}

public fun com.grippo.services.database.entity.WeightHistoryEntity.toDomain(): WeightHistory {
    return WeightHistory(
        id = id,
        weight = weight,
        createdAt = createdAt
    )
}