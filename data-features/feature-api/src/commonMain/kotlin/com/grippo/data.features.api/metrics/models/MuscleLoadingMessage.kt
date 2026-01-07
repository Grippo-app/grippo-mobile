package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class MuscleLoadingMessage(
    val kind: MuscleLoadingMessageKind,
    val tone: MuscleLoadingMessageTone,
    val suffix: MuscleLoadingMessageSuffix,
    val variant: Int,

    val strength: MuscleLoadingMessageStrength,

    val primary: FocusTarget?,
    val secondary: FocusTarget?,

    val involvedGroups: List<MuscleGroupEnum>,
    val topGroups: List<GroupShare>,
    val topMuscles: List<MuscleShare>,

    val dominance: DominanceInfo,
)

public enum class MuscleLoadingMessageStrength {
    Strong,
    Medium,
    Light,
    None,
}

public enum class MuscleLoadingMessageKind {
    DominantGroup,
    DualGroup,
    Balanced,
    Mixed,
    Empty,
}

public enum class MuscleLoadingMessageTone {
    Compact,
    Normal,
}

public enum class MuscleLoadingMessageSuffix {
    None,
    First,
    Regular,
}

public data class FocusTarget(
    val group: MuscleGroupEnum,
    val muscle: MuscleEnum?,
)

public data class GroupShare(
    val group: MuscleGroupEnum,
    val percent: Float,
)

public data class MuscleShare(
    val group: MuscleGroupEnum,
    val muscle: MuscleEnum,
    /**
     * This is a normalized score from per-muscle breakdown (0..100).
     * Use it for ranking/highlighting, not for "share of total" claims.
     */
    val score: Float,
)

public data class DominanceInfo(
    val top1SharePercent: Float,
    val top2SharePercent: Float,
)
