package com.grippo.design.components.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.InfoEmpty
import com.grippo.design.resources.icons.NavArrowRight
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onExerciseClick: (id: String) -> Unit,
    onExerciseExampleClick: (id: String) -> Unit,
) {
    Column(modifier = modifier) {

        val iconId = "open_exercise_icon"

        val annotated = buildAnnotatedString {
            append(value.name)
            append(" ")
            appendInlineContent(iconId)
        }

        val openExerciseProvider = remember {
            { onExerciseClick.invoke(value.id) }
        }

        val content = mapOf(
            iconId to InlineTextContent(
                placeholder = Placeholder(
                    width = AppTokens.typography.h4().fontSize,
                    height = AppTokens.typography.h4().fontSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                ),
                children = {
                    Icon(
                        modifier = Modifier
                            .scalableClick(onClick = openExerciseProvider)
                            .fillMaxSize(),
                        imageVector = AppTokens.icons.NavArrowRight,
                        tint = AppTokens.colors.icon.primary,
                        contentDescription = null,
                    )
                }
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {

            Text(
                modifier = Modifier.weight(1f),
                text = annotated,
                inlineContent = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )

            val example = value.exerciseExample

            if (example != null) {
                val openExerciseExampleProvider = remember(example) {
                    { onExerciseExampleClick.invoke(example.id) }
                }

                Icon(
                    modifier = Modifier
                        .scalableClick(onClick = openExerciseExampleProvider)
                        .size(AppTokens.dp.exerciseCard.icon),
                    imageVector = AppTokens.icons.InfoEmpty,
                    tint = AppTokens.colors.icon.secondary,
                    contentDescription = null,
                )
            }
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