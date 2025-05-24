package com.grippo.database.mapper.user

import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.database.entity.WeightHistoryEntity

public fun List<WeightHistoryEntity>.toDomain(): List<WeightHistory> {
    return mapNotNull { it.toDomain() }
}

public fun WeightHistoryEntity.toDomain(): WeightHistory {
    return WeightHistory(
        id = id,
        weight = weight,
        createdAt = createdAt
    )
}