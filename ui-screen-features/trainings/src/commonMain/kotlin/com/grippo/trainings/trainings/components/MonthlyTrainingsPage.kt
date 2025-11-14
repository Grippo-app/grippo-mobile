package com.grippo.trainings.trainings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.digest.DailyDigestState
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.trainings
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MonthlyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
) {
    val gridState = rememberLazyGridState()
    val digests = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.DailyDigest>()
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        if (digests.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                TrainingsEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.block)
                )
            }
        } else {
            items(digests, key = { it.key }) { digest ->
                MonthlyDigestCell(
                    modifier = Modifier.fillMaxWidth(),
                    state = digest.state
                )
            }
        }
    }
}

@Composable
private fun MonthlyDigestCell(
    modifier: Modifier = Modifier,
    state: DailyDigestState,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.contentPadding.block))
            .background(AppTokens.colors.background.card)
            .padding(AppTokens.dp.contentPadding.content),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = DateTimeUtils.format(state.date, DateFormat.DATE_DD_MMM),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            text = state.trainingsCount.toString(),
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary
        )

        Text(
            text = AppTokens.strings.res(Res.string.trainings),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            text = state.exercisesCount.toString(),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary
        )

        Text(
            text = AppTokens.strings.res(Res.string.exercises),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )
    }
}