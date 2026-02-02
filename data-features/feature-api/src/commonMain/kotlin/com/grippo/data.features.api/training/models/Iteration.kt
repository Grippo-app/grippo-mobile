package com.grippo.data.features.api.training.models

public data class Iteration(
    val id: String,
    val externalWeight: Float?,
    val assistWeight: Float?,
    val extraWeight: Float?,
    val bodyWeight: Float?,
    val repetitions: Int
) {
    val volume: Float
        get() = (externalWeight ?: 0f) + (extraWeight ?: 0f) + (bodyWeight ?: 0f) - (assistWeight
            ?: 0f)
}