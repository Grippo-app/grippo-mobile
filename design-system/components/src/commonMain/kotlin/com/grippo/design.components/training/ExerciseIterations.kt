package com.grippo.design.components.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.cards.information.InformationCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.set_value
import com.grippo.presentation.api.trainings.models.IterationState
import com.grippo.presentation.api.trainings.models.stubExercise
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun ExerciseIterations(
    modifier: Modifier = Modifier,
    iterations: ImmutableList<IterationState>,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = AppTokens.dp.exerciseIterations.horizontalPadding)
    ) {
        iterations.forEachIndexed { index, iteration ->
            InformationCard(
                modifier = Modifier.fillMaxWidth(),
                label = AppTokens.strings.res(Res.string.set_value, "${index + 1}"),
                value = {
                    IterationCard(
                        modifier = Modifier,
                        value = iteration,
                    )
                }
            )

            if (index < iterations.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTokens.colors.divider.default
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseIterationsPreview() {
    PreviewContainer {
        ExerciseIterations(
            iterations = stubExercise().iterations,
        )
    }
}