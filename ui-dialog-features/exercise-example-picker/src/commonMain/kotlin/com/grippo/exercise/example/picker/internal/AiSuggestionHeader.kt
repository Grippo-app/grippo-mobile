package com.grippo.exercise.example.picker.internal

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.ai.AiSuggestionCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.exercise.example.picker.AiSuggestionQueries
import com.grippo.exercise.example.picker.ExerciseExamplePickerContract

@Composable
internal fun AiSuggestionHeader(
    modifier: Modifier = Modifier,
    value: AiSuggestionQueries,
    contract: ExerciseExamplePickerContract,
) {
    AiSuggestionCard(
        modifier = modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        value = value.reason,
        onClose = contract::onClearSuggestion
    )
}

@Composable
@AppPreview
private fun AiSuggestionHeaderPreview() {
    PreviewContainer {
        AiSuggestionHeader(
            value = AiSuggestionQueries(
                name = "Bench",
                reason = "For some reason",
                id = "",
            ),
            contract = ExerciseExamplePickerContract.Empty,
        )
    }
}