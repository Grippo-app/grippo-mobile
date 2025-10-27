package com.grippo.debug.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.debug.DebugContract
import com.grippo.debug.DebugLoader
import com.grippo.debug.LoggerState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.loading.Loader
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.generate_training_btn
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun GeneralPage(
    modifier: Modifier = Modifier,
    state: LoggerState,
    contract: DebugContract,
    loaders: ImmutableSet<DebugLoader>
) {
    if (loaders.contains(DebugLoader.Logs)) {
        Loader(modifier = Modifier.fillMaxSize())
        return
    }

    Column(modifier = modifier) {
        val buttonState = remember(loaders) {
            when {
                loaders.contains(DebugLoader.GenerateTraining) -> ButtonState.Loading
                else -> ButtonState.Enabled
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                vertical = AppTokens.dp.contentPadding.content,
                horizontal = AppTokens.dp.screen.horizontalPadding
            )
        ) {
            item(key = "generate_training_btn") {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                    content = ButtonContent.Text(AppTokens.strings.res(Res.string.generate_training_btn)),
                    style = ButtonStyle.Magic,
                    state = buttonState,
                    onClick = contract::generateTraining
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
