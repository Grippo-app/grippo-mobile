package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum

public fun WeightTypeEnum.toState(): WeightTypeEnumState {
    return when (this) {
        WeightTypeEnum.FREE -> WeightTypeEnumState.FREE
        WeightTypeEnum.FIXED -> WeightTypeEnumState.FIXED
        WeightTypeEnum.BODY_WEIGHT -> WeightTypeEnumState.BODY_WEIGHT
    }
}