package com.grippo.state.domain.example

import com.grippo.data.features.api.exercise.example.models.ExampleSorting
import com.grippo.state.exercise.examples.ExampleSortingEnumState

public fun ExampleSortingEnumState.toDomain(): ExampleSorting {
    return when (this) {
        ExampleSortingEnumState.MostlyUsed -> ExampleSorting.MostlyUsed
        ExampleSortingEnumState.RecentlyUsed -> ExampleSorting.RecentlyUsed
        ExampleSortingEnumState.NewAdded -> ExampleSorting.NewAdded
    }
}