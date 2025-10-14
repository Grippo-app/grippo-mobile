package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum

public fun ForceTypeEnum.toState(): ForceTypeEnumState {
    return when (this) {
        ForceTypeEnum.PULL -> ForceTypeEnumState.PULL
        ForceTypeEnum.PUSH -> ForceTypeEnumState.PUSH
        ForceTypeEnum.HINGE -> ForceTypeEnumState.HINGE
    }
}