package com.grippo.network.mapper

import com.grippo.database.entity.EquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.EquipmentDto

public fun List<EquipmentDto>.toEntities(): List<EquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentDto.toEntityOrNull(): EquipmentEntity? {
    val entityId = AppLogger.mapping(id, { "EquipmentDto.id is null" }) ?: return null
    val entityEquipmentGroupId = AppLogger.mapping(equipmentGroupId, { "EquipmentDto.equipmentGroupId is null" }) ?: return null
    val entityName = AppLogger.mapping(name, { "EquipmentDto.name is null" }) ?: return null
    val entityType = AppLogger.mapping(type, { "EquipmentDto.type is null" }) ?: return null
    val entityCreatedAt = AppLogger.mapping(createdAt, { "EquipmentDto.createdAt is null" }) ?: return null
    val entityUpdatedAt = AppLogger.mapping(updatedAt, { "EquipmentDto.updatedAt is null" }) ?: return null

    return EquipmentEntity(
        id = entityId,
        equipmentGroupId = entityEquipmentGroupId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}