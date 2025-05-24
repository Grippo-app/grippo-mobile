package com.grippo.domain.mapper.user

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.user.models.ExperienceEnumState

public fun ExperienceEnumState.toDomain(): ExperienceEnum {
    return when (this) {
        ExperienceEnumState.BEGINNER -> ExperienceEnum.BEGINNER
        ExperienceEnumState.INTERMEDIATE -> ExperienceEnum.INTERMEDIATE
        ExperienceEnumState.ADVANCED -> ExperienceEnum.ADVANCED
        ExperienceEnumState.PRO -> ExperienceEnum.PRO
    }
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