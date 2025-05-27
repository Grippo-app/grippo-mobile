package com.grippo.network.mapper.user

import com.grippo.database.entity.UserEntity
import com.grippo.logger.AppLogger
import com.grippo.network.user.UserResponse

public fun List<UserResponse>.toEntities(): List<UserEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun UserResponse.toEntityOrNull(): UserEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "UserResponse.id is null"
    } ?: return null

    val entityWeight = AppLogger.checkOrLog(weight) {
        "UserResponse.weight is null"
    } ?: return null

    val entityHeight = AppLogger.checkOrLog(height) {
        "UserResponse.height is null"
    } ?: return null

    val entityEmail = AppLogger.checkOrLog(email) {
        "UserResponse.email is null"
    } ?: return null

    val entityExperience = AppLogger.checkOrLog(experience) {
        "UserResponse.experience is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "UserResponse.name is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "UserResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "UserResponse.updatedAt is null"
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