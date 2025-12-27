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
import com.grippo.design.components.digest.MonthDigestCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.TrainingsCard
import com.grippo.design.components.training.TrainingsCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

@Composable
internal fun MonthlyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
    onDigestClick: () -> Unit,
    onOpenDaily: (LocalDate) -> Unit,
) {
    val gridState = rememberLazyListState()

    val digest = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.MonthlyDigest>().firstOrNull()
    }
    val days = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.MonthlyTrainingsDay>()
    }
    if (digest == null && days.isEmpty()) {
        TrainingsEmptyState(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = gridState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        digest?.let { value ->
            item(key = value.key) {
                MonthDigestCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = onDigestClick)
                        .padding(vertical = AppTokens.dp.contentPadding.block),
                    value = value.summary,
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
            onDigestClick = {},
            onOpenDaily = { _ -> },
        )
    }
}

@AppPreview
@Composable
private fun MonthlyTrainingsEmptyPagePreview() {
    PreviewContainer {
        MonthlyTrainingsPage(
            trainings = persistentListOf(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onDigestClick = {},
            onOpenDaily = { _ -> },
        )
    }
}
