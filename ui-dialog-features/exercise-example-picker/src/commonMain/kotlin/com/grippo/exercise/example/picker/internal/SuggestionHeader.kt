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
import com.grippo.exercise.example.picker.ExerciseExamplePickerContract
import com.grippo.exercise.example.picker.SuggestionQueries

@Composable
internal fun SuggestionHeader(
    modifier: Modifier = Modifier,
    value: SuggestionQueries,
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