package com.grippo.muscle.loading.details

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.per_group
import com.grippo.design.resources.provider.per_muscle

@Immutable
public data class MuscleLoadingDetailsState(
    val range: DateRangeFormatState,
    val summary: MuscleLoadSummaryState? = null,
    val mode: MuscleLoadingDetailsShowingMode = MuscleLoadingDetailsShowingMode.PerGroup
)

@Immutable
public enum class MuscleLoadingDetailsShowingMode(
    public val id: String,
    public val text: UiText,
) {
    PerGroup("per_group", UiText.Res(Res.string.per_group)),
    PerMuscle("per_muscle", UiText.Res(Res.string.per_muscle)),
}