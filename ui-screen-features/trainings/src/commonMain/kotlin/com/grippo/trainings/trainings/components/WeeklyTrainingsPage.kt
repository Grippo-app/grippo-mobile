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
import com.grippo.design.components.digest.DailyDigestCard
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun WeeklyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
    onViewStatsClick: () -> Unit,
) {
    val listState = rememberLazyListState()

    val digests = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.DailyDigest>()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        if (digests.isEmpty()) {
            item {
                TrainingsEmptyState(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.block)
                )
            }
        } else {
            items(digests, key = { it.key }) { digest ->
                DailyDigestCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = digest.state,
                    onViewStatsClick = onViewStatsClick
                )
            }
        }
    }
}
