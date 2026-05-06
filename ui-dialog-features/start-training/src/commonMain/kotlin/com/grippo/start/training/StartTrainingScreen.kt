package com.grippo.start.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.indicators.PagerIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.start_training_description
import com.grippo.design.resources.provider.start_training_title
import com.grippo.design.resources.provider.start_training_use_btn
import com.grippo.start.training.internal.StartTrainingPage
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

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

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.start_training_description),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val options = state.options
    val pagerState = rememberPagerState(pageCount = { options.size })

    LaunchedEffect(options.size, pagerState) {
        val size = options.size
        if (pagerState.currentPage >= size && size > 0) {
            pagerState.scrollToPage(size - 1)
        }
    }

    val activeKey by remember(options) {
        derivedStateOf {
            options.getOrNull(pagerState.currentPage)?.key
        }
    }

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.subContent,
            bottom = AppTokens.dp.contentPadding.block,
        ),
        overlay = AppTokens.colors.background.dialog,
        content = { containerModifier, resolvedPadding ->
            HorizontalPager(
                modifier = containerModifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_PAGE_HEIGHT),
                state = pagerState,
                contentPadding = resolvedPadding,
                pageSpacing = AppTokens.dp.contentPadding.subContent,
                key = { index -> options.getOrNull(index)?.key ?: index },
            ) { index ->
                val option = options.getOrNull(index) ?: return@HorizontalPager
                StartTrainingPage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                    option = option,
                )
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

            PagerIndicator(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                    .fillMaxWidth(),
                pageCount = options.size,
                selectedIndex = pagerState.currentPage,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

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

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )

}

private val MIN_PAGE_HEIGHT = 280.dp

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
