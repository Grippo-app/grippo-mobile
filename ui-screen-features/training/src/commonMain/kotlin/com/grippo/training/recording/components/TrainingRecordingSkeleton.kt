package com.grippo.training.recording.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.training.ExerciseCardSkeleton
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

@Composable
internal fun TrainingRecordingHeaderSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        repeat(3) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                    style = AppTokens.typography.h5(),
                )
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                    style = AppTokens.typography.b12Med(),
                )
            }
        }
    }
}

@Composable
internal fun TrainingRecordingSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)

    LazyColumn(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        contentPadding = contentPadding,
    ) {
        items(count = 3) {
            ExerciseCardSkeleton(modifier = Modifier.fillMaxWidth(), style = ExerciseCardStyle.Large(onClick = {}))
        }
    }
}

@AppPreview
@Composable
private fun TrainingRecordingSkeletonPreview() {
    PreviewContainer {
        TrainingRecordingHeaderSkeleton(modifier = Modifier.fillMaxWidth())
        TrainingRecordingSkeleton(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
        )
    }
}
