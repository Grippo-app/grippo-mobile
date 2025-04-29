package com.grippo.network.mapper

import com.grippo.database.entity.UserEntity
import com.grippo.network.dto.UserDto

public fun List<UserDto>.toEntities(): List<UserEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun UserDto.toEntityOrNull(): UserEntity? {
    return UserEntity(
        id = id ?: return null,
        weight = weight?.toFloat() ?: return null,
        height = height?.toFloat() ?: return null,
        email = email ?: return null,
        experience = experience ?: return null,
        name = name ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}