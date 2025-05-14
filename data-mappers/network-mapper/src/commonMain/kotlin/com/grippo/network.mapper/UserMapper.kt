package com.grippo.network.mapper

import com.grippo.database.entity.UserEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.UserResponse

public fun List<UserResponse>.toEntities(): List<UserEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun UserResponse.toEntityOrNull(): UserEntity? {
    val entityId = AppLogger.mapping(id, { "UserDto.id is null" }) ?: return null
    val entityWeight = AppLogger.mapping(weight, { "UserDto.weight is null" }) ?: return null
    val entityHeight = AppLogger.mapping(height, { "UserDto.height is null" }) ?: return null
    val entityEmail = AppLogger.mapping(email, { "UserDto.email is null" }) ?: return null
    val entityExperience =
        AppLogger.mapping(experience, { "UserDto.experience is null" }) ?: return null
    val entityName = AppLogger.mapping(name, { "UserDto.name is null" }) ?: return null
    val entityCreatedAt =
        AppLogger.mapping(createdAt, { "UserDto.createdAt is null" }) ?: return null
    val entityUpdatedAt =
        AppLogger.mapping(updatedAt, { "UserDto.updatedAt is null" }) ?: return null

    return UserEntity(
        id = entityId,
        weight = entityWeight,
        height = entityHeight,
        email = entityEmail,
        experience = entityExperience,
        name = entityName,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}