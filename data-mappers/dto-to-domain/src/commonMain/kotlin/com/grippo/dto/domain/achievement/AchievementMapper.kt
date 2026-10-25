package com.grippo.dto.domain.achievement

import com.grippo.data.features.api.achievements.Achievement
import com.grippo.services.backend.dto.achievements.AchievementResponse
import com.grippo.services.backend.dto.achievements.BestTonnage
import com.grippo.services.backend.dto.achievements.BestWeight
import com.grippo.services.backend.dto.achievements.LifetimeVolume
import com.grippo.services.backend.dto.achievements.MaxRepetitions
import com.grippo.services.backend.dto.achievements.PeakIntensity
import com.grippo.toolkit.logger.AppLogger

public fun AchievementResponse.toDomain(): List<Achievement> {
    return buildList {
        bestTonnage?.toDomainOrNull()?.let(::add)
        bestWeight?.toDomainOrNull()?.let(::add)
        lifetimeVolume?.toDomainOrNull()?.let(::add)
        maxRepetitions?.toDomainOrNull()?.let(::add)
        peakIntensity?.toDomainOrNull()?.let(::add)
    }
}

private fun BestTonnage.toDomainOrNull(): Achievement.BestTonnage? {
    val exampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "BestTonnage.exerciseExampleId is null"
    } ?: return null

    val exerciseId = AppLogger.Mapping.log(exerciseId) {
        "BestTonnage.exerciseId is null for example $exerciseExampleId"
    } ?: return null

    val measuredTonnage = this.tonnage?.toFloat()?.takeIf { it.isFinite() && it >= 0f }
    val tonnage = AppLogger.Mapping.log(measuredTonnage) {
        "BestTonnage.tonnage is null, non-finite or negative for example $exerciseExampleId"
    } ?: return null

    return Achievement.BestTonnage(
        exerciseExampleId = exampleId,
        exerciseId = exerciseId,
        tonnage = tonnage
    )
}

private fun BestWeight.toDomainOrNull(): Achievement.BestWeight? {
    val exampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "BestWeight.exerciseExampleId is null"
    } ?: return null

    val exerciseId = AppLogger.Mapping.log(exerciseId) {
        "BestWeight.exerciseId is null for example $exerciseExampleId"
    } ?: return null

    val iterationId = AppLogger.Mapping.log(this.iterationId) {
        "BestWeight.iterationId is null for example $exerciseExampleId"
    } ?: return null

    val measuredWeight = this.weight?.toFloat()?.takeIf { it.isFinite() && it >= 0f }
    val weight = AppLogger.Mapping.log(measuredWeight) {
        "BestWeight.weight is null, non-finite or negative for example $exerciseExampleId"
    } ?: return null

    return Achievement.BestWeight(
        exerciseExampleId = exampleId,
        exerciseId = exerciseId,
        iterationId = iterationId,
        weight = weight
    )
}

private fun LifetimeVolume.toDomainOrNull(): Achievement.LifetimeVolume? {
    val exampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "LifetimeVolume.exerciseExampleId is null"
    } ?: return null

    val measuredVolume = this.totalVolume?.toFloat()?.takeIf { it.isFinite() && it >= 0f }
    val totalVolume = AppLogger.Mapping.log(measuredVolume) {
        "LifetimeVolume.totalVolume is null, non-finite or negative for example $exerciseExampleId"
    } ?: return null

    val sessionsCount = AppLogger.Mapping.log(this.sessionsCount) {
        "LifetimeVolume.sessionsCount is null for example $exerciseExampleId"
    } ?: return null

    return Achievement.LifetimeVolume(
        exerciseExampleId = exampleId,
        totalVolume = totalVolume,
        sessionsCount = sessionsCount
    )
}

private fun MaxRepetitions.toDomainOrNull(): Achievement.MaxRepetitions? {
    val exampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "MaxRepetitions.exerciseExampleId is null"
    } ?: return null

    val exerciseId = AppLogger.Mapping.log(this.exerciseId) {
        "MaxRepetitions.exerciseId is null for example $exerciseExampleId"
    } ?: return null

    val iterationId = AppLogger.Mapping.log(this.iterationId) {
        "MaxRepetitions.iterationId is null for example $exerciseExampleId"
    } ?: return null

    val countedRepetitions = this.repetitions?.takeIf { it >= 0 }
    val repetitions = AppLogger.Mapping.log(countedRepetitions) {
        "MaxRepetitions.repetitions is null or negative for example $exerciseExampleId"
    } ?: return null

    return Achievement.MaxRepetitions(
        exerciseExampleId = exampleId,
        exerciseId = exerciseId,
        iterationId = iterationId,
        repetitions = repetitions
    )
}

private fun PeakIntensity.toDomainOrNull(): Achievement.PeakIntensity? {
    val exampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "PeakIntensity.exerciseExampleId is null"
    } ?: return null

    val exerciseId = AppLogger.Mapping.log(this.exerciseId) {
        "PeakIntensity.exerciseId is null for example $exerciseExampleId"
    } ?: return null

    val measuredIntensity = this.intensity?.takeIf { it.isFinite() && it >= 0.0 }
    val intensity = AppLogger.Mapping.log(measuredIntensity) {
        "PeakIntensity.intensity is null, non-finite or negative for example $exerciseExampleId"
    } ?: return null

    return Achievement.PeakIntensity(
        exerciseExampleId = exampleId,
        exerciseId = exerciseId,
        intensity = intensity
    )
}
