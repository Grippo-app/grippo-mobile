package com.grippo.backend.dto.achievements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AchievementResponse(
    @SerialName("bestTonnage")
    val bestTonnage: BestTonnage? = null,
    @SerialName("bestWeight")
    val bestWeight: BestWeight? = null,
    @SerialName("lifetimeVolume")
    val lifetimeVolume: LifetimeVolume? = null,
    @SerialName("maxRepetitions")
    val maxRepetitions: MaxRepetitions? = null,
    @SerialName("peakIntensity")
    val peakIntensity: PeakIntensity? = null
)

@Serializable
public data class BestTonnage(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("exerciseId")
    val exerciseId: String? = null,
    @SerialName("tonnage")
    val tonnage: Int? = null
)

@Serializable
public data class BestWeight(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("exerciseId")
    val exerciseId: String? = null,
    @SerialName("iterationId")
    val iterationId: String? = null,
    @SerialName("weight")
    val weight: Int? = null
)

@Serializable
public data class LifetimeVolume(
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("firstPerformedAt")
    val firstPerformedAt: String? = null,
    @SerialName("lastPerformedAt")
    val lastPerformedAt: String? = null,
    @SerialName("sessionsCount")
    val sessionsCount: Int? = null,
    @SerialName("totalVolume")
    val totalVolume: Int? = null
)

@Serializable
public data class MaxRepetitions(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("exerciseId")
    val exerciseId: String? = null,
    @SerialName("iterationId")
    val iterationId: String? = null,
    @SerialName("totalVolume")
    val totalVolume: Int? = null,
    @SerialName("repetitions")
    val repetitions: Int? = null
)

@Serializable
public data class PeakIntensity(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("exerciseId")
    val exerciseId: String? = null,
    @SerialName("intensity")
    val intensity: Double? = null
)

