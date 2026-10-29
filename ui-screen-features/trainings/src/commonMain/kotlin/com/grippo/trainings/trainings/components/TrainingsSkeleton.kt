package com.grippo.trainings.trainings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.metrics.engagement.digest.DigestCardSkeleton
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.training.ExerciseCardSkeleton
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading
import com.grippo.toolkit.date.utils.DateCompose

@Composable
internal fun DailyTrainingsSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)

    LazyColumn(
        modifier = modifier.fillMaxSize().semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        contentPadding = contentPadding,
    ) {
        item {
            SkeletonText(
                modifier = Modifier
                    .padding(start = AppTokens.dp.dot.size + AppTokens.dp.contentPadding.subContent * 2)
                    .fillMaxWidth(AppTokens.dp.skeleton.line.medium)
                    .padding(vertical = AppTokens.dp.contentPadding.subContent),
                style = AppTokens.typography.b14Semi(),
            )
        }
        items(count = 3) {
            ExerciseCardSkeleton(
                modifier = Modifier
                    .padding(start = AppTokens.dp.dot.size + AppTokens.dp.contentPadding.subContent)
                    .fillMaxWidth(),
                style = ExerciseCardStyle.Medium(onClick = {}),
            )
        }
    }
}

@Composable
internal fun MonthlyTrainingsSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    weekCount: Int,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val weekDayLabels = DateCompose.rememberWeekDayShortLabels()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .semantics(mergeDescendants = true) {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
    ) {
        DigestCardSkeleton(modifier = Modifier.fillMaxWidth())

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
                weekDayLabels.forEach { label ->
                    Text(
                        modifier = Modifier.weight(1f),
                        text = label,
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            repeat(weekCount) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
                    repeat(weekDayLabels.size) {
                        SkeletonBone(
                            modifier = Modifier.weight(1f).height(AppTokens.dp.calendar.monthly.cellHeight),
                            shape = RoundedCornerShape(AppTokens.dp.focusFrame.radius),
                            animated = false,
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun DailyTrainingsSkeletonPreview() {
    PreviewContainer {
        DailyTrainingsSkeleton(contentPadding = PaddingValues(AppTokens.dp.contentPadding.content))
    }
}

@AppPreview
@Composable
private fun MonthlyTrainingsSkeletonPreview() {
    PreviewContainer {
        MonthlyTrainingsSkeleton(contentPadding = PaddingValues(AppTokens.dp.contentPadding.content), weekCount = 5)
    }
}
