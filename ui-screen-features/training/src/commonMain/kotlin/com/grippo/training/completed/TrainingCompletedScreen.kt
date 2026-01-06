package com.grippo.training.completed

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.trainings.TimelineState.Companion.exercise
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.go_to_dashboard
import com.grippo.design.resources.provider.workout_summary
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingCompletedScreen(
    state: TrainingCompletedState,
    loaders: ImmutableSet<TrainingCompletedLoader>,
    contract: TrainingCompletedContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    )
) {
    if (loaders.contains(TrainingCompletedLoader.SaveTraining)) {
        Loader(modifier = Modifier.fillMaxSize())
        return@BaseComposeScreen
    }

    val cardVisible = remember { mutableStateOf(false) }

    LaunchedEffect(state.timeline, loaders) {
        val hasLoader = loaders.contains(TrainingCompletedLoader.SaveTraining)
        val hasTraining = state.timeline.isNotEmpty()
        cardVisible.value = hasTraining && hasLoader.not()
    }

    val alpha by animateFloatAsState(
        targetValue = if (cardVisible.value) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "alpha"
    )

    val offsetY by animateDpAsState(
        targetValue = if (cardVisible.value) 0.dp else 40.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "offsetY"
    )

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = AppTokens.dp.screen.horizontalPadding,
                ).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.workout_summary),
                style = AppTokens.typography.h1(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            if (cardVisible.value) {
                BottomOverlayContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    content = { containerModifier, resolvedPadding ->
                        LazyColumn(
                            modifier = containerModifier
                                .fillMaxSize()
                                .offset(y = offsetY)
                                .alpha(alpha),
                            contentPadding = resolvedPadding,
                            verticalArrangement = Arrangement.Center
                        ) {
                            items(
                                items = state.timeline,
                                key = { it.key },
                                contentType = { it::class }
                            ) { value ->
                                val exercise = remember(value.key) { value.exercise() }

                                if (exercise != null) {
                                    val clickProvider = remember(exercise.id) {
                                        { contract.onExerciseClick(exercise.id) }
                                    }

                                    ExerciseCard(
                                        modifier = Modifier
                                            .padding(vertical = AppTokens.dp.contentPadding.subContent)
                                            .fillMaxWidth(),
                                        value = exercise,
                                        style = ExerciseCardStyle.Medium(clickProvider)
                                    )
                                }
                            }
                        }
                    },
                    overlay = AppTokens.colors.background.screen,
                    bottom = {
                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            content = ButtonContent.Text(
                                text = AppTokens.strings.res(Res.string.go_to_dashboard),
                            ),
                            style = ButtonStyle.Primary,
                            onClick = contract::onBack
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                )
            }
        }

        if (cardVisible.value && cardVisible.value) {
            KonfettiParade()
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(
                timeline = stubDailyTrainingTimeline()
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty
        )
    }
}