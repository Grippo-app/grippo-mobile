package com.grippo.design.components.training

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.information.InformationCard
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.ChevronDown
import com.grippo.design.resources.icons.ChevronRight
import com.grippo.design.resources.intensity
import com.grippo.design.resources.kg
import com.grippo.design.resources.percent
import com.grippo.design.resources.set_value
import com.grippo.design.resources.source
import com.grippo.design.resources.tonnage
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onExerciseExampleClick: (id: String) -> Unit
) {

    val expanded = remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (expanded.value) -180f else 0f,
        label = "rotation"
    )

    val shape = RoundedCornerShape(AppTokens.dp.shape.large)

    Column(
        modifier = modifier
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            )
            .clip(shape = shape)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(
                horizontal = AppTokens.dp.paddings.mediumHorizontal,
                vertical = AppTokens.dp.paddings.mediumVertical
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .nonRippleClick(onClick = { expanded.value = expanded.value.not() }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.name,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )

            Icon(
                modifier = Modifier
                    .graphicsLayer { rotationZ = rotation }
                    .size(AppTokens.dp.icon.m),
                imageVector = AppTokens.icons.ChevronDown,
                tint = AppTokens.colors.icon.default,
                contentDescription = null
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = shape)
                .background(AppTokens.colors.background.primary)
                .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
                .padding(horizontal = AppTokens.dp.paddings.mediumHorizontal)
        ) {
            AnimatedContent(
                modifier = Modifier.fillMaxWidth(),
                targetState = expanded.value,
                transitionSpec = {
                    fadeIn(animationSpec = tween(320, delayMillis = 90))
                        .togetherWith(fadeOut(animationSpec = tween(320)))
                }
            ) { ex ->
                when (ex) {
                    true -> Column(modifier = Modifier.fillMaxWidth()) {
                        value.iterations.forEachIndexed { index, iteration ->

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


                            if (index < value.iterations.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = AppTokens.colors.divider.default
                                )
                            }
                        }
                    }

                    false -> Column(modifier = Modifier.fillMaxWidth()) {

                        InformationCard(
                            modifier = Modifier.fillMaxWidth(),
                            label = AppTokens.strings.res(Res.string.tonnage),
                            value = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = value.volume.toString(),
                                        style = AppTokens.typography.b14Bold(),
                                        color = AppTokens.colors.text.primary
                                    )

                                    Text(
                                        text = AppTokens.strings.res(Res.string.kg),
                                        style = AppTokens.typography.b14Semi(),
                                        color = AppTokens.colors.text.secondary
                                    )
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTokens.colors.divider.default
                        )

                        InformationCard(
                            modifier = Modifier.fillMaxWidth(),
                            label = AppTokens.strings.res(Res.string.intensity),
                            value = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = value.intensity.toString(),
                                        style = AppTokens.typography.b14Bold(),
                                        color = AppTokens.colors.text.primary
                                    )

                                    Text(
                                        text = AppTokens.strings.res(Res.string.percent),
                                        style = AppTokens.typography.b14Semi(),
                                        color = AppTokens.colors.text.secondary
                                    )
                                }
                            }
                        )

                        value.exerciseExample?.let { example ->
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = AppTokens.colors.divider.default
                            )

                            InformationCard(
                                modifier = Modifier.fillMaxWidth(),
                                label = AppTokens.strings.res(Res.string.source),
                                value = {
                                    val clickProvider = remember(example) {
                                        { onExerciseExampleClick.invoke(example.id) }
                                    }

                                    Button(
                                        text = example.name,
                                        endIcon = AppTokens.icons.ChevronRight,
                                        style = ButtonStyle.Transparent,
                                        onClick = clickProvider
                                    )
                                }
                            )
                        }
                    }
                }
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
            onExerciseExampleClick = {}
        )
    }
}