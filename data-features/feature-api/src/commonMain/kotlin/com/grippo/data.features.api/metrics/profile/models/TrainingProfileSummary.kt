package com.grippo.data.features.api.metrics.profile.models

public data class TrainingLoadProfile(
    val kind: TrainingProfileKind,
    val dimensions: List<TrainingDimensionScore>,
    val dominant: TrainingDimensionKind?,
    val confidence: Int,
    val artifacts: TrainingLoadProfileArtifacts = TrainingLoadProfileArtifacts.empty(),
)
