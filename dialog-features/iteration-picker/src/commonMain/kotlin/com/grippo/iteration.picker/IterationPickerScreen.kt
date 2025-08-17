package com.grippo.iteration.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun IterationPickerScreen(
    state: IterationPickerState,
    loaders: ImmutableSet<IterationPickerLoader>,
    contract: IterationPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
    ) {
        Text(
            text = "Iteration",
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

//        Input(
//            value = state.weight.toString(),
//            placeholder = PlaceHolder.OverInput("Weight (kg)"),
//            inputStyle = InputStyle.Default(onValueChange = contract::onWeightChange),
//        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.content))

//        Input(
//            value = state.repetitions,
//            placeholder = PlaceHolder.OverInput("Repetitions"),
//            inputStyle = InputStyle.Default(onValueChange = contract::onRepetitionsChange),
//        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        Button(text = "Submit", style = ButtonStyle.Primary, onClick = contract::onSubmit)
    }
}




