package com.grippo.muscle.loading.details

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun MuscleLoadingDetailsScreen(
    state: MuscleLoadingDetailsState,
    loaders: ImmutableSet<MuscleLoadingDetailsLoader>,
    contract: MuscleLoadingDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPerGroupPreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = MuscleLoadingDetailsState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                summary = stubMuscleLoadSummary(),
                mode = MuscleLoadingDetailsShowingMode.PerGroup
            ),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPerMusclePreview() {
    PreviewContainer {
        MuscleLoadingDetailsScreen(
            state = MuscleLoadingDetailsState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                summary = stubMuscleLoadSummary(),
                mode = MuscleLoadingDetailsShowingMode.PerMuscle
            ),
            loaders = persistentSetOf(),
            contract = MuscleLoadingDetailsContract.Empty
        )
    }
}
