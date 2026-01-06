package com.grippo.muscle.loading

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.per_group
import com.grippo.design.resources.provider.per_muscle
import com.grippo.toolkit.date.utils.DateRange

@Immutable
public data class MuscleLoadingState(
    val range: DateRange,
    val summary: MuscleLoadSummaryState? = null,
    val mode: MuscleLoadingShowingMode = MuscleLoadingShowingMode.PerGroup
)

@Immutable
public enum class MuscleLoadingShowingMode(
    public val id: String,
    public val text: UiText,
) {
    PerGroup("per_group", UiText.Res(Res.string.per_group)),
    PerMuscle("per_muscle", UiText.Res(Res.string.per_muscle)),
}