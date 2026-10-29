package com.grippo.exercise.example.exerciseexample.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingSkeleton
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

@Composable
internal fun ExerciseExampleDetailsSkeleton(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)

    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
            SkeletonBone(
                modifier = Modifier.size(AppTokens.dp.exerciseExampleImage.large.size),
                shape = RoundedCornerShape(AppTokens.dp.exerciseExampleImage.large.radius),
            )
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                style = AppTokens.typography.h1(),
            )
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                style = AppTokens.typography.b13Med(),
            )
        }

        SkeletonBone(
            modifier = Modifier.fillMaxWidth().height(AppTokens.dp.segment.outline.height),
            animated = false,
        )

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            repeat(3) {
                SkeletonText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.b14Med())
            }
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                style = AppTokens.typography.b14Med(),
            )
        }

        MuscleLoadingSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsSkeletonPreview() {
    PreviewContainer {
        ExerciseExampleDetailsSkeleton(modifier = Modifier.fillMaxWidth())
    }
}
