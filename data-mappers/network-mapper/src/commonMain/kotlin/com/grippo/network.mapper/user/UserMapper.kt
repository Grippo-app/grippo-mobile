package com.grippo.network.mapper.user

import com.grippo.database.entity.UserEntity
import com.grippo.logger.AppLogger
import com.grippo.network.user.UserResponse

public fun List<UserResponse>.toEntities(): List<UserEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun UserResponse.toEntityOrNull(): UserEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "UserResponse.id is null"
    } ?: return null

    val entityWeight = AppLogger.Mapping.log(weight) {
        "UserResponse.weight is null"
    } ?: return null

    val entityHeight = AppLogger.Mapping.log(height) {
        "UserResponse.height is null"
    } ?: return null

    val entityEmail = AppLogger.Mapping.log(email) {
        "UserResponse.email is null"
    } ?: return null

    val entityExperience = AppLogger.Mapping.log(experience) {
        "UserResponse.experience is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(name) {
        "UserResponse.name is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "UserResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
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