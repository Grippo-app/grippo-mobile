package com.grippo.state.domain.example

import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum

public fun ForceTypeEnumState.toDomain(): ForceTypeEnum {
    return when (this) {
        ForceTypeEnumState.PULL -> ForceTypeEnum.PULL
        ForceTypeEnumState.PUSH -> ForceTypeEnum.PUSH
        ForceTypeEnumState.HINGE -> ForceTypeEnum.HINGE
    }
}