package com.grippo.domain.state.exercise.example

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.state.exercise.examples.CategoryEnumState

public fun CategoryEnum.toState(): CategoryEnumState {
    return when (this) {
        CategoryEnum.COMPOUND -> CategoryEnumState.COMPOUND
        CategoryEnum.ISOLATION -> CategoryEnumState.ISOLATION
    }
}