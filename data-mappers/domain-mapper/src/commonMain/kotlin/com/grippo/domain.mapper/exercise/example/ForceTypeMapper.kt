package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.ForceTypeEnumState

public fun ForceTypeEnum.toState(): ForceTypeEnumState? {
    return when (this) {
        ForceTypeEnum.PULL -> ForceTypeEnumState.PULL
        ForceTypeEnum.PUSH -> ForceTypeEnumState.PUSH
        ForceTypeEnum.HINGE -> ForceTypeEnumState.HINGE
        ForceTypeEnum.UNIDENTIFIED -> AppLogger.checkOrLog(null) {
            "ForceTypeEnum.UNIDENTIFIED cannot be mapped to state"
        }
    }
}