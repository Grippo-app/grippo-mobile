package com.grippo.domain.state.metrics.profile

import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.metrics.profile.TrainingDimensionKindState as StateTrainingDimensionKind
import com.grippo.core.state.metrics.profile.TrainingDimensionScoreState as StateTrainingDimensionScore
import com.grippo.core.state.metrics.profile.TrainingLoadProfileArtifactsState as StateTrainingLoadProfileArtifacts
import com.grippo.core.state.metrics.profile.TrainingLoadProfileState as StateTrainingLoadProfile
import com.grippo.core.state.metrics.profile.TrainingProfileInsightActionState as StateTrainingProfileInsightAction
import com.grippo.core.state.metrics.profile.TrainingProfileInsightReasonState as StateTrainingProfileInsightReason
import com.grippo.core.state.metrics.profile.TrainingProfileInsightSeverityState as StateTrainingProfileInsightSeverity
import com.grippo.core.state.metrics.profile.TrainingProfileInsightState as StateTrainingProfileInsight
import com.grippo.core.state.metrics.profile.TrainingProfileKindState as StateTrainingProfileKind
import com.grippo.data.features.api.metrics.profile.models.TrainingDimensionKind as DomainTrainingDimensionKind
import com.grippo.data.features.api.metrics.profile.models.TrainingDimensionScore as DomainTrainingDimensionScore
import com.grippo.data.features.api.metrics.profile.models.TrainingLoadProfile as DomainTrainingLoadProfile
import com.grippo.data.features.api.metrics.profile.models.TrainingLoadProfileArtifacts as DomainTrainingLoadProfileArtifacts
import com.grippo.data.features.api.metrics.profile.models.TrainingProfileInsight as DomainTrainingProfileInsight
import com.grippo.data.features.api.metrics.profile.models.TrainingProfileInsightAction as DomainTrainingProfileInsightAction
import com.grippo.data.features.api.metrics.profile.models.TrainingProfileInsightReason as DomainTrainingProfileInsightReason
import com.grippo.data.features.api.metrics.profile.models.TrainingProfileInsightSeverity as DomainTrainingProfileInsightSeverity
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
        insights = value.insights.map(DomainTrainingProfileInsight::toState).toPersistentList(),
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
        topExerciseShare = topExerciseShare,
        topTwoMusclesShare = topTwoMusclesShare,
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

private fun DomainTrainingProfileInsight.toState(): StateTrainingProfileInsight {
    return StateTrainingProfileInsight(
        severity = severity.toState(),
        reason = reason.toState(),
        action = action?.toState(),
    )
}

private fun DomainTrainingProfileInsightSeverity.toState(): StateTrainingProfileInsightSeverity {
    return when (this) {
        DomainTrainingProfileInsightSeverity.Positive -> StateTrainingProfileInsightSeverity.Positive
        DomainTrainingProfileInsightSeverity.Warning -> StateTrainingProfileInsightSeverity.Warning
        DomainTrainingProfileInsightSeverity.Negative -> StateTrainingProfileInsightSeverity.Negative
        DomainTrainingProfileInsightSeverity.Neutral -> StateTrainingProfileInsightSeverity.Neutral
    }
}

private fun DomainTrainingProfileInsightReason.toState(): StateTrainingProfileInsightReason {
    return when (this) {
        DomainTrainingProfileInsightReason.EasySession -> StateTrainingProfileInsightReason.EasySession
        DomainTrainingProfileInsightReason.MixedSession -> StateTrainingProfileInsightReason.MixedSession
        DomainTrainingProfileInsightReason.PowerbuildingPattern -> StateTrainingProfileInsightReason.PowerbuildingPattern
        DomainTrainingProfileInsightReason.ClearDominant -> StateTrainingProfileInsightReason.ClearDominant
        DomainTrainingProfileInsightReason.LowConfidence -> StateTrainingProfileInsightReason.LowConfidence
        DomainTrainingProfileInsightReason.CompoundFoundation -> StateTrainingProfileInsightReason.CompoundFoundation
        DomainTrainingProfileInsightReason.IsolationHeavy -> StateTrainingProfileInsightReason.IsolationHeavy
        DomainTrainingProfileInsightReason.HighExerciseConcentration -> StateTrainingProfileInsightReason.HighExerciseConcentration
        DomainTrainingProfileInsightReason.BalancedExerciseSpread -> StateTrainingProfileInsightReason.BalancedExerciseSpread
        DomainTrainingProfileInsightReason.PushPullImbalance -> StateTrainingProfileInsightReason.PushPullImbalance
        DomainTrainingProfileInsightReason.NarrowMuscleFocus -> StateTrainingProfileInsightReason.NarrowMuscleFocus
    }
}

private fun DomainTrainingProfileInsightAction.toState(): StateTrainingProfileInsightAction {
    return when (this) {
        DomainTrainingProfileInsightAction.AddCompoundAnchor -> StateTrainingProfileInsightAction.AddCompoundAnchor
        DomainTrainingProfileInsightAction.DiversifyExercises -> StateTrainingProfileInsightAction.DiversifyExercises
        DomainTrainingProfileInsightAction.AddPullingWork -> StateTrainingProfileInsightAction.AddPullingWork
        DomainTrainingProfileInsightAction.AddPushingWork -> StateTrainingProfileInsightAction.AddPushingWork
        DomainTrainingProfileInsightAction.SpreadMuscleFocus -> StateTrainingProfileInsightAction.SpreadMuscleFocus
        DomainTrainingProfileInsightAction.AddOneHardSet -> StateTrainingProfileInsightAction.AddOneHardSet
        DomainTrainingProfileInsightAction.PickAClearerStyle -> StateTrainingProfileInsightAction.PickAClearerStyle
    }
}
