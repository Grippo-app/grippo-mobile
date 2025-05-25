package com.grippo.network.mapper.equipment

import com.grippo.database.entity.EquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.EquipmentResponse

public fun List<EquipmentResponse>.toEntities(): List<EquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentResponse.toEntityOrNull(): EquipmentEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "EquipmentResponse.id is null"
    } ?: return null

    val entityEquipmentGroupId = AppLogger.checkOrLog(equipmentGroupId) {
        "EquipmentResponse.equipmentGroupId is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "EquipmentResponse.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "EquipmentResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "EquipmentResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "EquipmentResponse.updatedAt is null"
    } ?: return null

    return EquipmentEntity(
        id = entityId,
        equipmentGroupId = entityEquipmentGroupId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}