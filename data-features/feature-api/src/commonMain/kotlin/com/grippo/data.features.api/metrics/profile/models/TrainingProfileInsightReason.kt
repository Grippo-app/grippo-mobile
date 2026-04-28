package com.grippo.data.features.api.metrics.profile.models

public enum class TrainingProfileInsightReason {

    /** kind = Easy: the session lacked stimulus to classify. */
    EasySession,

    /** kind = Mixed: no single axis dominated and no powerbuilding pattern. */
    MixedSession,

    /** kind = Powerbuilding: balanced strength + hypertrophy with low endurance. */
    PowerbuildingPattern,

    /** Confidence ≥ 70 and a single dominant dimension. */
    ClearDominant,

    /** Dominant dimension exists but confidence < 30 — pattern is fuzzy. */
    LowConfidence,

    /** ≥ 50% of the stimulus came from compound lifts. */
    CompoundFoundation,

    /** < 25% of the stimulus came from compounds — mostly accessories. */
    IsolationHeavy,

    /** Top exercise carried ≥ 40% of the period's stimulus alone. */
    HighExerciseConcentration,

    /** ≥ 5 exercises logged and top exercise ≤ 30% — well distributed. */
    BalancedExerciseSpread,

    /** Push and Pull shares are far apart (gap > 60% of the larger). */
    PushPullImbalance,

    /** Top-2 muscles together represent ≥ 70% of all muscle work. */
    NarrowMuscleFocus,
}
