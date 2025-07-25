package com.grippo.settings.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.settings.ColorCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.system
import com.grippo.presentation.api.settings.models.ColorModeState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun SystemScreen(
    state: SystemState,
    loaders: ImmutableSet<SystemLoader>,
    contract: SystemContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.system),
        onBack = contract::back,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        item(key = "color_mode") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                ColorCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = state.colorMode == ColorModeState.LIGHT,
                    style = ColorModeState.LIGHT,
                    onClick = {}
                )

                ColorCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = state.colorMode == ColorModeState.DARK,
                    style = ColorModeState.DARK,
                    onClick = {}
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        SystemScreen(
            state = SystemState(
                colorMode = ColorModeState.LIGHT
            ),
            loaders = persistentSetOf(),
            contract = SystemContract.Empty
        )
    }
}