package com.grippo.training.completed

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.HorizontalDivider
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
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.menu.MenuCard
import com.grippo.design.components.menu.MenuTrailing
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.complete
import com.grippo.design.resources.provider.workout_saved
import com.grippo.domain.state.training.toTrainingListValues
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.TrainingListValue
import com.grippo.state.trainings.TrainingListValue.Companion.exercise
import com.grippo.state.trainings.TrainingListValue.Companion.index
import com.grippo.state.trainings.TrainingListValue.Companion.shape
import com.grippo.state.trainings.stubTraining
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay

@Composable
internal fun TrainingCompletedScreen(
    state: TrainingCompletedState,
    loaders: ImmutableSet<TrainingCompletedLoader>,
    contract: TrainingCompletedContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    if (loaders.contains(TrainingCompletedLoader.SaveTraining)) {
        Loader(modifier = Modifier.fillMaxSize())
        return@BaseComposeScreen
    }

    val cardVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        cardVisible.value = true
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
                .navigationBarsPadding()
                .fillMaxSize()
                .padding(
                    horizontal = AppTokens.dp.screen.horizontalPadding,
                    vertical = AppTokens.dp.contentPadding.content
                ).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.workout_saved),
                style = AppTokens.typography.h1(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            if (state.training.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .offset(y = offsetY)
                        .alpha(alpha),
                    contentPadding = PaddingValues(
                        horizontal = AppTokens.dp.screen.horizontalPadding,
                        vertical = AppTokens.dp.contentPadding.content
                    ),
                    verticalArrangement = Arrangement.Center
                ) {
                    items(
                        items = state.training,
                        key = { it.id },
                        contentType = { it::class }
                    ) { value ->
                        val radius = AppTokens.dp.exerciseCard.radius
                        val shape = remember(value) { value.shape(radius) }
                        val exercise = remember(value) { value.exercise() }
                        val index = remember(value) { value.index() }

                        if (exercise != null) {
                            val clickProvider = remember {
                                { contract.onExerciseClick(exercise.id) }
                            }

                            val trailing = index?.let {
                                MenuTrailing.Text(UiText.Str("$it."))
                            } ?: MenuTrailing.Empty

                            MenuCard(
                                modifier = Modifier
                                    .background(AppTokens.colors.background.card, shape)
                                    .fillMaxWidth(),
                                title = exercise.name,
                                trailing = trailing,
                                onClick = clickProvider
                            )
                        }

                        if (value is TrainingListValue.BetweenExercises) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .background(AppTokens.colors.background.card, shape)
                                    .padding(horizontal = AppTokens.dp.menu.item.horizontalPadding)
                                    .fillMaxWidth(),
                                color = AppTokens.colors.divider.default
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.complete),
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onBack
            )
        }

        if (cardVisible.value) {
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
                training = stubTraining().toTrainingListValues()
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty
        )
    }
}
