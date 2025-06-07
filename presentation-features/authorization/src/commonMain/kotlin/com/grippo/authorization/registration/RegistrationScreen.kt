package com.grippo.authorization.registration

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.platform.core.backAnimation
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun RegistrationScreen(
    component: RegistrationComponent,
    state: RegistrationState,
    loaders: ImmutableSet<RegistrationLoader>,
    contract: RegistrationContract
) = BaseComposeScreen {
    ChildStackCompose(
        stack = component.childStack,
        animation = backAnimation(
            backHandler = component.backHandler,
            onBack = contract::back
        ),
        content = { child -> child.instance.component.Render() }
    )
}