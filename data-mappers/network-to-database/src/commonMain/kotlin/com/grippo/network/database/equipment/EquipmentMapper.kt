package com.grippo.network.database.equipment

import com.grippo.database.entity.EquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.equipment.EquipmentResponse

public fun List<EquipmentResponse>.toEntities(): List<EquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentResponse.toEntityOrNull(): EquipmentEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "EquipmentResponse.id is null"
    } ?: return null

    val entityEquipmentGroupId = AppLogger.Mapping.log(equipmentGroupId) {
        "EquipmentResponse.equipmentGroupId is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(name) {
        "EquipmentResponse.name is null"
    } ?: return null

    val entityType = AppLogger.Mapping.log(type) {
        "EquipmentResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "EquipmentResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
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