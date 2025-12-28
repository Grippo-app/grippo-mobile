package com.grippo.core.foundation.platform

import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator

public expect fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T>
public expect fun platformStackAnimator(): StackAnimator