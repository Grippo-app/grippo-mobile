package com.grippo.entity.domain.user

import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.services.database.entity.WeightHistoryEntity
import com.grippo.toolkit.date.utils.DateTimeUtils

public fun List<WeightHistoryEntity>.toDomain(): List<WeightHistory> {
    return mapNotNull { it.toDomain() }
}

public fun WeightHistoryEntity.toDomain(): WeightHistory {
    return WeightHistory(
        id = id,
        weight = weight,
        createdAt = DateTimeUtils.toLocalDateTime(createdAt)
    )
}