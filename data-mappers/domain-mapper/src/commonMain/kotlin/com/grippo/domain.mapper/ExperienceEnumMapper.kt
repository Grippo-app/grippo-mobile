package com.grippo.domain.mapper

import com.grippo.data.features.api.exercise.example.models.ExperienceEnumEnum
import com.grippo.presentation.api.user.models.ExperienceEnumState

public fun ExperienceEnumState.toDomain(): ExperienceEnumEnum {
    return when (this) {
        ExperienceEnumState.BEGINNER -> ExperienceEnumEnum.BEGINNER
        ExperienceEnumState.INTERMEDIATE -> ExperienceEnumEnum.INTERMEDIATE
        ExperienceEnumState.ADVANCED -> ExperienceEnumEnum.ADVANCED
        ExperienceEnumState.PRO -> ExperienceEnumEnum.PRO
    }
}