package com.grippo.domain.mapper

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.User
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.user.models.ExperienceEnumState
import com.grippo.presentation.api.user.models.UserState

public fun User.toState(): UserState? {
    val mappedExperience = AppLogger.checkOrLog(experience.toState()) {
        "User $id has unrecognized experience: $experience"
    } ?: return null

    return UserState(
        id = id,
        name = name,
        weight = weight,
        height = height,
        createdAt = createAt,
        experience = mappedExperience,
        records = 0, // todo add from BE
        workouts = 0 // todo add from BE
    )
}

public fun ExperienceEnum.toState(): ExperienceEnumState? {
    return when (this) {
        ExperienceEnum.BEGINNER -> ExperienceEnumState.BEGINNER
        ExperienceEnum.INTERMEDIATE -> ExperienceEnumState.INTERMEDIATE
        ExperienceEnum.ADVANCED -> ExperienceEnumState.ADVANCED
        ExperienceEnum.PRO -> ExperienceEnumState.PRO
        ExperienceEnum.UNIDENTIFIED -> AppLogger.checkOrLog(null) {
            "ExperienceEnum.UNIDENTIFIED cannot be mapped to state"
        }
    }
}