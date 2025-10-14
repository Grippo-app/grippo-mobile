package com.grippo.state.domain.example

import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum

public fun WeightTypeEnumState.toDomain(): WeightTypeEnum {
    return when (this) {
        WeightTypeEnumState.FREE -> WeightTypeEnum.FREE
        WeightTypeEnumState.FIXED -> WeightTypeEnum.FIXED
        WeightTypeEnumState.BODY_WEIGHT -> WeightTypeEnum.BODY_WEIGHT
    }
}