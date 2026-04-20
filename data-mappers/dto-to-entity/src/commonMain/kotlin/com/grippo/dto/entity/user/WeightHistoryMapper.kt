package com.grippo.dto.entity.user

import com.grippo.services.backend.dto.user.WeightHistoryResponse
import com.grippo.services.database.entity.WeightHistoryEntity
import com.grippo.toolkit.logger.AppLogger

public fun List<WeightHistoryResponse>.toEntities(userId: String): List<WeightHistoryEntity> {
    return mapNotNull { it.toEntityOrNull(userId) }
}

public fun WeightHistoryResponse.toEntityOrNull(userId: String): WeightHistoryEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "WeightHistoryResponse.id is null for user $userId"
    } ?: return null

    val entityWeight = AppLogger.Mapping.log(weight) {
        "WeightHistoryResponse.weight is null for user $userId"
    } ?: return null

    val created = AppLogger.Mapping.log(createdAt) {
        "WeightHistoryResponse.createdAt is null for user $userId"
    } ?: return null

    val updated = AppLogger.Mapping.log(updatedAt) {
        "WeightHistoryResponse.updatedAt is null for user $userId"
    } ?: return null

    return WeightHistoryEntity(
        id = entityId,
        userId = userId,
        weight = entityWeight,
        createdAt = created,
        updatedAt = updated,
    )
}