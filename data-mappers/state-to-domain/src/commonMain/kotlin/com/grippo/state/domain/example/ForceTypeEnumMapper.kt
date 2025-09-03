package com.grippo.state.domain.example

import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.state.exercise.examples.ForceTypeEnumState

public fun ForceTypeEnumState.toDomain(): ForceTypeEnum {
    return when (this) {
        ForceTypeEnumState.PULL -> ForceTypeEnum.PULL
        ForceTypeEnumState.PUSH -> ForceTypeEnum.PUSH
        ForceTypeEnumState.HINGE -> ForceTypeEnum.HINGE
    }
}