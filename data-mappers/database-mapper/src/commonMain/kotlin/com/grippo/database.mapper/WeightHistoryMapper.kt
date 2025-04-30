package com.grippo.database.mapper

import com.grippo.data.features.api.user.models.WeightHistory
import com.grippo.database.entity.WeightHistoryEntity

public fun WeightHistoryEntity.toDomain(): WeightHistory {
    return WeightHistory(
        id = id,
        weight = weight,
        createdAt = createdAt
    )
}