package com.grippo.services.backend.dto.training

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TrainingBody(
    @SerialName("duration")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val duration: Long? = null,
    @SerialName("exercises")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val exercises: List<ExerciseBody>? = null,
    @SerialName("intensity")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val intensity: Float? = null,
    @SerialName("volume")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val volume: Float? = null,
    @SerialName("repetitions")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val repetitions: Int? = null,
)

@Serializable
public data class ExerciseBody(
    @SerialName("repetitions")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val repetitions: Int? = null,
    @SerialName("intensity")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val intensity: Float? = null,
    @SerialName("iterations")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val iterations: List<IterationBody>? = null,
    @SerialName("name")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String? = null,
    @SerialName("volume")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val volume: Float? = null,
    @SerialName("exerciseExampleId")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val exerciseExampleId: String? = null,
)

@Serializable
public data class IterationBody(
    @SerialName("repetitions")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val repetitions: Int? = null,
    @SerialName("extraWeight")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val extraWeight: Float? = null,
    @SerialName("bodyWeight")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val bodyWeight: Float? = null,
    @SerialName("bodyMultiplier")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val bodyMultiplier: Float? = null,
    @SerialName("assistWeight")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val assistWeight: Float? = null,
    @SerialName("externalWeight")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val externalWeight: Float? = null,
)