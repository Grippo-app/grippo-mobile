package com.grippo.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TrainingResponse(
    @SerialName("repetitions")
    val repetitions: Int? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("duration")
    val duration: Long? = null,
    @SerialName("exercises")
    val exercises: List<ExerciseResponse> = emptyList(),
    @SerialName("id")
    val id: String? = null,
    @SerialName("intensity")
    val intensity: Float? = null,
    @SerialName("volume")
    val volume: Float? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("userId")
    val userId: String? = null
)

@Serializable
public data class ExerciseResponse(
    @SerialName("repetitions")
    val repetitions: Int? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("intensity")
    val intensity: Float? = null,
    @SerialName("iterations")
    val iterations: List<IterationResponse> = emptyList(),
    @SerialName("name")
    val name: String? = null,
    @SerialName("volume")
    val volume: Float? = null,
    @SerialName("trainingId")
    val trainingId: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("exerciseExample")
    val exerciseExample: ExerciseExampleDto? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)

@Serializable
public data class IterationResponse(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("exerciseId")
    val exerciseId: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("repetitions")
    val repetitions: Int? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("weight")
    val weight: Float? = null
)