package com.grippo.shared.dialog.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.keyHashString
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.shared.dialog.content.internal.RetainDialogContentStates

@OptIn(InternalDecomposeApi::class)
@Composable
internal fun DialogContentScreen(
    component: DialogContentComponent,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    val stackState by component.childStack.subscribeAsState()
    val active = stackState.active

    val holder = rememberSaveableStateHolder()
    RetainDialogContentStates(
        holder = holder,
        currentKeys = stackState.items.mapTo(HashSet(), Child<*, *>::keyHashString),
    )

    AnimatedContent(
        modifier = Modifier.fillMaxWidth(),
        targetState = active,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(200)) using SizeTransform(clip = false)
        },
        contentKey = { it.keyHashString() }
    ) { child ->
        holder.SaveableStateProvider(child.keyHashString()) {
            child.instance.component.Render()
        }
    }
}
