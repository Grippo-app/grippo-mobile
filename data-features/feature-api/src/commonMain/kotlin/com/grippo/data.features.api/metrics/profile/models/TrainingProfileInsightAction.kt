package com.grippo.data.features.api.metrics.profile.models

public enum class TrainingProfileInsightAction {

    /** Anchor the session with a compound lift. */
    AddCompoundAnchor,

    /** Add another exercise instead of stacking sets on one. */
    DiversifyExercises,

    /** Add pulling work to balance push-heavy sessions. */
    AddPullingWork,

    /** Add pushing work to balance pull-heavy sessions. */
    AddPushingWork,

    /** Spread sets across more muscle groups. */
    SpreadMuscleFocus,

    /** Push closer to failure / add a hard set so the session classifies. */
    AddOneHardSet,

    /** Pick a knob (heavier / more sets / shorter rest) to clarify the profile. */
    PickAClearerStyle,
}
