package com.grippo.network.mapper

import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.EquipmentGroupResponse

public fun EquipmentGroupResponse.toEquipments(): List<EquipmentEntity> {
    return equipments.orEmpty().mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentGroupResponse.toEntityOrNull(): EquipmentGroupEntity? {
    val entityId = AppLogger.mapping(id, { "EquipmentGroupDto.id is null" }) ?: return null
    val entityName = AppLogger.mapping(name, { "EquipmentGroupDto.name is null" }) ?: return null
    val entityType = AppLogger.mapping(type, { "EquipmentGroupDto.type is null" }) ?: return null
    val entityCreatedAt =
        AppLogger.mapping(createdAt, { "EquipmentGroupDto.createdAt is null" }) ?: return null
    val entityUpdatedAt =
        AppLogger.mapping(updatedAt, { "EquipmentGroupDto.updatedAt is null" }) ?: return null

    return EquipmentGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}