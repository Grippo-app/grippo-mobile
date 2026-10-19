package com.grippo.data.features.api.exercise.example.models

public data class ExamplePage(
    val limits: Int?,
    val number: Int,
) {

    public fun <T> slice(items: List<T>): List<T> {
        val pageSize = limits?.coerceAtLeast(0) ?: return items
        val offset = (number - 1).coerceAtLeast(0) * pageSize
        return items.drop(offset).take(pageSize)
    }

    public companion object Companion {
        public const val CHUNK_LIMITS: Int = 20

        public val Chunk: ExamplePage = ExamplePage(
            number = 1,
            limits = CHUNK_LIMITS
        )

        public val Unlimited: ExamplePage = ExamplePage(
            number = 1,
            limits = null
        )
    }
}
