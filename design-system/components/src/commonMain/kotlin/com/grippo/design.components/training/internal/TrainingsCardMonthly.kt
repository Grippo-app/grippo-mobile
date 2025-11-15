package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.trainings
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TrainingsCardMonthly(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingState>,
) {
    if (trainings.isEmpty()) return

    val dateLabel = remember(trainings) {
        val date = trainings.minByOrNull { it.createdAt }!!.createdAt.date
        DateTimeUtils.format(date, DateFormat.DATE_DD_MMM)
    }

    val trainingsCount = remember(trainings) { trainings.size }

    val exercisesCount = remember(trainings) {
        trainings.sumOf { it.exercises.size }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.trainingCard.monthly.radius))
            .background(AppTokens.colors.background.card)
            .padding(AppTokens.dp.contentPadding.content),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dateLabel,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            text = trainingsCount.toString(),
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
            text = exercisesCount.toString(),
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

@AppPreview
@Composable
private fun TrainingsCardMonthlyPreview() {
    PreviewContainer {
        TrainingsCardMonthly(
            trainings = persistentListOf(stubTraining(), stubTraining())
        )
    }
}
