package com.grippo.network.mapper

import com.grippo.database.entity.WeightHistoryEntity
import com.grippo.network.dto.WeightHistoryDto

public fun List<WeightHistoryDto>.toEntities(): List<WeightHistoryEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun WeightHistoryDto.toEntityOrNull(): WeightHistoryEntity? {
    return WeightHistoryEntity(
        id = id ?: return null,
        weight = weight?.toFloat() ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}