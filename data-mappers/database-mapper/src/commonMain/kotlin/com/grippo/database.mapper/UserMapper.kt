package com.grippo.database.mapper

import com.grippo.data.features.api.exercise.example.models.ExperienceEnumEnum
import com.grippo.data.features.api.user.models.User
import com.grippo.database.entity.UserEntity

public fun UserEntity.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        weight = weight,
        experience = ExperienceEnumEnum.of(experience),
        height = height
    )
}