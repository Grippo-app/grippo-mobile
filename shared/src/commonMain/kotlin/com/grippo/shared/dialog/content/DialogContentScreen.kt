package com.grippo.shared.dialog.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.keyHashString
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet

@OptIn(InternalDecomposeApi::class)
@Composable
internal fun DialogContentScreen(
    component: DialogContentComponent,
    state: DialogContentState,
    loaders: ImmutableSet<DialogContentLoader>,
    contract: DialogContentContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    val stackState by component.childStack.subscribeAsState()
    val active = stackState.active

    val visibleChild = remember { mutableStateOf(active) }

    LaunchedEffect(active.key) {
        visibleChild.value = active
    }

    val holder = rememberSaveableStateHolder()
    holder.RetainStates(stackState.items.mapTo(HashSet(), Child<*, *>::keyHashString))

    AnimatedContent(
        modifier = Modifier.fillMaxWidth(),
        targetState = visibleChild.value,
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

/**
 * Retains and removes saveable states based on current ChildStack keys.
 * Prevents memory leaks by cleaning up obsolete child screen states.
 * Used in conjunction with Decompose ChildStack + SaveableStateHolder.
 */
@Composable
private fun SaveableStateHolder.RetainStates(currentKeys: Set<String>) {
    val keys = remember(this) { Keys(currentKeys) }

    DisposableEffect(this, currentKeys) {
        keys.set.forEach {
            if (it !in currentKeys) {
                removeState(it)
            }
        }

        keys.set = currentKeys

        onDispose {}
    }
}

// Mutable holder for the list of keys to track state retention
private class Keys(
    var set: Set<Any>
)