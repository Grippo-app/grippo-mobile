package com.grippo.network.dto.training

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TrainingBody(
    @SerialName("repetitions")
    val repetitions: Int,
    @SerialName("duration")
    val duration: Long,
    @SerialName("exercises")
    val exercises: List<ExerciseBody>,
    @SerialName("intensity")
    val intensity: Float,
    @SerialName("volume")
    val volume: Float,
)

@Serializable
public data class ExerciseBody(
    @SerialName("repetitions")
    val repetitions: Int,
    @SerialName("intensity")
    val intensity: Float,
    @SerialName("iterations")
    val iterations: List<IterationBody>,
    @SerialName("name")
    val name: String,
    @SerialName("volume")
    val volume: Float,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
)

@Serializable
public data class IterationBody(
    @SerialName("repetitions")
    val repetitions: Int,
    @SerialName("weight")
    val weight: Float
)