package com.grippo.platform.core

import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation

public actual fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T> =
    stackAnimation(animator = fade())