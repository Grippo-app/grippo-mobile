package com.grippo.authorization.registration

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.platform.core.platformAnimation
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun RegistrationScreen(
    component: RegistrationComponent,
    state: RegistrationState,
    loaders: ImmutableSet<RegistrationLoader>,
    contract: RegistrationContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {
    ChildStackCompose(
        stack = component.childStack,
        animation = platformAnimation(),
        content = { child -> child.instance.component.Render() }
    )
}