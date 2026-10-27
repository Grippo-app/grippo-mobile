package com.grippo.training.completed.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.grippo.design.components.metrics.performance.PerformanceComparisonSectionSkeleton
import com.grippo.design.components.metrics.volume.WeeklyVolumeSectionSkeleton
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

@Composable
internal fun TrainingCompletedSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val sectionModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)

    LazyColumn(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.block,
            bottom = contentPadding.calculateBottomPadding(),
        ),
    ) {
        item(key = "performance_comparison_skeleton") {
            PerformanceComparisonSectionSkeleton(modifier = sectionModifier)
        }

        item(key = "progress_heading_skeleton") {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

            SkeletonText(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(AppTokens.dp.skeleton.line.short),
                style = AppTokens.typography.h4(),
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
        }

        item(key = "weekly_volume_skeleton") {
            WeeklyVolumeSectionSkeleton(modifier = sectionModifier)
        }

        item(key = "training_streak_skeleton") {
            ProgressDivider(modifier = sectionModifier)

            TrainingStreakCardSkeleton(
                modifier = sectionModifier,
                style = TrainingStreakCardStyle.Growth(previous = null),
            )
        }

        item(key = "muscle_load_skeleton") {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

            MuscleLoadingCardSkeleton(
                modifier = sectionModifier,
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
        )
    }
}
