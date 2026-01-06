package com.grippo.muscle.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.MuscleLoading
import com.grippo.design.components.metrics.MuscleLoadingImagesMode
import com.grippo.design.components.metrics.MuscleLoadingImagesRow
import com.grippo.design.components.metrics.MuscleLoadingMode
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_loading
import com.grippo.design.resources.provider.value_muscle_loading
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MuscleLoadingScreen(
    state: MuscleLoadingState,
    loaders: ImmutableSet<MuscleLoadingLoader>,
    contract: MuscleLoadingContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.range.label()?.let {
                AppTokens.strings.res(Res.string.value_muscle_loading, it)
            } ?: AppTokens.strings.res(Res.string.muscle_loading),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.range.formatted(),
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        val segmentItems = remember {
            MuscleLoadingShowingMode.entries.map { it to it.text }
                .toPersistentList()
        }

        Segment(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            items = segmentItems,
            selected = state.mode,
            onSelect = contract::onSelectMode,
            segmentWidth = SegmentWidth.EqualFill,
            style = SegmentStyle.Fill
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        if (loaders.contains(MuscleLoadingLoader.Content)) {
            Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = AppTokens.dp.dialog.horizontalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
            ) {
                state.summary?.let { summary ->
                    item(key = "images") {
                        val mode = remember(state.mode) {
                            when (state.mode) {
                                MuscleLoadingShowingMode.PerGroup -> MuscleLoadingImagesMode.Collapsed
                                MuscleLoadingShowingMode.PerMuscle -> MuscleLoadingImagesMode.Expanded
                            }
                        }

                        MuscleLoadingImagesRow(
                            modifier = Modifier.fillMaxWidth(),
                            summary = summary,
                            mode = mode
                        )
                    }

                    item(key = "summary") {
                        val mode = remember(state.mode) {
                            when (state.mode) {
                                MuscleLoadingShowingMode.PerGroup -> MuscleLoadingMode.Collapsed
                                MuscleLoadingShowingMode.PerMuscle -> MuscleLoadingMode.Expanded
                            }
                        }

                        MuscleLoading(
                            modifier = Modifier.fillMaxWidth(),
                            summary = summary,
                            mode = mode
                        )
                    }
                }

                item("bottom_space") {
                    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreenPerGroupPreview() {
    PreviewContainer {
        MuscleLoadingScreen(
            state = MuscleLoadingState(
                range = DateTimeUtils.trailingWeek(),
                summary = stubMuscleLoadSummary(),
                mode = MuscleLoadingShowingMode.PerGroup
            ),
            loaders = persistentSetOf(),
            contract = MuscleLoadingContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPerMusclePreview() {
    PreviewContainer {
        MuscleLoadingScreen(
            state = MuscleLoadingState(
                range = DateTimeUtils.trailingWeek(),
                summary = stubMuscleLoadSummary(),
                mode = MuscleLoadingShowingMode.PerMuscle
            ),
            loaders = persistentSetOf(),
            contract = MuscleLoadingContract.Empty
        )
    }
}
