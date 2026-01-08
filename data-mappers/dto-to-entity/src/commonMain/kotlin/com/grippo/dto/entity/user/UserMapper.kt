package com.grippo.dto.entity.user

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.user.UserResponse>.toEntities(): List<com.grippo.services.database.entity.UserEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.user.UserResponse.toEntityOrNull(): com.grippo.services.database.entity.UserEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "UserResponse.id is null"
    } ?: return null

    val profile = AppLogger.Mapping.log(profile) {
        "UserResponse.profile is null"
    } ?: return null

    val entityProfileId = AppLogger.Mapping.log(profile.id) {
        "UserProfileResponse.id is null"
    } ?: return null

    val entityWeight = AppLogger.Mapping.log(profile.weight) {
        "UserProfileResponse.weight is null"
    } ?: return null

    val entityHeight = AppLogger.Mapping.log(profile.height) {
        "UserProfileResponse.height is null"
    } ?: return null

    val entityEmail = AppLogger.Mapping.log(email) {
        "UserResponse.email is null"
    } ?: return null

    val entityExperience = AppLogger.Mapping.log(profile.experience) {
        "UserProfileResponse.experience is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(profile.name) {
        "UserProfileResponse.name is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "UserResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "UserResponse.updatedAt is null"
    } ?: return null

    return _root_ide_package_.com.grippo.services.database.entity.UserEntity(
        id = entityId,
        profileId = entityProfileId,
        weight = entityWeight,
        height = entityHeight,
        email = entityEmail,
        experience = entityExperience,
        name = entityName,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}
