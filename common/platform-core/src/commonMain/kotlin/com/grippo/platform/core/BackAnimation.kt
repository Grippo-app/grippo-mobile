package com.grippo.platform.core

import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation

public expect fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T>