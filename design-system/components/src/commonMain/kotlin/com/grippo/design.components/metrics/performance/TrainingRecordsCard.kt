package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.core.state.metrics.performance.TrainingRecordState
import com.grippo.core.state.metrics.performance.stubTrainingRecords
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.skeleton.SkeletonPanel
import com.grippo.design.components.skeleton.SkeletonPanelStyle
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.training_records_description
import com.grippo.design.resources.provider.training_records_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private const val RECORD_KINDS_SEPARATOR = "\u00A0· "

private const val NO_BREAK_SPACE = '\u00A0'

@Composable
public fun TrainingRecordsCard(
    modifier: Modifier = Modifier,
    records: ImmutableList<TrainingRecordState>,
    onRecordClick: (exampleId: String) -> Unit,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            Text(
                text = AppTokens.strings.res(Res.string.training_records_title),
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = AppTokens.strings.res(Res.string.training_records_description),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            records.forEachIndexed { index, record ->
                key(record.exerciseExample.id) {
                    if (index > 0) {
                        ContentDivider()
                    }

                    TrainingRecordRow(
                        modifier = Modifier.fillMaxWidth(),
                        record = record,
                        onClick = { onRecordClick(record.exerciseExample.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingRecordRow(
    modifier: Modifier = Modifier,
    record: TrainingRecordState,
    onClick: () -> Unit,
) {
    val kinds = record.achievements.map { achievement -> "${achievement.text().text()} ${achievement.totalValue()}" }
    val kindsLine = kinds.joinToString(separator = RECORD_KINDS_SEPARATOR) { kind -> kind.replace(' ', NO_BREAK_SPACE) }
    val spokenKinds = AnnotatedString(kinds.joinToString(separator = ", "))

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseExampleImage(
            value = record.exerciseExample.imageUrl,
            style = ExerciseExampleImageStyle.SMALL,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = record.exerciseExample.name,
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                modifier = Modifier.clearAndSetSemantics { text = spokenKinds },
                text = kindsLine,
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.secondary,
            )
        }

        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.performance.records.trophyIcon),
            imageVector = AppTokens.icons.Trophy,
            tint = AppTokens.colors.semantic.success,
            contentDescription = null,
        )

        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
            imageVector = AppTokens.icons.ArrowRight,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null,
        )
    }
}

@Composable
public fun TrainingRecordsCardSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val descriptionLine = rememberTextHeight(style = AppTokens.typography.b13Med(), textMeasurer = textMeasurer)
    val nameLine = rememberTextHeight(style = AppTokens.typography.b13Semi(), textMeasurer = textMeasurer)
    val kindsLine = rememberTextHeight(style = AppTokens.typography.b11Med(), textMeasurer = textMeasurer)

    val textGap = AppTokens.dp.contentPadding.text
    val headerHeight = titleLine + textGap + descriptionLine
    val rowHeight = maxOf(
        AppTokens.dp.exerciseExampleImage.small.size,
        nameLine + textGap + kindsLine,
    ) + AppTokens.dp.contentPadding.subContent * 2

    SkeletonPanel(
        modifier = modifier,
        contentHeight = headerHeight + AppTokens.dp.metrics.panel.large.spacer +
            rowHeight * 2 + AppTokens.dp.divider.thickness,
        style = SkeletonPanelStyle.Large,
    )
}

@AppPreview
@Composable
private fun TrainingRecordsCardSkeletonPreview() {
    PreviewContainer {
        TrainingRecordsCardSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun TrainingRecordsCardSeveralKindsPreview() {
    PreviewContainer {
        TrainingRecordsCard(
            modifier = Modifier.fillMaxWidth(),
            records = persistentListOf(stubTrainingRecords().first()),
            onRecordClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecordsCardManyExercisesPreview() {
    val records = stubTrainingRecords()
    val moreRecords = persistentListOf(
        records[1].copy(
            exerciseExample = records[1].exerciseExample.copy(
                id = "preview-example-deadlift",
                name = "Romanian Deadlift",
            ),
        ),
        records[2].copy(
            exerciseExample = records[2].exerciseExample.copy(
                id = "preview-example-row",
                name = "Seated Cable Row",
            ),
        ),
    )
    PreviewContainer {
        TrainingRecordsCard(
            modifier = Modifier.fillMaxWidth(),
            records = (records + moreRecords).toPersistentList(),
            onRecordClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingRecordsCardLongNamesPreview() {
    PreviewContainer {
        TrainingRecordsCard(
            modifier = Modifier.fillMaxWidth(),
            records = stubTrainingRecords().map { record ->
                record.copy(
                    exerciseExample = record.exerciseExample.copy(
                        name = "Single-arm incline dumbbell bench press with neutral grip and pause",
                    ),
                )
            }.toPersistentList(),
            onRecordClick = {},
        )
    }
}
