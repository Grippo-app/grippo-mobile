package com.grippo.database.mapper

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.User
import com.grippo.database.entity.UserEntity
import com.grippo.date.utils.DateTimeUtils

public fun UserEntity.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        weight = weight,
        experience = ExperienceEnum.of(experience),
        height = height,
        createAt = DateTimeUtils.toLocalDateTime(createdAt)
    )
}