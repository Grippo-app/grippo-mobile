package com.grippo.shared.dialog.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.platform.core.platformAnimation
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun DialogContentScreen(
    component: DialogContentComponent,
    state: DialogContentState,
    loaders: ImmutableSet<DialogContentLoader>,
    contract: DialogContentContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {
    ChildStackCompose(
        modifier = Modifier.fillMaxSize(),
        stack = component.childStack,
        animation = platformAnimation(),
        content = { child -> child.instance.component.Render() }
    )
}