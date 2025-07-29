package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.state.exercise.examples.WeightTypeEnumState

public fun WeightTypeEnum.toState(): WeightTypeEnumState {
    return when (this) {
        WeightTypeEnum.FREE -> WeightTypeEnumState.FREE
        WeightTypeEnum.FIXED -> WeightTypeEnumState.FIXED
        WeightTypeEnum.BODY_WEIGHT -> WeightTypeEnumState.BODY_WEIGHT
    }
}