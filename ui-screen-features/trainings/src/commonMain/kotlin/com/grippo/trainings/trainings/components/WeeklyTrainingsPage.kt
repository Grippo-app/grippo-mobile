package com.grippo.trainings.trainings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.digest.WeeklyDigestCard
import com.grippo.design.components.training.TrainingsCard
import com.grippo.design.components.training.TrainingsCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun WeeklyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
    onViewStatsClick: () -> Unit,
) {
    val listState = rememberLazyListState()

    val summaries = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.WeeklySummary>()
    }

    val weeklyTrainings = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.WeeklyTrainingsDay>()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        if (summaries.isEmpty() && weeklyTrainings.isEmpty()) {
            item {
                TrainingsEmptyState(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.block)
                )
            }
        } else {
            items(summaries, key = { it.key }) { summary ->
                WeeklyDigestCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = summary.summary,
                    onViewStatsClick = onViewStatsClick
                )
            }

            items(weeklyTrainings, key = { it.key }) { item ->
                TrainingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    trainings = item.trainings,
                    style = TrainingsCardStyle.Weekly
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun WeeklyTrainingsPagePreview() {
    PreviewContainer {
        WeeklyTrainingsPage(
            trainings = listOf(
                stubTraining(),
                stubTraining(),
            ).transformToTrainingListValue(
                range = DateTimeUtils.thisWeek()
            ),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onViewStatsClick = {}
        )
    }
}
