package com.grippo.entity.domain.user

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.User
import com.grippo.database.entity.UserEntity
import com.grippo.date.utils.DateTimeUtils
import com.grippo.logger.AppLogger

public fun UserEntity.toDomain(): User? {
    val mappedExperience = AppLogger.Mapping.log(ExperienceEnum.of(experience)) {
        "UserEntity $id has unrecognized experience: $experience"
    } ?: return null

    return User(
        id = id,
        name = name,
        email = email,
        weight = weight,
        experience = mappedExperience,
        height = height,
        createAt = DateTimeUtils.toLocalDateTime(createdAt)
    )
}