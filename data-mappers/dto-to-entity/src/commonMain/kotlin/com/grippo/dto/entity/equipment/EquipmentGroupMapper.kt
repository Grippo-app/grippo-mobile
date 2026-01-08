package com.grippo.dto.entity.equipment

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.equipment.EquipmentGroupResponse>.toEntities(): List<com.grippo.services.database.entity.EquipmentGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.equipment.EquipmentGroupResponse.toEntityOrNull(): com.grippo.services.database.entity.EquipmentGroupEntity? {
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

    return _root_ide_package_.com.grippo.services.database.entity.EquipmentGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}