package com.grippo.settings.system

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.system
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
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        SystemScreen(
            state = SystemState,
            loaders = persistentSetOf(),
            contract = SystemContract.Empty
        )
    }
}