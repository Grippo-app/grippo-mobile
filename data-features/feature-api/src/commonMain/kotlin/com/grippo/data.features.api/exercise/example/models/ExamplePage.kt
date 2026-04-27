package com.grippo.data.features.api.exercise.example.models

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
