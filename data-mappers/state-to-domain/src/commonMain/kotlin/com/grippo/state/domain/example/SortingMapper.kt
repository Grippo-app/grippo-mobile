package com.grippo.state.domain.example

import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.state.sorting.SortingEnumState

public fun SortingEnumState.toDomain(): ExampleSortingEnum {
    return when (this) {
        SortingEnumState.MostlyUsed -> ExampleSortingEnum.MostlyUsed
        SortingEnumState.RecentlyUsed -> ExampleSortingEnum.RecentlyUsed
        SortingEnumState.NewAdded -> ExampleSortingEnum.NewAdded
    }
}