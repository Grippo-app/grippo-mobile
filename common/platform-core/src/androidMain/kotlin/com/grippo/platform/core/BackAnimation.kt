package com.grippo.platform.core

import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.PredictiveBackParams
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.materialPredictiveBackAnimatable
import com.arkivanov.essenty.backhandler.BackHandler

public actual fun <C : Any, T : Any> backAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit,
): StackAnimation<C, T> = stackAnimation(
    animator = fade(),
    predictiveBackParams = {
        PredictiveBackParams(
            backHandler = backHandler,
            onBack = onBack,
            animatable = ::materialPredictiveBackAnimatable,
        )
    }
)