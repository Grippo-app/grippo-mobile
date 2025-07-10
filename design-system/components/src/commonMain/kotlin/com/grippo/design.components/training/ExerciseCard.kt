package com.grippo.design.components.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onExerciseExampleClick: (id: String) -> Unit
) {
    Column(modifier = modifier) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
//        ) {
//            Text(
//                modifier = Modifier.weight(1f),
//                text = value.name,
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis,
//                style = AppTokens.typography.h4(),
//                color = AppTokens.colors.text.primary
//            )
//
//            val example = value.exerciseExample
//
//            if (example != null) {
//                val clickProvider = remember(example) {
//                    { onExerciseExampleClick.invoke(example.id) }
//                }
//                Button(
//                    text = AppTokens.strings.res(Res.string.overview),
//                    style = ButtonStyle.Transparent,
//                    endIcon = AppTokens.icons.NavArrowRight,
//                    onClick = clickProvider
//                )
//            }
//        }

        val example = value.exerciseExample

        val iconId = "exercise_example_icon"
        val (name, content) = if (example != null) {
            val annotated = buildAnnotatedString {
                append(value.name)
                append(" ")
                appendInlineContent(iconId)
            }

            val clickProvider = remember(example) {
                { onExerciseExampleClick.invoke(example.id) }
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
                                .scalableClick(onClick = clickProvider)
                                .fillMaxHeight()
                                .aspectRatio(1f),
                            imageVector = AppTokens.icons.InfoEmpty,
                            contentDescription = null,
                            tint = AppTokens.colors.icon.accent
                        )
                    }
                )
            )

            annotated to content
        } else {
            buildAnnotatedString { append(value.name) } to emptyMap()
        }

        Text(
            modifier = Modifier.weight(1f),
            text = name,
            inlineContent = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        IterationsCard(
            modifier = Modifier.fillMaxWidth(),
            value = value.iterations
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))
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