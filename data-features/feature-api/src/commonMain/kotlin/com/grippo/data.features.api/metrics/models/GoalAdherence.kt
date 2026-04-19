package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.goal.models.Goal

public data class GoalAdherence(
    val goal: Goal,
    val score: Int,
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,
)
