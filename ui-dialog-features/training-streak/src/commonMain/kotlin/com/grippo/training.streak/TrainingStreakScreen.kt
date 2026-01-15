package com.grippo.training.streak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.TrainingStreakCard
import com.grippo.design.components.metrics.TrainingStreakInsightsCard
import com.grippo.design.components.metrics.TrainingStreakTimelineCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak
import com.grippo.design.resources.provider.value_training_streak
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingStreakScreen(
    state: TrainingStreakDialogState,
    loaders: ImmutableSet<TrainingStreakLoader>,
    contract: TrainingStreakContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.range.label()?.let {
                AppTokens.strings.res(Res.string.value_training_streak, it)
            } ?: AppTokens.strings.res(Res.string.training_streak),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.range.formatted(),
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        if (loaders.contains(TrainingStreakLoader.Content)) {
            Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = AppTokens.dp.dialog.horizontalPadding,
                    end = AppTokens.dp.dialog.horizontalPadding,
                    top = AppTokens.dp.contentPadding.content,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
            ) {
                state.streak?.let { streak ->
                    item(key = "summary") {
                        TrainingStreakCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = streak
                        )
                    }

                    item(key = "insights") {
                        TrainingStreakInsightsCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = streak
                        )
                    }

                    item(key = "timeline") {
                        TrainingStreakTimelineCard(
                            modifier = Modifier.fillMaxWidth(),
                            entries = streak.timeline
                        )
                    }
                }

                item("bottom_space") {
                    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingStreakScreen(
            state = TrainingStreakDialogState(
                range = DateRange.Range.Last7Days().range,
                streak = stubTrainingStreaks().first()
            ),
            loaders = persistentSetOf(),
            contract = TrainingStreakContract.Empty
        )
    }
}
