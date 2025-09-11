package com.grippo.state.domain.example

import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.state.exercise.examples.ExampleSortingEnumState

public fun ExampleSortingEnumState.toDomain(): ExampleSortingEnum {
    return when (this) {
        ExampleSortingEnumState.MostlyUsed -> ExampleSortingEnum.MostlyUsed
        ExampleSortingEnumState.RecentlyUsed -> ExampleSortingEnum.RecentlyUsed
        ExampleSortingEnumState.NewAdded -> ExampleSortingEnum.NewAdded
    }
}