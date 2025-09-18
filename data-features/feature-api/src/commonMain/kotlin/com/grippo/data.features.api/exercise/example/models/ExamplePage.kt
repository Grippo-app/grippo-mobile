package com.grippo.data.features.api.exercise.example.models

public data class ExamplePage(
    val limits: Int,
    val number: Int,
) {
    public companion object {
        public val First15: ExamplePage = ExamplePage(
            number = 1,
            limits = 20
        )
    }
}