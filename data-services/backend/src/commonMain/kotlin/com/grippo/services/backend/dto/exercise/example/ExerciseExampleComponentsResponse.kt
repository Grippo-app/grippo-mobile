package com.grippo.services.backend.dto.exercise.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ExerciseExampleComponentsResponse(
    @SerialName("assistWeight")
    val assistWeight: AssistWeightResponse? = null,
    @SerialName("bodyWeight")
    val bodyWeight: BodyWeightResponse? = null,
    @SerialName("externalWeight")
    val externalWeight: ExternalWeightResponse? = null,
    @SerialName("extraWeight")
    val extraWeight: ExtraWeightResponse? = null
)

@Serializable
public data class ExternalWeightResponse(
    @SerialName("required")
    val required: Boolean? = null
)

@Serializable
public data class ExtraWeightResponse(
    @SerialName("required")
    val required: Boolean? = null
)

@Serializable
public data class AssistWeightResponse(
    @SerialName("required")
    val required: Boolean? = null
)

@Serializable
public data class BodyWeightResponse(
    @SerialName("required")
    val required: Boolean? = null,
    @SerialName("multiplier")
    val multiplier: Float? = null,
)