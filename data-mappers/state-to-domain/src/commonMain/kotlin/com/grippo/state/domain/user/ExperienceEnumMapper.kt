package com.grippo.state.domain.user

import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum

public fun ExperienceEnumState.toDomain(): ExperienceEnum {
    return when (this) {
        ExperienceEnumState.BEGINNER -> ExperienceEnum.BEGINNER
        ExperienceEnumState.INTERMEDIATE -> ExperienceEnum.INTERMEDIATE
        ExperienceEnumState.ADVANCED -> ExperienceEnum.ADVANCED
        ExperienceEnumState.PRO -> ExperienceEnum.PRO
    }
}