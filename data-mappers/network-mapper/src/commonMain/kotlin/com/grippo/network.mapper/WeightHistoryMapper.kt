package com.grippo.network.mapper

import com.grippo.database.entity.WeightHistoryEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.WeightHistoryResponse

public fun List<WeightHistoryResponse>.toEntities(): List<WeightHistoryEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun WeightHistoryResponse.toEntityOrNull(): WeightHistoryEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "WeightHistoryDto.id is null"
    } ?: return null

    val entityWeight = AppLogger.checkOrLog(weight) {
        "WeightHistoryDto.weight is null"
    } ?: return null

    val created = AppLogger.checkOrLog(createdAt) {
        "WeightHistoryDto.createdAt is null"
    } ?: return null

    val updated = AppLogger.checkOrLog(updatedAt) {
        "WeightHistoryDto.updatedAt is null"
    } ?: return null

    return WeightHistoryEntity(
        id = entityId,
        weight = entityWeight,
        createdAt = created,
        updatedAt = updated,
    )
}