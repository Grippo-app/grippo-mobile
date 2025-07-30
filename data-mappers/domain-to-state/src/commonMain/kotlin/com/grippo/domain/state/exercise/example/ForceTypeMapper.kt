package com.grippo.domain.state.exercise.example

import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.state.exercise.examples.ForceTypeEnumState

public fun ForceTypeEnum.toState(): ForceTypeEnumState {
    return when (this) {
        ForceTypeEnum.PULL -> ForceTypeEnumState.PULL
        ForceTypeEnum.PUSH -> ForceTypeEnumState.PUSH
        ForceTypeEnum.HINGE -> ForceTypeEnumState.HINGE
    }
}