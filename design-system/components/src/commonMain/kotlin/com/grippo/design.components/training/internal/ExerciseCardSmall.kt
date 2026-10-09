package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ExerciseCardSmall(
    modifier: Modifier = Modifier,
    value: ExerciseState,
) {
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.exerciseCard.small.radius)
            )
            .padding(
                horizontal = AppTokens.dp.exerciseCard.small.horizontalPadding,
                vertical = AppTokens.dp.exerciseCard.small.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = value.createdAt.display,
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        ExerciseSummary(
            modifier = Modifier.fillMaxWidth(),
            value = value,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardSmallPreview() {
    PreviewContainer {
        ExerciseCardSmall(
            value = stubExercise(),
        )
    }
}
