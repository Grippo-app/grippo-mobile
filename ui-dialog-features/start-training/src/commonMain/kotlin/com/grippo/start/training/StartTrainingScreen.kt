package com.grippo.start.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.indicators.PagerIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.start_training_title
import com.grippo.design.resources.provider.start_training_use_btn
import com.grippo.start.training.internal.StartTrainingPage
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

internal val overlayReservedHeight: Dp
    @Composable
    @ReadOnlyComposable
    get() = AppTokens.dp.contentPadding.block +
            (AppTokens.dp.pagerIndicator.dotSize + AppTokens.dp.contentPadding.subContent * 2) +
            AppTokens.dp.contentPadding.block +
            AppTokens.dp.button.medium.height +
            AppTokens.dp.dialog.bottom

@Composable
internal fun StartTrainingScreen(
    state: StartTrainingState,
    loaders: ImmutableSet<StartTrainingLoader>,
    contract: StartTrainingContract
) = BaseComposeScreen(
    background = ScreenBackground.Color(AppTokens.colors.background.dialog)
) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.start_training_title),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

    val pagerState = rememberPagerState(
        pageCount = { state.options.size }
    )

    LaunchedEffect(state.options.size, pagerState) {
        val size = state.options.size
        if (pagerState.currentPage >= size && size > 0) {
            pagerState.scrollToPage(size - 1)
        }
    }

    var didInitialFocusOnEmpty by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(state.options) {
        if (didInitialFocusOnEmpty) return@LaunchedEffect
        if (state.options.size <= 1) return@LaunchedEffect

        val emptyIndex = state.options.indexOfFirst { it is StartTrainingOption.Empty }
        if (emptyIndex >= 0 && pagerState.currentPage != emptyIndex) {
            pagerState.scrollToPage(emptyIndex)
        }
        didInitialFocusOnEmpty = true
    }

    val activeKey by remember(state.options) {
        derivedStateOf {
            state.options.getOrNull(pagerState.currentPage)?.key
        }
    }

    val pagerScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .navigationBarsPadding(),
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            pageSpacing = AppTokens.dp.contentPadding.subContent,
            key = { index -> state.options.getOrNull(index)?.key ?: index },
        ) { index ->

            val option = state.options.getOrNull(index)
                ?: return@HorizontalPager

            StartTrainingPage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                option = option,
                previousOption = state.options.getOrNull(index - 1),
                nextOption = state.options.getOrNull(index + 1),
                onSwipePrevious = {
                    pagerScope.launch {
                        pagerState.animateScrollToPage((index - 1).coerceAtLeast(0))
                    }
                },
                onSwipeNext = {
                    pagerScope.launch {
                        pagerState.animateScrollToPage(
                            (index + 1).coerceAtMost(state.options.lastIndex)
                        )
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(overlayReservedHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            PagerIndicator(
                pageCount = state.options.size,
                selectedIndex = pagerState.currentPage,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_training_use_btn),
                ),
                style = ButtonStyle.Primary,
                onClick = {
                    val key = activeKey ?: return@Button
                    contract.onSelect(key)
                }
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            StartTrainingScreen(
                state = StartTrainingState(),
                contract = StartTrainingContract.Empty,
                loaders = persistentSetOf(),
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreviewWithOptions() {
    PreviewContainer {
        StartTrainingScreen(
            state = StartTrainingState(
                options = persistentListOf(
                    StartTrainingOption.Empty,
                    StartTrainingOption.Preset(stubExercises()),
                    StartTrainingOption.Recent(
                        trainingId = "stub-1",
                        createdAt = DateTimeFormatState.of(
                            value = DateTimeUtils.now(),
                            range = DateRangePresets.infinity(),
                            format = DateFormat.DateOnly.DateMmmDdYyyy,
                        ),
                        exercises = stubExercises(),
                    ),
                ),
            ),
            contract = StartTrainingContract.Empty,
            loaders = persistentSetOf(),
        )
    }
}
