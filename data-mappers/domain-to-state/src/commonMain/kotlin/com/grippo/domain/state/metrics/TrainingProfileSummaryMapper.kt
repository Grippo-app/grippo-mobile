package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.TrainingDimensionKindState as StateTrainingDimensionKind
import com.grippo.core.state.metrics.TrainingDimensionScoreState as StateTrainingDimensionScore
import com.grippo.core.state.metrics.TrainingLoadProfileState as StateTrainingLoadProfile
import com.grippo.core.state.metrics.TrainingProfileKindState as StateTrainingProfileKind
import com.grippo.data.features.api.metrics.models.TrainingDimensionKind as DomainTrainingDimensionKind
import com.grippo.data.features.api.metrics.models.TrainingDimensionScore as DomainTrainingDimensionScore
import com.grippo.data.features.api.metrics.models.TrainingLoadProfile as DomainTrainingLoadProfile
import com.grippo.data.features.api.metrics.models.TrainingProfileKind as DomainTrainingProfileKind

public fun DomainTrainingLoadProfile?.toState(): StateTrainingLoadProfile? {
    val value = this ?: return null
    return StateTrainingLoadProfile(
        kind = value.kind.toState(),
        dimensions = value.dimensions.map(DomainTrainingDimensionScore::toState),
        dominant = value.dominant?.toState(),
        confidence = value.confidence,
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
