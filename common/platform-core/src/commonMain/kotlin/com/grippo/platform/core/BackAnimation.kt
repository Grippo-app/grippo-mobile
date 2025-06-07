package com.grippo.platform.core

import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.essenty.backhandler.BackHandler

public expect fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit,
): StackAnimation<C, T>