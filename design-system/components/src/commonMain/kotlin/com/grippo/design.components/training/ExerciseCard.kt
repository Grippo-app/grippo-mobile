package com.grippo.design.components.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.NavArrowRight
import com.grippo.design.resources.overview
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onExerciseExampleClick: (id: String) -> Unit
) {
    Column(
        modifier = modifier
            .padding(
                vertical = AppTokens.dp.exerciseCard.verticalPadding,
                horizontal = AppTokens.dp.exerciseCard.horizontalPadding
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary
            )

            val example = value.exerciseExample

            if (example != null) {
                val clickProvider = remember(example) {
                    { onExerciseExampleClick.invoke(example.id) }
                }
                Button(
                    text = AppTokens.strings.res(Res.string.overview),
                    style = ButtonStyle.Transparent,
                    endIcon = AppTokens.icons.NavArrowRight,
                    onClick = clickProvider
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        IterationsCard(
            modifier = Modifier.fillMaxWidth(),
            value = value.iterations
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardPreview() {
    PreviewContainer {
        ExerciseCard(
            value = stubExercise(),
            onExerciseExampleClick = {}
        )
    }
}