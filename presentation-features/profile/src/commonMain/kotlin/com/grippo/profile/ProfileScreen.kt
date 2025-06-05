package com.grippo.profile

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.presentation.api.profile.ProfileRouter
import com.grippo.profile.ProfileComponent.Child
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ProfileScreen(
    stack: Value<ChildStack<ProfileRouter, Child>>,
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract
) = BaseComposeScreen {
    Children(
        modifier = Modifier.systemBarsPadding(),
        stack = stack,
        animation = stackAnimation(fade()),
        content = { child -> child.instance.component.Render() }
    )
}