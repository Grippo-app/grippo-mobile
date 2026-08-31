package com.grippo.training.streak.details

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingStreakDetailsScreen(
    state: TrainingStreakDetailsDialogState,
    loaders: ImmutableSet<TrainingStreakDetailsLoader>,
    contract: TrainingStreakDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = TrainingStreakDetailsDialogState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                streak = stubTrainingStreaks().first()
            ),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty
        )
    }
}
