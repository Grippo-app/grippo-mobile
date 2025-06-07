package com.grippo.authorization

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.platform.core.backAnimation
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun AuthScreen(
    component: AuthComponent,
    state: AuthState,
    loaders: ImmutableSet<AuthLoader>,
    contract: AuthContract
) = BaseComposeScreen {
    ChildStackCompose(
        modifier = Modifier.systemBarsPadding(),
        stack = component.childStack,
        animation = backAnimation(
            backHandler = component.backHandler,
            onBack = contract::back
        ),
        content = { child -> child.instance.component.Render() }
    )
}
