package com.grippo.trainings.trainings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.digest.MonthlyDigestCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.TrainingsCard
import com.grippo.design.components.training.TrainingsCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate

@Composable
internal fun MonthlyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
    onViewStatsClick: () -> Unit,
    onOpenDaily: (LocalDate) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val digest = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.MonthlyDigest>().firstOrNull()
    }
    val days = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.MonthlyTrainingsDay>()
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        if (digest == null && days.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                TrainingsEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.block)
                )
            }
        } else {
            digest?.let { value ->
                item(span = { GridItemSpan(2) }, key = value.key) {
                    MonthlyDigestCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = value.summary,
                        onViewStatsClick = onViewStatsClick
                    )
                }
            }

            items(days, key = { it.key }) { day ->
                val clickProvider = remember(day.date) {
                    { onOpenDaily(day.date) }
                }
                TrainingsCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = clickProvider),
                    trainings = day.trainings,
                    style = TrainingsCardStyle.Monthly
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MonthlyTrainingsPagePreview() {
    PreviewContainer {
        MonthlyTrainingsPage(
            trainings = listOf(
                stubTraining(),
                stubTraining(),
            ).transformToTrainingListValue(
                range = DateTimeUtils.thisMonth()
            ),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onViewStatsClick = {},
            onOpenDaily = { _ -> }
        )
    }
}
