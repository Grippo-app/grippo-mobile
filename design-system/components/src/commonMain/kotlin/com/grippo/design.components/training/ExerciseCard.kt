package com.grippo.design.components.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.NavArrowRight
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onExerciseClick: (id: String) -> Unit,
    onExerciseExampleClick: (id: String) -> Unit,
) {
    val openExerciseProvider = remember {
        { onExerciseClick.invoke(value.id) }
    }

    Column(modifier = modifier) {

        Row(
            modifier = Modifier
                .scalableClick(onClick = openExerciseProvider)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )

            Icon(
                modifier = Modifier.size(AppTokens.dp.exerciseCard.icon),
                imageVector = AppTokens.icons.NavArrowRight,
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseCardPreview() {
    PreviewContainer {
        ExerciseCard(
            value = stubExercise(),
            onExerciseExampleClick = {},
            onExerciseClick = {}
        )
    }
}