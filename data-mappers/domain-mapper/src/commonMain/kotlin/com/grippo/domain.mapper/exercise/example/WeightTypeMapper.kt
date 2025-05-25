package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.WeightTypeEnumState

public fun WeightTypeEnum.toState(): WeightTypeEnumState? {
    return when (this) {
        WeightTypeEnum.FREE -> WeightTypeEnumState.FREE
        WeightTypeEnum.FIXED -> WeightTypeEnumState.FIXED
        WeightTypeEnum.BODY_WEIGHT -> WeightTypeEnumState.BODY_WEIGHT
        WeightTypeEnum.UNIDENTIFIED -> AppLogger.checkOrLog(null) {
            "WeightTypeEnum.UNIDENTIFIED cannot be mapped to state"
        }
    }
}