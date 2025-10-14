package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.data.features.api.exercise.example.models.CategoryEnum

public fun CategoryEnum.toState(): CategoryEnumState {
    return when (this) {
        CategoryEnum.COMPOUND -> CategoryEnumState.COMPOUND
        CategoryEnum.ISOLATION -> CategoryEnumState.ISOLATION
    }
}