package com.grippo.dto.entity.equipment

import com.grippo.services.backend.dto.equipment.EquipmentGroupResponse
import com.grippo.services.database.entity.EquipmentGroupEntity
import com.grippo.toolkit.logger.AppLogger

public fun List<EquipmentGroupResponse>.toEntities(): List<EquipmentGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentGroupResponse.toEntityOrNull(): EquipmentGroupEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "EquipmentGroupResponse.id is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(name) {
        "EquipmentGroupResponse.name is null"
    } ?: return null

    val entityType = AppLogger.Mapping.log(type) {
        "EquipmentGroupResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "EquipmentGroupResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
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