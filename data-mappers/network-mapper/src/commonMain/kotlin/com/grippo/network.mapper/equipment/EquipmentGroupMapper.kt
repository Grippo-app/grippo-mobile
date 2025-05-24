package com.grippo.network.mapper.equipment

import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.EquipmentGroupResponse

public fun List<EquipmentGroupResponse>.toEntities(): List<EquipmentGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentGroupResponse.toEntityOrNull(): EquipmentGroupEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "EquipmentGroupDto.id is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "EquipmentGroupDto.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "EquipmentGroupDto.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "EquipmentGroupDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "EquipmentGroupDto.updatedAt is null"
    } ?: return null

    return EquipmentGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}