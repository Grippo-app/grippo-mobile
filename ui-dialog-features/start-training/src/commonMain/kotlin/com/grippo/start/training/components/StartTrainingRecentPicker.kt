package com.grippo.start.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.start_training_recent_picker_label
import com.grippo.start.training.StartTrainingOption
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
internal fun StartTrainingRecentPicker(
    modifier: Modifier = Modifier,
    options: ImmutableList<StartTrainingOption.Recent>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = options.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0),
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            text = AppTokens.strings.res(Res.string.start_training_recent_picker_label),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            items(items = options, key = { it.key }) { option ->
                val date = DateTimeUtils.format(
                    value = option.createdAt,
                    format = DateFormat.DateOnly.DateDdMmm,
                )
                val time = DateTimeUtils.format(
                    value = option.createdAt,
                    format = DateFormat.TimeOnly.Time24hHm,
                )
                val label = "$date · $time"

                CheckSelectableCard(
                    style = CheckSelectableCardStyle.Small(title = label),
                    isSelected = option.key == selectedKey,
                    onSelect = { onSelect(option.key) },
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun StartTrainingRecentPickerSinglePreview() {
    PreviewContainer {
        val recent = startTrainingRecentPickerPreviewOptions()
            .take(1)
            .toPersistentList()

        StartTrainingRecentPicker(
            modifier = Modifier.fillMaxWidth(),
            options = recent,
            selectedKey = recent.first().key,
            onSelect = {},
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingRecentPickerMultiplePreview() {
    PreviewContainer {
        val recent = startTrainingRecentPickerPreviewOptions()

        StartTrainingRecentPicker(
            modifier = Modifier.fillMaxWidth(),
            options = recent,
            selectedKey = recent.last().key,
            onSelect = {},
        )
    }
}

private fun startTrainingRecentPickerPreviewOptions(): ImmutableList<StartTrainingOption.Recent> {
    val iteration = stubPendingIteration().copy(
        id = "picker-set",
        repetitions = RepetitionsFormatState.of(10),
    )
    val exercise = stubExercise().copy(
        id = "picker-exercise",
        iterations = persistentListOf(iteration),
    )
    val exercises = persistentListOf(exercise)

    return persistentListOf(
        StartTrainingOption.Recent(
            trainingId = "evening",
            createdAt = LocalDateTime(year = 2026, month = 10, day = 16, hour = 18, minute = 30),
            exercises = exercises,
        ),
        StartTrainingOption.Recent(
            trainingId = "morning",
            createdAt = LocalDateTime(year = 2026, month = 10, day = 16, hour = 8, minute = 15),
            exercises = exercises,
        ),
        StartTrainingOption.Recent(
            trainingId = "earlier",
            createdAt = LocalDateTime(year = 2026, month = 10, day = 14, hour = 17, minute = 0),
            exercises = exercises,
        ),
    )
}
