package com.grippo.data.features.api.exercise.example.models

public data class ExampleParams(
    val queries: ExampleQueries,
    val page: ExamplePage,
)

public data class ExamplePage(
    val limits: Int,
    val number: Int,
) {
    public companion object Companion {
        public val Chunk: ExamplePage = ExamplePage(
            number = 1,
            limits = 20
        )
    }
}

public data class ExampleQueries(
    val name: String?,
    val forceType: ForceTypeEnum?,
    val weightType: WeightTypeEnum?,
    val category: CategoryEnum?,
    val muscleGroupId: String?,
)