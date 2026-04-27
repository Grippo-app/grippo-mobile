package com.grippo.domain.state.metrics.profile

import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.metrics.profile.TrainingDimensionKindState as StateTrainingDimensionKind
import com.grippo.core.state.metrics.profile.TrainingDimensionScoreState as StateTrainingDimensionScore
import com.grippo.core.state.metrics.profile.TrainingLoadProfileArtifactsState as StateTrainingLoadProfileArtifacts
import com.grippo.core.state.metrics.profile.TrainingLoadProfileState as StateTrainingLoadProfile
import com.grippo.core.state.metrics.profile.TrainingProfileKindState as StateTrainingProfileKind
import com.grippo.data.features.api.metrics.profile.models.TrainingDimensionKind as DomainTrainingDimensionKind
import com.grippo.data.features.api.metrics.profile.models.TrainingDimensionScore as DomainTrainingDimensionScore
import com.grippo.data.features.api.metrics.profile.models.TrainingLoadProfile as DomainTrainingLoadProfile
import com.grippo.data.features.api.metrics.profile.models.TrainingLoadProfileArtifacts as DomainTrainingLoadProfileArtifacts
import com.grippo.data.features.api.metrics.profile.models.TrainingProfileKind as DomainTrainingProfileKind

public fun DomainTrainingLoadProfile?.toState(): StateTrainingLoadProfile? {
    val value = this ?: return null
    return StateTrainingLoadProfile(
        kind = value.kind.toState(),
        dimensions = value.dimensions
            .map(DomainTrainingDimensionScore::toState)
            .toPersistentList(),
        dominant = value.dominant?.toState(),
        confidence = value.confidence,
        artifacts = value.artifacts.toState(),
    )
}

private fun DomainTrainingLoadProfileArtifacts.toState(): StateTrainingLoadProfileArtifacts {
    return StateTrainingLoadProfileArtifacts(
        topExercises = topExercises.map { it.toState() }.toPersistentList(),
        topMuscles = topMuscles.map { it.toState() }.toPersistentList(),
        totalExercisesCount = totalExercisesCount,
        totalMusclesCount = totalMusclesCount,
        compoundRatio = compoundRatio,
        pushRatio = pushRatio,
        pullRatio = pullRatio,
        hingeRatio = hingeRatio,
    )
}

private fun DomainTrainingDimensionScore.toState(): StateTrainingDimensionScore {
    return StateTrainingDimensionScore(
        kind = kind.toState(),
        score = score,
    )
}

private fun DomainTrainingDimensionKind.toState(): StateTrainingDimensionKind {
    return when (this) {
        DomainTrainingDimensionKind.Strength -> StateTrainingDimensionKind.Strength
        DomainTrainingDimensionKind.Hypertrophy -> StateTrainingDimensionKind.Hypertrophy
        DomainTrainingDimensionKind.Endurance -> StateTrainingDimensionKind.Endurance
    }
}

private fun DomainTrainingProfileKind.toState(): StateTrainingProfileKind {
    return when (this) {
        DomainTrainingProfileKind.Strength -> StateTrainingProfileKind.Strength
        DomainTrainingProfileKind.Hypertrophy -> StateTrainingProfileKind.Hypertrophy
        DomainTrainingProfileKind.Endurance -> StateTrainingProfileKind.Endurance
        DomainTrainingProfileKind.Powerbuilding -> StateTrainingProfileKind.Powerbuilding
        DomainTrainingProfileKind.Mixed -> StateTrainingProfileKind.Mixed
        DomainTrainingProfileKind.Easy -> StateTrainingProfileKind.Easy
    }
}
