package com.grippo.network.mapper.equipment

import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.equipment.EquipmentGroupResponse

public fun List<EquipmentGroupResponse>.toEntities(): List<EquipmentGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentGroupResponse.toEntityOrNull(): EquipmentGroupEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "EquipmentGroupResponse.id is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "EquipmentGroupResponse.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "EquipmentGroupResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "EquipmentGroupResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "EquipmentGroupResponse.updatedAt is null"
    } ?: return null

    return EquipmentGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}