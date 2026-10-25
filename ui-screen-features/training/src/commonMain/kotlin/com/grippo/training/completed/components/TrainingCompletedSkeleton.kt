package com.grippo.training.completed.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardSkeleton
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardSkeleton
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardStyle
import com.grippo.design.components.metrics.performance.PerformanceComparisonCardSkeleton
import com.grippo.design.components.metrics.volume.WeeklyVolumeCardSkeleton
import com.grippo.design.components.training.TrainingRecapHeroSkeleton
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

@Composable
internal fun TrainingCompletedSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)

    LazyColumn(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        state = listState,
        contentPadding = contentPadding,
    ) {
        item(key = "completion_hero_skeleton") {
            TrainingRecapHeroSkeleton(
                modifier = Modifier.fillMaxWidth(),
                windowInsets = WindowInsets.statusBars,
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))
        }

        item(key = "performance_comparison_skeleton") {
            PerformanceComparisonCardSkeleton(modifier = cardModifier)
        }

        item(key = "weekly_volume_skeleton") {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            WeeklyVolumeCardSkeleton(modifier = cardModifier)
        }

        item(key = "training_streak_skeleton") {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            TrainingStreakCardSkeleton(
                modifier = cardModifier,
                style = TrainingStreakCardStyle.Growth(previous = null),
            )
        }

        item(key = "muscle_load_skeleton") {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            MuscleLoadingCardSkeleton(
                modifier = cardModifier,
                style = MuscleLoadingCardStyle.Session,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingCompletedSkeletonPreview() {
    PreviewContainer {
        TrainingCompletedSkeleton(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppTokens.dp.contentPadding.content),
            listState = rememberLazyListState(),
        )
    }
}
