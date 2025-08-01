package com.grippo.domain.state.user

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.state.profile.ExperienceEnumState

public fun ExperienceEnum.toState(): ExperienceEnumState {
    return when (this) {
        ExperienceEnum.BEGINNER -> ExperienceEnumState.BEGINNER
        ExperienceEnum.INTERMEDIATE -> ExperienceEnumState.INTERMEDIATE
        ExperienceEnum.ADVANCED -> ExperienceEnumState.ADVANCED
        ExperienceEnum.PRO -> ExperienceEnumState.PRO
    }
}