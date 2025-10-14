package com.grippo.state.domain.example

import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.data.features.api.exercise.example.models.CategoryEnum

public fun CategoryEnumState.toDomain(): CategoryEnum {
    return when (this) {
        CategoryEnumState.COMPOUND -> CategoryEnum.COMPOUND
        CategoryEnumState.ISOLATION -> CategoryEnum.ISOLATION
    }
}