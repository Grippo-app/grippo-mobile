package com.grippo.network.database.user

import com.grippo.database.entity.WeightHistoryEntity
import com.grippo.logger.AppLogger
import com.grippo.network.user.WeightHistoryResponse

public fun List<WeightHistoryResponse>.toEntities(): List<WeightHistoryEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun WeightHistoryResponse.toEntityOrNull(): WeightHistoryEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "WeightHistoryResponse.id is null"
    } ?: return null

    val entityWeight = AppLogger.Mapping.log(weight) {
        "WeightHistoryResponse.weight is null"
    } ?: return null

    val created = AppLogger.Mapping.log(createdAt) {
        "WeightHistoryResponse.createdAt is null"
    } ?: return null

    val updated = AppLogger.Mapping.log(updatedAt) {
        "WeightHistoryResponse.updatedAt is null"
    } ?: return null

    return WeightHistoryEntity(
        id = entityId,
        weight = entityWeight,
        createdAt = created,
        updatedAt = updated,
    )
}