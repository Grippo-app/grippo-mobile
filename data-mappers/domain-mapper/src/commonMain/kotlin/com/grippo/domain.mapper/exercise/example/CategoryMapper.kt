package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.CategoryEnumState

public fun CategoryEnum.toState(): CategoryEnumState? {
    return when (this) {
        CategoryEnum.COMPOUND -> CategoryEnumState.COMPOUND
        CategoryEnum.ISOLATION -> CategoryEnumState.ISOLATION
        CategoryEnum.UNIDENTIFIED -> AppLogger.checkOrLog(null) {
            "CategoryEnum.UNIDENTIFIED cannot be mapped to state"
        }
    }
}