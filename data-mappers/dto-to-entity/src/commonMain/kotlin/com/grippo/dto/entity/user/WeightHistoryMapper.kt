package com.grippo.dto.entity.user

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.user.WeightHistoryResponse>.toEntities(): List<com.grippo.services.database.entity.WeightHistoryEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.user.WeightHistoryResponse.toEntityOrNull(): com.grippo.services.database.entity.WeightHistoryEntity? {
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

    return _root_ide_package_.com.grippo.services.database.entity.WeightHistoryEntity(
        id = entityId,
        weight = entityWeight,
        createdAt = created,
        updatedAt = updated,
    )
}