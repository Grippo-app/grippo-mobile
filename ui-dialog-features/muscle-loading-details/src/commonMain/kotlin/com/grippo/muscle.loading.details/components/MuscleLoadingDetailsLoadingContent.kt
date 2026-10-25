package com.grippo.muscle.loading.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardSkeleton
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingImagesRowSkeleton
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingSkeleton
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

@Composable
internal fun MuscleLoadingDetailsLoadingContent(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        MuscleLoadingCardSkeleton(
            modifier = Modifier.fillMaxWidth(),
            style = MuscleLoadingCardStyle.Summary,
        )

        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.segment.outline.height),
            animated = false,
        )

        MuscleLoadingImagesRowSkeleton(modifier = Modifier.fillMaxWidth())

        MuscleLoadingSkeleton(modifier = Modifier.fillMaxWidth())

        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.button.medium.height),
            shape = RoundedCornerShape(AppTokens.dp.button.medium.radius),
            animated = false,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingDetailsLoadingContentPreview() {
    PreviewContainer {
        MuscleLoadingDetailsLoadingContent(modifier = Modifier.fillMaxWidth())
    }
}
