package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ExerciseCardSkeleton(
    modifier: Modifier = Modifier,
    style: ExerciseCardStyle,
) {
    when (style) {
        is ExerciseCardStyle.Small -> {
            val dimensions = AppTokens.dp.exerciseCard.small
            val shape = RoundedCornerShape(dimensions.radius)

            Column(
                modifier = modifier
                    .background(AppTokens.colors.background.card, shape)
                    .border(dimensions.borderWidth, AppTokens.colors.border.default, shape)
                    .padding(horizontal = dimensions.horizontalPadding, vertical = dimensions.verticalPadding),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.short),
                    style = AppTokens.typography.h6(),
                )
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                    style = AppTokens.typography.b14Semi(),
                )
            }
        }

        is ExerciseCardStyle.Large,
        is ExerciseCardStyle.Medium,
        ExerciseCardStyle.Planned -> {
            val compact = style !is ExerciseCardStyle.Large
            val imageSize = if (compact) {
                AppTokens.dp.exerciseExampleImage.compact.size
            } else {
                AppTokens.dp.exerciseExampleImage.medium.size
            }
            val imageRadius = if (compact) {
                AppTokens.dp.exerciseExampleImage.compact.radius
            } else {
                AppTokens.dp.exerciseExampleImage.medium.radius
            }
            val titleStyle = if (compact) AppTokens.typography.b14Semi() else AppTokens.typography.h5()
            val spacing = if (compact) AppTokens.dp.contentPadding.subContent else AppTokens.dp.contentPadding.content

            Row(
                modifier = modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBone(modifier = Modifier.size(imageSize), shape = RoundedCornerShape(imageRadius))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                ) {
                    SkeletonText(
                        modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                        style = titleStyle,
                    )
                    SkeletonText(
                        modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                        style = if (style == ExerciseCardStyle.Planned) {
                            AppTokens.typography.b12Med()
                        } else {
                            AppTokens.typography.b13Med()
                        },
                    )
                }

                if (style is ExerciseCardStyle.Large || style is ExerciseCardStyle.Medium && style.onClick != null) {
                    Spacer(modifier = Modifier.size(AppTokens.dp.exerciseCard.chevron))
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseCardSkeletonMediumPreview() {
    PreviewContainer {
        ExerciseCardSkeleton(modifier = Modifier.fillMaxWidth(), style = ExerciseCardStyle.Medium())
        ExerciseCardSkeleton(modifier = Modifier.fillMaxWidth(), style = ExerciseCardStyle.Medium(onClick = {}))
    }
}

@AppPreview
@Composable
private fun ExerciseCardSkeletonLargePreview() {
    PreviewContainer {
        ExerciseCardSkeleton(modifier = Modifier.fillMaxWidth(), style = ExerciseCardStyle.Large(onClick = {}))
    }
}

@AppPreview
@Composable
private fun ExerciseCardSkeletonSmallPreview() {
    PreviewContainer {
        ExerciseCardSkeleton(modifier = Modifier.fillMaxWidth(), style = ExerciseCardStyle.Small())
    }
}

@AppPreview
@Composable
private fun ExerciseCardSkeletonPlannedPreview() {
    PreviewContainer {
        ExerciseCardSkeleton(modifier = Modifier.fillMaxWidth(), style = ExerciseCardStyle.Planned)
    }
}
