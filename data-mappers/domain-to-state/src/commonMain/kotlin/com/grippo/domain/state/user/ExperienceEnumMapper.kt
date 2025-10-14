package com.grippo.domain.state.user

import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum

public fun ExperienceEnum.toState(): ExperienceEnumState {
    return when (this) {
        ExperienceEnum.BEGINNER -> ExperienceEnumState.BEGINNER
        ExperienceEnum.INTERMEDIATE -> ExperienceEnumState.INTERMEDIATE
        ExperienceEnum.ADVANCED -> ExperienceEnumState.ADVANCED
        ExperienceEnum.PRO -> ExperienceEnumState.PRO
    }
}