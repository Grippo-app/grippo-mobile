package com.grippo.dto.entity.equipment

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.equipment.EquipmentResponse>.toEntities(): List<com.grippo.services.database.entity.EquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.equipment.EquipmentResponse.toEntityOrNull(): com.grippo.services.database.entity.EquipmentEntity? {
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

    return _root_ide_package_.com.grippo.services.database.entity.EquipmentEntity(
        id = entityId,
        equipmentGroupId = entityEquipmentGroupId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}