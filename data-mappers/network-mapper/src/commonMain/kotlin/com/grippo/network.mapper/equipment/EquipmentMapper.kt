package com.grippo.network.mapper.equipment

import com.grippo.database.entity.EquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.EquipmentResponse

public fun List<EquipmentResponse>.toEntities(): List<EquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentResponse.toEntityOrNull(): EquipmentEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "EquipmentDto.id is null"
    } ?: return null

    val entityEquipmentGroupId = AppLogger.checkOrLog(equipmentGroupId) {
        "EquipmentDto.equipmentGroupId is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "EquipmentDto.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "EquipmentDto.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "EquipmentDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "EquipmentDto.updatedAt is null"
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