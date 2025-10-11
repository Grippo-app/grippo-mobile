package com.grippo.exercise.example.picker.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.reason,
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary
            )
        }
    }
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