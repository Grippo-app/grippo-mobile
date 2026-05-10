package com.grippo.data.features.api.exercise.example.models

public sealed interface ExampleScope {

    public data class All(
        public val muscleGroupId: String? = null,
    ) : ExampleScope

    public data class SimilarTo(
        public val targetId: String,
    ) : ExampleScope
}
