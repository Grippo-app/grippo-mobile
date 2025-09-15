package com.grippo.settings.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.settings.LocaleCard
import com.grippo.design.components.settings.ThemeCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.system
import com.grippo.state.settings.LocaleState
import com.grippo.state.settings.ThemeState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun SystemScreen(
    state: SystemState,
    loaders: ImmutableSet<SystemLoader>,
    contract: SystemContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.system),
        onBack = contract::onBack,
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

                val lightClickProvider = remember {
                    { contract.onThemeClick(ThemeState.LIGHT) }
                }

                ThemeCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = state.theme == ThemeState.LIGHT,
                    style = ThemeState.LIGHT,
                    onClick = lightClickProvider
                )

                val darkClickProvider = remember {
                    { contract.onThemeClick(ThemeState.DARK) }
                }

                ThemeCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = state.theme == ThemeState.DARK,
                    style = ThemeState.DARK,
                    onClick = darkClickProvider
                )
            }
        }

        state.locale?.let { locale ->
            item(key = "locale") {
                LocaleCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = locale,
                    onClick = contract::onLocaleClick
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
                theme = ThemeState.LIGHT,
                locale = LocaleState.UA
            ),
            loaders = persistentSetOf(),
            contract = SystemContract.Empty
        )
    }
}