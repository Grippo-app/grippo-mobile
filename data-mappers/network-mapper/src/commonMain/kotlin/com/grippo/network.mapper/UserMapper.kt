package com.grippo.network.mapper

import com.grippo.database.entity.UserEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.UserResponse

public fun List<UserResponse>.toEntities(): List<UserEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun UserResponse.toEntityOrNull(): UserEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "UserDto.id is null"
    } ?: return null

    val entityWeight = AppLogger.checkOrLog(weight) {
        "UserDto.weight is null"
    } ?: return null

    val entityHeight = AppLogger.checkOrLog(height) {
        "UserDto.height is null"
    } ?: return null

    val entityEmail = AppLogger.checkOrLog(email) {
        "UserDto.email is null"
    } ?: return null

    val entityExperience = AppLogger.checkOrLog(experience) {
        "UserDto.experience is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "UserDto.name is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "UserDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "UserDto.updatedAt is null"
    } ?: return null

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