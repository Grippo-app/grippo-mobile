package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.goal.models.Goal

public data class GoalAdherence(
    val goal: Goal,
    val score: Int,
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,
    val sessionCount: Int,
    val compoundRatio: Int,
    val topExercises: List<TopExerciseContribution>,
    val topMuscles: List<TopMuscleContribution>,
    val topMuscleGroups: List<TopMuscleGroupContribution>,
    val findings: List<GoalFitFinding>,
)
