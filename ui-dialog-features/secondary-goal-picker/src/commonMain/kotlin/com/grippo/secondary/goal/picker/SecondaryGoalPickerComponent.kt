package com.grippo.secondary.goal.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState

public class SecondaryGoalPickerComponent(
    componentContext: ComponentContext,
    private val title: String,
    private val initial: GoalSecondaryGoalEnumState?,
    private val onResult: (value: GoalSecondaryGoalEnumState?) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<SecondaryGoalPickerDirection>(componentContext) {

    override val viewModel: SecondaryGoalPickerViewModel = componentContext.retainedInstance {
        SecondaryGoalPickerViewModel(
            initial = initial,
            title = title,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: SecondaryGoalPickerDirection) {
        when (direction) {
            is SecondaryGoalPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            SecondaryGoalPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        SecondaryGoalPickerScreen(state.value, loaders.value, viewModel)
    }
}
