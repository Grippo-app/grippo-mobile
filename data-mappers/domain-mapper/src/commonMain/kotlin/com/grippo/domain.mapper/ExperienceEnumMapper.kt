package com.grippo.domain.mapper

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.presentation.api.user.models.ExperienceEnumState

public fun ExperienceEnumState.toDomain(): ExperienceEnum {
    return when (this) {
        ExperienceEnumState.BEGINNER -> ExperienceEnum.BEGINNER
        ExperienceEnumState.INTERMEDIATE -> ExperienceEnum.INTERMEDIATE
        ExperienceEnumState.ADVANCED -> ExperienceEnum.ADVANCED
        ExperienceEnumState.PRO -> ExperienceEnum.PRO
    }
}