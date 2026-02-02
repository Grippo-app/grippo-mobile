package com.grippo.services.backend.dto.training

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TrainingBody(
    @SerialName("duration")
    val duration: Long,
    @SerialName("exercises")
    val exercises: List<ExerciseBody>,
    @SerialName("intensity")
    val intensity: Float,
    @SerialName("volume")
    val volume: Float,
    @SerialName("repetitions")
    val repetitions: Int,
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
    val exerciseExampleId: String?,
)

@Serializable
public data class IterationBody(
    @SerialName("repetitions")
    val repetitions: Int,
    @SerialName("extraWeight")
    val extraWeight: Float?,
    @SerialName("bodyWeight")
    val bodyWeight: Float?,
    @SerialName("assistWeight")
    val assistWeight: Float?,
    @SerialName("externalWeight")
    val externalWeight: Float?,
)