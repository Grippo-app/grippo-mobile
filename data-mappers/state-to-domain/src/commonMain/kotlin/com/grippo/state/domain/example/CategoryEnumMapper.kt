package com.grippo.state.domain.example

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.state.exercise.examples.CategoryEnumState

public fun CategoryEnumState.toDomain(): CategoryEnum {
    return when (this) {
        CategoryEnumState.COMPOUND -> CategoryEnum.COMPOUND
        CategoryEnumState.ISOLATION -> CategoryEnum.ISOLATION
    }
}