package com.grippo.trainings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun TrainingsRootScreen(
    component: TrainingsRootComponent,
    state: TrainingsRootState,
    loaders: ImmutableSet<TrainingsRootLoader>,
    contract: TrainingsRootContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    ChildStackCompose(
        modifier = Modifier.fillMaxWidth().weight(1f),
        stack = component.childStack,
        animation = stackAnimation(animator = fade()),
        content = { child -> child.instance.component.Render() }
    )
}
